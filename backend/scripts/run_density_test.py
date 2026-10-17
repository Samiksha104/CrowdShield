"""
run_density_test.py
Tests the CSRNet density estimator on a single frame from your MOT20
sequence, saving a heatmap overlay so you can visually verify it.
"""

import sys
import os
import cv2
import glob

sys.path.append(os.path.join(os.path.dirname(__file__), ".."))
from src.density.density_estimator import DensityEstimator

WEIGHTS_PATH = os.path.join(os.path.dirname(__file__), "..", "models", "csrnet_shanghaitech_partA.pth")
SEQUENCE_DIR = os.path.join(os.path.dirname(__file__), "..", "data", "raw_videos", "MOT20-02", "img1")
OUTPUT_PATH = os.path.join(os.path.dirname(__file__), "..", "outputs", "csrnet_heatmap_test.jpg")

TEST_FRAME_INDEX = 100  # pick a mid-sequence frame, likely dense


def main():
    print("Loading CSRNet model...")
    estimator = DensityEstimator(weights_path=WEIGHTS_PATH, device="cpu")

    frame_files = sorted(glob.glob(os.path.join(SEQUENCE_DIR, "*.jpg")))
    frame_path = frame_files[TEST_FRAME_INDEX]
    frame = cv2.imread(frame_path)

    print(f"Running inference on {frame_path}...")
    density_map, estimated_count = estimator.predict(frame)

    print(f"Estimated count: {estimated_count:.1f}")

    overlay = estimator.generate_heatmap_overlay(frame, density_map)
    cv2.putText(overlay, f"Estimated Count: {estimated_count:.0f}", (20, 40),
                cv2.FONT_HERSHEY_SIMPLEX, 1.0, (255, 255, 255), 2)

    cv2.imwrite(OUTPUT_PATH, overlay)
    print(f"Heatmap saved to: {OUTPUT_PATH}")


if __name__ == "__main__":
    main()
