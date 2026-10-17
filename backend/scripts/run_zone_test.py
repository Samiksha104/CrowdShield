"""
run_zone_test.py
Ties together CSRNet density estimation + zone classification on a single
frame. Reads real-world area and thresholds from config.yaml (not
hardcoded), so this stays configurable per venue.
"""

import sys
import os
import cv2
import glob
import yaml

sys.path.append(os.path.join(os.path.dirname(__file__), ".."))
from src.density.density_estimator import DensityEstimator
from src.density.zone_classifier import ZoneClassifier

CONFIG_PATH = os.path.join(os.path.dirname(__file__), "..", "config", "config.yaml")
WEIGHTS_PATH = os.path.join(os.path.dirname(__file__), "..", "models", "csrnet_shanghaitech_partA.pth")
SEQUENCE_DIR = os.path.join(os.path.dirname(__file__), "..", "data", "raw_videos", "MOT20-02", "img1")
OUTPUT_PATH = os.path.join(os.path.dirname(__file__), "..", "outputs", "zone_risk_test.jpg")

TEST_FRAME_INDEX = 100


def main():
    with open(CONFIG_PATH) as f:
        config = yaml.safe_load(f)

    print("Loading CSRNet model...")
    estimator = DensityEstimator(weights_path=WEIGHTS_PATH, device="cpu")

    classifier = ZoneClassifier(
        grid_rows=config["density"]["grid_rows"],
        grid_cols=config["density"]["grid_cols"],
        total_ground_area_sqm=config["density"]["ground_area_sqm"],
        safe_threshold=config["density"]["density_thresholds"]["safe"],
        warning_threshold=config["density"]["density_thresholds"]["warning"],
    )

    frame_files = sorted(glob.glob(os.path.join(SEQUENCE_DIR, "*.jpg")))
    frame_path = frame_files[TEST_FRAME_INDEX]
    frame = cv2.imread(frame_path)

    print(f"Running inference on {frame_path}...")
    density_map, total_count = estimator.predict(frame)
    print(f"Total estimated count (whole frame): {total_count:.1f}")

    zones = classifier.classify(density_map, frame.shape)

    print("\nZone breakdown:")
    for zone in zones:
        print(f"  Zone ({zone.row},{zone.col}): {zone.estimated_count:.1f} people, "
              f"{zone.density_per_sqm:.2f} people/sqm -> {zone.risk_level}")

    overlay = classifier.draw_zones(frame, zones)
    cv2.imwrite(OUTPUT_PATH, overlay)
    print(f"\nZone risk overlay saved to: {OUTPUT_PATH}")


if __name__ == "__main__":
    main()
