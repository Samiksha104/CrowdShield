"""
debug_risk_breakdown.py
Prints the raw density_risk_score and movement_risk for every zone on
ONE frame, so we can see exactly which component is pushing zones into
WARNING/CRITICAL — instead of guessing calibration values blindly.
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

FRAMES_TO_WARM_UP = 6  # process a few frames first so movement history builds up


def main():
    with open(CONFIG_PATH) as f:
        config = yaml.safe_load(f)

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

    zone_movements = None
    zone_results = None
    for i in range(FRAMES_TO_WARM_UP):
        frame = cv2.imread(frame_files[i])
        detections = detector.detect_and_track(frame)
        movement_analyzer.update(detections)
        zone_movements = movement_analyzer.analyze(detections, frame.shape)

        flow_results = flow_analyzer.compute_zone_flow(frame)
        for zm in zone_movements:
            if zm.person_count < 2 and (zm.row, zm.col) in flow_results:
                avg_mag, dir_var = flow_results[(zm.row, zm.col)]
                speed_risk = min(avg_mag / 5.0, 1.0)
                chaos_risk = dir_var
                zm.movement_risk = float(min(0.6 * speed_risk + 0.4 * chaos_risk, 1.0))

        density_map, total_count = estimator.predict(frame)
        zone_results = zone_classifier.classify(density_map, frame.shape)

    overall_risks = risk_engine.combine(zone_results, zone_movements)

    print(f"{'Zone':<8}{'Density/sqm':<14}{'DensityRisk':<14}{'MovementRisk':<14}{'Overall':<10}{'Level'}")
    for zr in overall_risks:
        print(f"({zr.row},{zr.col})   {zr.density_per_sqm:<14.2f}{zr.density_risk_score:<14.2f}"
              f"{zr.movement_risk:<14.2f}{zr.overall_score:<10.2f}{zr.overall_level}")


if __name__ == "__main__":
    main()

