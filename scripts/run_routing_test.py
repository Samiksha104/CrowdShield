"""
run_routing_test.py
Full Phase 1-5 integration test: runs YOLO detection + CSRNet density +
movement analysis + combined risk engine on a real frame, then feeds
that live risk data into the blueprint router to find the safest
evacuation route from the entry point to an exit.

This proves the routing logic responds to REAL risk data, not synthetic
placeholder values.
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
from src.routing.blueprint_router import BlueprintRouter

CONFIG_PATH = os.path.join(os.path.dirname(__file__), "..", "config", "config.yaml")
YOLO_WEIGHTS = os.path.join(os.path.dirname(__file__), "..", "models", "crowd_head_yolov8.pt")
CSRNET_WEIGHTS = os.path.join(os.path.dirname(__file__), "..", "models", "csrnet_shanghaitech_partA.pth")
SEQUENCE_DIR = os.path.join(os.path.dirname(__file__), "..", "data", "raw_videos", "MOT20-02", "img1")
BLUEPRINT_PATH = os.path.join(os.path.dirname(__file__), "..", "data", "blueprints", "example_venue.json")

FRAMES_TO_WARM_UP = 6  # need a few consecutive frames for movement history to build up
START_NODE = "entry_main"


def compute_current_risk(config, detector, estimator, zone_classifier, movement_analyzer,
                          flow_analyzer, risk_engine, frame_files):
    """Runs frames 0..N through the full Phase 1-4 pipeline, returns the
    final combined risk per zone for the last frame."""
    zone_movements = None
    zone_results = None

    for i in range(FRAMES_TO_WARM_UP):
        frame = cv2.imread(frame_files[i])

        detections = detector.detect_and_track(frame)
        movement_analyzer.update(detections)
        zone_movements = movement_analyzer.analyze(detections, frame.shape, real_frame_gap=1)

        flow_results = flow_analyzer.compute_zone_flow(frame)
        for zm in zone_movements:
            if zm.person_count < 2 and (zm.row, zm.col) in flow_results:
                avg_mag, dir_var = flow_results[(zm.row, zm.col)]
                speed_risk = min(avg_mag / 5.0, 1.0)
                chaos_risk = dir_var
                zm.movement_risk = float(min(0.6 * speed_risk + 0.4 * chaos_risk, 1.0))

        density_map, total_count = estimator.predict(frame)
        zone_results = zone_classifier.classify(density_map, frame.shape)

    return risk_engine.combine(zone_results, zone_movements)


def main():
    with open(CONFIG_PATH) as f:
        config = yaml.safe_load(f)

    print("Loading models...")
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
    router = BlueprintRouter(blueprint_path=BLUEPRINT_PATH)

    frame_files = sorted(glob.glob(os.path.join(SEQUENCE_DIR, "*.jpg")))

    print("Computing current risk from real footage (frames 0-5)...")
    overall_risks = compute_current_risk(
        config, detector, estimator, zone_classifier, movement_analyzer,
        flow_analyzer, risk_engine, frame_files
    )

    print("\nZone risk levels:")
    for r in overall_risks:
        print(f"  Zone ({r.row},{r.col}): {r.overall_level} (score={r.overall_score:.2f})")

    print(f"\nFinding safest route from '{START_NODE}'...")
    result = router.find_safest_route(START_NODE, overall_risks)

    if result is None:
        print("No route found!")
    else:
        print(f"\nSafest route: {' -> '.join(result['path'])}")
        print(f"Exits at: {result['exit']}")
        print(f"Total real-world distance: {result['total_distance_m']:.1f} m")
        print(f"Risk-adjusted cost: {result['risk_adjusted_cost']:.1f}")


if __name__ == "__main__":
    main()
