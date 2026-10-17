"""
crowdshield_pipeline.py
THE full CrowdShield pipeline — Phases 1 through 5, wired into one
continuous loop. Point this at any video source (file, image sequence,
or eventually a live RTSP stream) and it runs detection, density
estimation, movement analysis, risk classification, and evacuation
routing automatically, frame by frame.

This is the actual integrated system — not separate test scripts.
"""

import sys
import os
import cv2
import glob
import yaml
import time

sys.path.append(os.path.join(os.path.dirname(__file__), ".."))
from src.detection.detector import CrowdDetector
from src.density.density_estimator import DensityEstimator
from src.density.zone_classifier import ZoneClassifier
from src.behavior.social_force import MovementAnalyzer
from src.behavior.optical_flow import OpticalFlowAnalyzer
from src.risk.risk_engine import RiskEngine
from src.routing.blueprint_router import BlueprintRouter

CONFIG_PATH = os.path.join(os.path.dirname(__file__), "..", "config", "config.yaml")
YOLO_WEIGHTS = os.path.join(os.path.dirname(__file__), "..", "models", "crowd_head_yolov8.pt")
CSRNET_WEIGHTS = os.path.join(os.path.dirname(__file__), "..", "models", "csrnet_shanghaitech_partA.pth")
BLUEPRINT_PATH = os.path.join(os.path.dirname(__file__), "..", "data", "blueprints", "example_venue.json")

# Input source: change this to any MOT-style image sequence folder,
# or swap for cv2.VideoCapture(video_path) for a real .mp4/live stream.
SEQUENCE_DIR = os.path.join(os.path.dirname(__file__), "..", "data", "raw_videos", "MOT20-02", "img1")
OUTPUT_PATH = os.path.join(os.path.dirname(__file__), "..", "outputs", "crowdshield_full_run.mp4")

FRAME_SKIP = 5
MAX_FRAMES = 40
ROUTE_START_NODE = "entry_main"

COLOR_MAP = {"SAFE": (0, 200, 0), "WARNING": (0, 165, 255), "CRITICAL": (0, 0, 255)}


class CrowdShieldPipeline:
    """Wraps every phase into one object with a single per-frame entry point."""

    def __init__(self, config_path: str):
        with open(config_path) as f:
            self.config = yaml.safe_load(f)

        print("[CrowdShield] Loading models...")
        self.detector = CrowdDetector(
            weights_path=YOLO_WEIGHTS, confidence_threshold=0.15, imgsz=1280
        )
        self.estimator = DensityEstimator(weights_path=CSRNET_WEIGHTS, device="cpu")

        grid_rows = self.config["density"]["grid_rows"]
        grid_cols = self.config["density"]["grid_cols"]
        self.grid_rows = grid_rows
        self.grid_cols = grid_cols

        self.zone_classifier = ZoneClassifier(
            grid_rows=grid_rows, grid_cols=grid_cols,
            total_ground_area_sqm=self.config["density"]["ground_area_sqm"],
            safe_threshold=self.config["density"]["density_thresholds"]["safe"],
            warning_threshold=self.config["density"]["density_thresholds"]["warning"],
        )
        self.movement_analyzer = MovementAnalyzer(history_length=5, grid_rows=grid_rows, grid_cols=grid_cols)
        self.flow_analyzer = OpticalFlowAnalyzer(grid_rows=grid_rows, grid_cols=grid_cols)
        self.risk_engine = RiskEngine(
            density_weight=self.config["risk"]["density_weight"],
            movement_weight=self.config["risk"]["movement_weight"],
            safe_threshold=self.config["density"]["density_thresholds"]["safe"],
            warning_threshold=self.config["density"]["density_thresholds"]["warning"],
        )
        self.router = BlueprintRouter(blueprint_path=BLUEPRINT_PATH)
        print("[CrowdShield] All models loaded. Pipeline ready.\n")

    def process_frame(self, frame, frame_gap: int = 1):
        """
        Runs ALL phases on a single frame. Returns a dict with everything
        downstream code (drawing, alerts, routing) needs.
        """
        # --- Phase 1: Detection + Tracking ---
        detections = self.detector.detect_and_track(frame)

        # --- Phase 3: Movement analysis (+ optical flow fallback) ---
        self.movement_analyzer.update(detections)
        zone_movements = self.movement_analyzer.analyze(detections, frame.shape, real_frame_gap=frame_gap)

        flow_results = self.flow_analyzer.compute_zone_flow(frame)
        for zm in zone_movements:
            if zm.person_count < 2 and (zm.row, zm.col) in flow_results:
                avg_mag, dir_var = flow_results[(zm.row, zm.col)]
                speed_risk = min(avg_mag / 5.0, 1.0)
                chaos_risk = dir_var
                zm.movement_risk = float(min(0.6 * speed_risk + 0.4 * chaos_risk, 1.0))

        # --- Phase 1b/2: Density estimation + zone classification ---
        density_map, total_count = self.estimator.predict(frame)
        zone_results = self.zone_classifier.classify(density_map, frame.shape)

        # --- Phase 4: Combined risk ---
        overall_risks = self.risk_engine.combine(zone_results, zone_movements)

        # --- Phase 5: Evacuation routing, based on CURRENT risk ---
        route = self.router.find_safest_route(ROUTE_START_NODE, overall_risks)

        return {
            "detections": detections,
            "total_count": total_count,
            "overall_risks": overall_risks,
            "route": route,
        }

    def draw_overlay(self, frame, result):
        h, w = frame.shape[:2]
        zone_h = h // self.grid_rows
        zone_w = w // self.grid_cols

        critical_count = 0
        warning_count = 0

        for zr in result["overall_risks"]:
            y1, y2 = zr.row * zone_h, (zr.row + 1) * zone_h
            x1, x2 = zr.col * zone_w, (zr.col + 1) * zone_w
            color = COLOR_MAP[zr.overall_level]

            if zr.overall_level == "CRITICAL":
                critical_count += 1
            elif zr.overall_level == "WARNING":
                warning_count += 1

            cv2.rectangle(frame, (x1, y1), (x2, y2), color, 2)
            cv2.putText(frame, f"{zr.overall_level} ({zr.overall_score:.2f})",
                        (x1 + 5, y1 + 20), cv2.FONT_HERSHEY_SIMPLEX, 0.45, color, 1)

        status_text = (f"Total: {result['total_count']:.0f} | "
                        f"CRITICAL zones: {critical_count} | WARNING zones: {warning_count}")
        cv2.putText(frame, status_text, (20, h - 40),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)

        if result["route"]:
            route_text = f"Recommended exit: {result['route']['exit']} via {' -> '.join(result['route']['path'])}"
            cv2.putText(frame, route_text, (20, h - 15),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 0), 1)
        else:
            cv2.putText(frame, "NO SAFE ROUTE FOUND", (20, h - 15),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)

        return frame


def main():
    pipeline = CrowdShieldPipeline(CONFIG_PATH)

    frame_files = sorted(glob.glob(os.path.join(SEQUENCE_DIR, "*.jpg")))
    if not frame_files:
        print(f"ERROR: no frames found in {SEQUENCE_DIR}")
        return

    first_frame = cv2.imread(frame_files[0])
    height, width = first_frame.shape[:2]
    fourcc = cv2.VideoWriter_fourcc(*"mp4v")
    out = cv2.VideoWriter(OUTPUT_PATH, fourcc, 3, (width, height))

    processed = 0
    start_time = time.time()

    for idx, frame_path in enumerate(frame_files):
        if idx % FRAME_SKIP != 0:
            continue
        if processed >= MAX_FRAMES:
            break

        frame = cv2.imread(frame_path)
        result = pipeline.process_frame(frame, frame_gap=FRAME_SKIP)
        frame = pipeline.draw_overlay(frame, result)

        out.write(frame)

        route_summary = result["route"]["exit"] if result["route"] else "NONE"
        print(f"Frame {idx}: total={result['total_count']:.1f}, "
              f"route_exit={route_summary}")

        processed += 1

    out.release()
    elapsed = time.time() - start_time
    print(f"\n[CrowdShield] Done. Processed {processed} frames in {elapsed:.1f}s "
          f"({elapsed/max(processed,1):.2f}s/frame).")
    print(f"[CrowdShield] Full pipeline video saved to: {OUTPUT_PATH}")


if __name__ == "__main__":
    main()
