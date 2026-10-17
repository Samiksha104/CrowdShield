"""
run_full_pipeline_test.py
The full CrowdShield pipeline, integrated: YOLO detection/tracking +
CSRNet density estimation + zone classification + movement analysis
(SFM signals + optical flow fallback) + combined risk engine.

This is the real Phase 1-4 integration test — the core "brain" of the
system, before routing (Phase 5) is added.
"""

import sys
import os
import cv2
import glob
import yaml

sys.path.append(os.path.join(os.path.dirname(__file__), ".."))
from src.detection.detector import CrowdDetector
from src.density.density_estimator import DensityEstimator
from src.density.zone_classifier import ZoneClassifier
from src.behavior.social_force import MovementAnalyzer
from src.behavior.optical_flow import OpticalFlowAnalyzer
from src.risk.risk_engine import RiskEngine

CONFIG_PATH = os.path.join(os.path.dirname(__file__), "..", "config", "config.yaml")
YOLO_WEIGHTS = os.path.join(os.path.dirname(__file__), "..", "models", "crowd_head_yolov8.pt")
CSRNET_WEIGHTS = os.path.join(os.path.dirname(__file__), "..", "models", "csrnet_shanghaitech_partA.pth")
SEQUENCE_DIR = os.path.join(os.path.dirname(__file__), "..", "data", "raw_videos", "MOT20-02", "img1")
OUTPUT_PATH = os.path.join(os.path.dirname(__file__), "..", "outputs", "full_pipeline_test.mp4")

FRAME_SKIP = 5      # heaviest script yet (YOLO + CSRNet + optical flow per frame)
MAX_FRAMES = 60      # keep this modest for a reasonable CPU runtime


def main():
    with open(CONFIG_PATH) as f:
        config = yaml.safe_load(f)

    print("Loading models (YOLO + CSRNet)...")
    detector = CrowdDetector(weights_path=YOLO_WEIGHTS, confidence_threshold=0.15, imgsz=1280)
    estimator = DensityEstimator(weights_path=CSRNET_WEIGHTS, device="cpu")

    grid_rows = config["density"]["grid_rows"]
    grid_cols = config["density"]["grid_cols"]

    zone_classifier = ZoneClassifier(
        grid_rows=grid_rows, grid_cols=grid_cols,
        total_ground_area_sqm=config["density"]["ground_area_sqm"],
        safe_threshold=config["density"]["density_thresholds"]["safe"],
        warning_threshold=config["density"]["density_thresholds"]["warning"],
    )

    movement_analyzer = MovementAnalyzer(history_length=5, grid_rows=grid_rows, grid_cols=grid_cols)
    flow_analyzer = OpticalFlowAnalyzer(grid_rows=grid_rows, grid_cols=grid_cols)

    risk_engine = RiskEngine(
        density_weight=config["risk"]["density_weight"],
        movement_weight=config["risk"]["movement_weight"],
        safe_threshold=config["density"]["density_thresholds"]["safe"],
        warning_threshold=config["density"]["density_thresholds"]["warning"],
    )

    frame_files = sorted(glob.glob(os.path.join(SEQUENCE_DIR, "*.jpg")))
    first_frame = cv2.imread(frame_files[0])
    height, width = first_frame.shape[:2]

    fourcc = cv2.VideoWriter_fourcc(*"mp4v")
    out = cv2.VideoWriter(OUTPUT_PATH, fourcc, 3, (width, height))  # low fps output, heavy processing per frame

    zone_h = height // grid_rows
    zone_w = width // grid_cols

    color_map = {"SAFE": (0, 200, 0), "WARNING": (0, 165, 255), "CRITICAL": (0, 0, 255)}

    processed = 0
    for idx, frame_path in enumerate(frame_files):
        if idx % FRAME_SKIP != 0:
            continue
        if processed >= MAX_FRAMES:
            break

        frame = cv2.imread(frame_path)

        # --- Detection + tracking (YOLO) ---
        detections = detector.detect_and_track(frame)
        movement_analyzer.update(detections)
        zone_movements = movement_analyzer.analyze(detections, frame.shape, real_frame_gap=FRAME_SKIP)

        # --- Optical flow fallback for low-tracked zones ---
        flow_results = flow_analyzer.compute_zone_flow(frame)
        for zm in zone_movements:
            if zm.person_count < 2 and (zm.row, zm.col) in flow_results:
                avg_mag, dir_var = flow_results[(zm.row, zm.col)]
                speed_risk = min(avg_mag / 5.0, 1.0)
                chaos_risk = dir_var
                zm.movement_risk = float(min(0.6 * speed_risk + 0.4 * chaos_risk, 1.0))

        # --- Density estimation (CSRNet) ---
        density_map, total_count = estimator.predict(frame)
        zone_results = zone_classifier.classify(density_map, frame.shape)

        # --- Combine into final risk ---
        overall_risks = risk_engine.combine(zone_results, zone_movements)

        # --- Draw ---
        critical_count = 0
        warning_count = 0
        for zr in overall_risks:
            y1, y2 = zr.row * zone_h, (zr.row + 1) * zone_h
            x1, x2 = zr.col * zone_w, (zr.col + 1) * zone_w
            color = color_map[zr.overall_level]

            if zr.overall_level == "CRITICAL":
                critical_count += 1
            elif zr.overall_level == "WARNING":
                warning_count += 1

            cv2.rectangle(frame, (x1, y1), (x2, y2), color, 2)
            cv2.putText(frame, f"{zr.overall_level} ({zr.overall_score:.2f})",
                        (x1 + 5, y1 + 20), cv2.FONT_HERSHEY_SIMPLEX, 0.45, color, 1)
            cv2.putText(frame, f"d:{zr.density_per_sqm:.1f} m:{zr.movement_risk:.2f}",
                        (x1 + 5, y1 + 38), cv2.FONT_HERSHEY_SIMPLEX, 0.4, color, 1)

        cv2.putText(frame, f"Total: {total_count:.0f} | CRITICAL zones: {critical_count} | WARNING zones: {warning_count}",
                    (20, height - 20), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)

        out.write(frame)
        print(f"Frame {idx}: total={total_count:.1f}, critical_zones={critical_count}, warning_zones={warning_count}")
        processed += 1

    out.release()
    print(f"\nDone. Full pipeline video saved to: {OUTPUT_PATH}")


if __name__ == "__main__":
    main()
