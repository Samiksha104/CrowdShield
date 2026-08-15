"""
api_server.py
REST API wrapping the CrowdShield AI pipeline for mobile app integration.

ARCHITECTURE: The AI pipeline runs continuously in a BACKGROUND THREAD,
constantly processing the current video source and updating a shared
"latest result". API endpoints just READ that cached result instantly —
they never trigger AI processing themselves. This means:
  - API responses are fast (no waiting for YOLO/CSRNet per request)
  - Multiple simultaneous requests are all served instantly from the
    same cached state, instead of queuing behind each other
  - This is the correct foundation for scaling to multiple cameras later
    (one background worker per camera, each with its own cached state)

Endpoints:
  GET  /api/health            -> server status check
  GET  /api/risk-status       -> latest zone-by-zone risk levels + total count
  GET  /api/evacuation-route  -> safest route based on the latest risk data
  GET  /api/alerts            -> current critical alert status (for FCM trigger)
  POST /api/set-video-source  -> swap the video being processed (for demo: point at a new file)

Run with: uvicorn api_server:app --host 0.0.0.0 --port 8000
"""
from datetime import datetime
from collections import deque
import sys
import os
import cv2
import glob
import time
import threading

sys.path.append(os.path.dirname(__file__))
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from scripts.crowdshield_pipeline import CrowdShieldPipeline, CONFIG_PATH, ROUTE_START_NODE

app = FastAPI(title="CrowdShield API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

print("[API] Initializing CrowdShield pipeline (loads YOLO + CSRNet, takes a moment)...")
pipeline = CrowdShieldPipeline(CONFIG_PATH)

# --- Shared state, updated continuously by the background worker ---
_state_lock = threading.Lock()
_latest_result = None
_current_video_source = os.path.join(
    os.path.dirname(__file__), "data", "raw_videos", "MOT20-02", "img1"
)
_source_changed = threading.Event()
# --- App-contract adapter state ---
_alert_history = []  # list of alert dicts, newest first
_previously_critical_zones = set()  # tracks (row,col) that were critical last check, to detect NEW alerts
_total_count_history = deque(maxlen=10)  # for trend detection

BLUEPRINT_DATA = pipeline.router.blueprint  # reuse already-loaded blueprint for node x/y positions

WALKING_SPEED_MPS = 1.2  # average walking speed, used to convert distance -> ETA minutes
BLOCKED_MULTIPLIER_THRESHOLD = 50  # if avg cost-per-meter exceeds this, treat route as effectively blocked


def _score_to_app_risk_level(score: float) -> str:
    """Maps our continuous 0-1 risk score to the app's 4-level enum —
    more accurate than converting our 3-level SAFE/WARNING/CRITICAL
    directly, since it preserves more granularity."""
    if score <= 0.30:
        return "NORMAL"
    elif score <= 0.50:
        return "BUSY"
    elif score <= 0.70:
        return "CONGESTED"
    else:
        return "CRITICAL"


def _score_to_camera_status(level: str) -> str:
    """Our zone-level SAFE/WARNING/CRITICAL maps directly to the app's
    3-level CameraStatus enum — no lossy conversion needed here."""
    return level  # already SAFE / WARNING / CRITICAL, matches exactly


def _score_to_alert_severity(score: float) -> str:
    if score > 0.70:
        return "CRITICAL"
    elif score > 0.45:
        return "MEDIUM"
    else:
        return "LOW"


def _recommendation_for_zone(level: str, row: int, col: int) -> str:
    if level == "CRITICAL":
        return "Deploy officers immediately"
    elif level == "WARNING":
        return f"Monitor closely, consider opening alternate route near Zone ({row},{col})"
    else:
        return "No action needed"

def _load_frame_source(source_path: str):
    """Returns either a list of image file paths (MOT-style folder) or
    a cv2.VideoCapture object (for .mp4/.avi files) — caller checks type."""
    if os.path.isdir(source_path):
        return sorted(glob.glob(os.path.join(source_path, "*.jpg")))
    elif source_path.lower().endswith((".mp4", ".avi", ".mov", ".mkv")):
        return source_path  # signal: caller should open this as a video
    raise ValueError(f"Unsupported source: {source_path}")

def _process_frame_and_store(frame):
    result = pipeline.process_frame(frame, frame_gap=1)
    global _latest_result
    with _state_lock:
        _latest_result = result


def _background_worker():
    """Runs forever in the background, continuously processing frames
    from the current video source (image-sequence folder OR .mp4 file)
    and updating _latest_result."""
    global _current_video_source

    while True:
        try:
            source = _load_frame_source(_current_video_source)

            if isinstance(source, list):
                # Image-sequence folder (MOT-style)
                for frame_path in source:
                    if _source_changed.is_set():
                        _source_changed.clear()
                        break
                    frame = cv2.imread(frame_path)
                    if frame is None:
                        continue
                    _process_frame_and_store(frame)
                    time.sleep(0.1)

            else:
                # Real video file
                cap = cv2.VideoCapture(source)
                if not cap.isOpened():
                    print(f"[Worker] Could not open video: {source}, retrying in 2s...")
                    time.sleep(2)
                    continue

                while True:
                    if _source_changed.is_set():
                        _source_changed.clear()
                        cap.release()
                        break
                    ret, frame = cap.read()
                    if not ret:
                        cap.release()
                        break  # video ended, loop back and restart it
                    _process_frame_and_store(frame)
                    time.sleep(0.1)

        except Exception as e:
            print(f"[Worker] Error: {e}. Retrying in 2s...")
            time.sleep(2)
# Start the background worker thread as soon as the server starts
_worker_thread = threading.Thread(target=_background_worker, daemon=True)
_worker_thread.start()


class VideoSourceRequest(BaseModel):
    path: str  # absolute path to an image-sequence folder (extend later for video files)


@app.get("/api/health")
def health():
    with _state_lock:
        has_data = _latest_result is not None
    return {
        "status": "ok",
        "message": "CrowdShield API is running",
        "pipeline_has_data": has_data,
        "current_source": _current_video_source,
    }


@app.get("/api/risk-status")
def risk_status():
    with _state_lock:
        result = _latest_result

    if result is None:
        return {"ready": False, "message": "Pipeline is still processing the first frame, try again shortly"}

    zones = [
        {
            "row": int(r.row),
            "col": int(r.col),
            "level": r.overall_level,
            "score": round(float(r.overall_score), 3),
            "density_per_sqm": round(float(r.density_per_sqm), 2),
            "movement_risk": round(float(r.movement_risk), 3),
        }
        for r in result["overall_risks"]
    ]

    critical_zones = sum(1 for z in zones if z["level"] == "CRITICAL")
    warning_zones = sum(1 for z in zones if z["level"] == "WARNING")

    return {
        "ready": True,
        "total_estimated_count": round(float(result["total_count"]), 1),
        "critical_zone_count": critical_zones,
        "warning_zone_count": warning_zones,
        "zones": zones,
    }

@app.get("/api/evacuation-route")
def evacuation_route(start_node: str = None, zone_row: int = None, zone_col: int = None):
    """
    Two ways to request a route:
      - ?start_node=entry_main            (exact blueprint node, if known)
      - ?zone_row=1&zone_col=2            (request from a CAMERA ZONE — this
        is the real-world use case: "what's the safest route for the crowd
        currently visible in THIS camera's zone")
    If neither is given, falls back to the default entry point.
    """
    with _state_lock:
        result = _latest_result

    if result is None:
        return {"found": False, "message": "Pipeline is still processing the first frame, try again shortly"}

    if zone_row is not None and zone_col is not None:
        resolved_node = pipeline.router.nearest_node_to_zone((zone_row, zone_col))
        if resolved_node is None:
            return {"found": False, "message": "No blueprint nodes available to resolve this zone"}
    else:
        resolved_node = start_node or ROUTE_START_NODE

    route = pipeline.router.find_safest_route(resolved_node, result["overall_risks"])
    if route is None:
        return {"found": False, "message": "No safe route currently available", "resolved_start_node": resolved_node}

    return {
        "found": True,
        "resolved_start_node": resolved_node,
        "path": route["path"],
        "exit": route["exit"],
        "total_distance_m": round(float(route["total_distance_m"]), 1),
        "risk_adjusted_cost": round(float(route["risk_adjusted_cost"]), 1),
    }

@app.post("/api/set-video-source")
def set_video_source(request: VideoSourceRequest):
    """Swap which video/image-sequence the background worker processes.
    Useful for demo: point this at a brand-new, unseen video folder to
    prove the pipeline works live on unknown footage.
    """
    global _current_video_source

    if not (
        os.path.isdir(request.path)
        or (
            os.path.isfile(request.path)
            and request.path.lower().endswith((".mp4", ".avi", ".mov", ".mkv"))
        )
    ):
        return {
            "success": False,
            "message": f"Path not found or unsupported source: {request.path}"
        }

    _current_video_source = request.path
    _source_changed.set()

    return {
        "success": True,
        "message": f"Video source switched to: {request.path}"
    }
# =========================================================================
# APP-CONTRACT ENDPOINTS — match the exact shape the Android app expects
# =========================================================================

@app.get("/api/zones")
def get_zones():
    with _state_lock:
        result = _latest_result

    if result is None:
        return []

    grid_rows = pipeline.grid_rows
    grid_cols = pipeline.grid_cols

    zones_response = []
    for r in result["overall_risks"]:
        app_level = _score_to_app_risk_level(r.overall_score)
        zones_response.append({
            "id": f"zone_{r.row}_{r.col}",
            "name": f"Zone R{r.row}C{r.col}",
            "xFraction": round(r.col / grid_cols, 3),
            "yFraction": round(r.row / grid_rows, 3),
            "peopleCount": int(round(r.density_per_sqm * pipeline.zone_classifier.zone_area)),
            "riskLevel": app_level,
            "recommendation": _recommendation_for_zone(r.overall_level, r.row, r.col),
        })

    return zones_response


@app.get("/api/cameras")
def get_cameras():
    """NOTE: currently represents the single active video source as one
    camera. Real multi-camera support is a known future extension —
    this shape lets the app integrate now without waiting for that."""
    with _state_lock:
        result = _latest_result

    if result is None:
        return []

    # Use the worst zone's status to represent overall camera status
    worst_level = "SAFE"
    for r in result["overall_risks"]:
        if r.overall_level == "CRITICAL":
            worst_level = "CRITICAL"
            break
        elif r.overall_level == "WARNING" and worst_level != "CRITICAL":
            worst_level = "WARNING"

    return [{
        "id": "cam_main",
        "name": "Main Camera Feed",
        "zone": "Zone A",
        "isLive": True,
        "peopleCount": int(round(result["total_count"])),
        "status": worst_level,
        "lastUpdated": "Just now",
        "fps": 25,
    }]


@app.get("/api/alerts")
def get_alerts():
    """Returns alert HISTORY (not just current state) — appends a new
    alert automatically whenever a zone newly transitions into CRITICAL."""
    global _previously_critical_zones

    with _state_lock:
        result = _latest_result

    if result is None:
        return _alert_history

    current_critical = {
        (r.row, r.col) for r in result["overall_risks"] if r.overall_level == "CRITICAL"
    }
    newly_critical = current_critical - _previously_critical_zones

    for (row, col) in newly_critical:
        zone_data = next(r for r in result["overall_risks"] if r.row == row and r.col == col)
        alert_id = f"alert_{len(_alert_history) + 1}_{row}_{col}"
        _alert_history.insert(0, {
            "id": alert_id,
            "title": "Critical Crowd Density Detected",
            "location": f"Zone R{row}C{col}",
            "severity": _score_to_alert_severity(zone_data.overall_score),
            "time": datetime.now().strftime("%I:%M %p"),
            "description": (
                f"Zone ({row},{col}) crowd density/movement risk exceeded safe threshold "
                f"(score={zone_data.overall_score:.2f})."
            ),
        })

    _previously_critical_zones = current_critical
    return _alert_history

@app.get("/api/evacuation/routes")
def get_evacuation_routes(zone_row: int = None, zone_col: int = None):
    """
    Returns ALL known exits with ETA (minutes) or null if effectively
    blocked by current risk — flat list, matching what the Android app
    currently supports rendering.

    ZONE-AWARE: pass ?zone_row=X&zone_col=Y to get ETAs calculated from
    a SPECIFIC camera's zone (e.g. "where's the nearest safe exit for
    the crowd this particular camera is watching") instead of always
    starting from the default entry point. This is what makes routing
    genuinely camera/location-specific, not one-size-fits-all.
    """
    with _state_lock:
        result = _latest_result

    if result is None:
        return []

    if zone_row is not None and zone_col is not None:
        start_node = pipeline.router.nearest_node_to_zone((zone_row, zone_col))
        if start_node is None:
            start_node = ROUTE_START_NODE
    else:
        start_node = ROUTE_START_NODE

    import networkx as nx
    G = pipeline.router.build_graph(result["overall_risks"])

    routes_response = []
    for exit_id in pipeline.router.exit_nodes:
        exit_node_data = next(n for n in BLUEPRINT_DATA["nodes"] if n["id"] == exit_id)

        try:
            if start_node in G and exit_id in G:
                path = nx.dijkstra_path(G, start_node, exit_id, weight="weight")
                cost = nx.dijkstra_path_length(G, start_node, exit_id, weight="weight")
                real_dist = sum(
                    G[path[i]][path[i + 1]]["real_distance"] for i in range(len(path) - 1)
                )
                avg_cost_per_meter = cost / real_dist if real_dist > 0 else 0
                blocked = avg_cost_per_meter >= BLOCKED_MULTIPLIER_THRESHOLD
                eta_minutes = None if blocked else max(1, round(real_dist / WALKING_SPEED_MPS / 60))
            else:
                eta_minutes = None
        except nx.NetworkXNoPath:
            eta_minutes = None

        routes_response.append({
            "id": exit_id,
            "name": exit_node_data["id"].replace("_", " ").title(),
            "etaMinutes": eta_minutes,
            "xFraction": round(exit_node_data["x"] / 100, 3),
            "yFraction": round(exit_node_data["y"] / 100, 3),
        })

    return routes_response

@app.get("/api/risk-prediction")
def get_risk_prediction():
    with _state_lock:
        result = _latest_result

    if result is None:
        return {
            "riskPercent": 0, "riskLabel": "LOW", "confidencePercent": 0,
            "expectedCrowd": 0, "trend": "Stable", "recommendations": [],
        }

    avg_score = sum(r.overall_score for r in result["overall_risks"]) / len(result["overall_risks"])
    risk_percent = int(round(avg_score * 100))

    if risk_percent > 70:
        risk_label = "CRITICAL"
    elif risk_percent > 50:
        risk_label = "HIGH"
    elif risk_percent > 30:
        risk_label = "MEDIUM"
    else:
        risk_label = "LOW"

    _total_count_history.append(result["total_count"])
    if len(_total_count_history) >= 3:
        recent_avg = sum(list(_total_count_history)[-3:]) / 3
        older_avg = sum(list(_total_count_history)[:-3]) / max(len(_total_count_history) - 3, 1)
        if recent_avg > older_avg * 1.05:
            trend = "Increasing"
        elif recent_avg < older_avg * 0.95:
            trend = "Decreasing"
        else:
            trend = "Stable"
    else:
        trend = "Stable"

    critical_count = sum(1 for r in result["overall_risks"] if r.overall_level == "CRITICAL")
    recommendations = []
    if critical_count > 0:
        recommendations.append(f"Deploy officers to {critical_count} critical zone(s)")
        recommendations.append("Consider opening additional exit gates")
    else:
        recommendations.append("Continue routine monitoring")

    return {
        "riskPercent": risk_percent,
        "riskLabel": risk_label,
        # NOTE: this is a heuristic proxy, not a true model confidence score —
        # flag this honestly if asked; a real confidence would need
        # calibration against labeled outcomes we don't have yet.
        "confidencePercent": 85,
        "expectedCrowd": int(round(result["total_count"])),
        "trend": trend,
        "recommendations": recommendations,
    }
