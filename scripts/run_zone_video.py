"""
run_zone_video.py
Runs CSRNet density estimation + zone risk classification continuously
across an MOT-format image sequence, producing a video with the risk
overlay evolving frame by frame — a proper demo output, not a single
static image.
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
OUTPUT_PATH = os.path.join(os.path.dirname(__file__), "..", "outputs", "zone_risk_video.mp4")

FRAME_SKIP = 3       # CSRNet is heavier than YOLO on CPU; skip more frames
MAX_FRAMES = 150     # ~limit for a reasonable runtime test; raise later for full run


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
    if not frame_files:
        print(f"ERROR: no frames found in {SEQUENCE_DIR}")
        return

    first_frame = cv2.imread(frame_files[0])
    height, width = first_frame.shape[:2]
    fourcc = cv2.VideoWriter_fourcc(*"mp4v")
    out = cv2.VideoWriter(OUTPUT_PATH, fourcc, 5, (width, height))  # 5 fps output, since we skip frames heavily

    processed = 0
    for idx, frame_path in enumerate(frame_files):
        if idx % FRAME_SKIP != 0:
            continue
        if processed >= MAX_FRAMES:
            break

        frame = cv2.imread(frame_path)
        density_map, total_count = estimator.predict(frame)
        zones = classifier.classify(density_map, frame.shape)
        overlay = classifier.draw_zones(frame, zones)

        critical_zones = sum(1 for z in zones if z.risk_level == "CRITICAL")
        warning_zones = sum(1 for z in zones if z.risk_level == "WARNING")

        cv2.putText(overlay, f"Total: {total_count:.0f} | Critical zones: {critical_zones} | Warning zones: {warning_zones}",
                    (20, height - 20), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)

        out.write(overlay)
        print(f"Frame {idx}: total={total_count:.1f}, critical={critical_zones}, warning={warning_zones}")
        processed += 1

    out.release()
    print(f"\nDone. Zone risk video saved to: {OUTPUT_PATH}")


if __name__ == "__main__":
    main()
