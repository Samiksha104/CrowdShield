"""
run_detection_mot.py
Same pipeline as run_detection.py, but reads an MOT-format image sequence
(img1/000001.jpg, img1/000002.jpg, ...) instead of a single video file.

Now using the fine-tuned head-detection model, lower confidence threshold,
and higher inference resolution to catch small/distant heads in wide-angle
CCTV-style footage (e.g. MOT20).
"""

import sys
import os
import glob
import cv2

sys.path.append(os.path.join(os.path.dirname(__file__), ".."))
from src.detection.detector import CrowdDetector

SEQUENCE_DIR = os.path.join(os.path.dirname(__file__), "..", "data", "raw_videos", "MOT20-02", "img1")
OUTPUT_PATH = os.path.join(os.path.dirname(__file__), "..", "outputs", "mot20_02_annotated.mp4")
WEIGHTS_PATH = os.path.join(os.path.dirname(__file__), "..", "models", "crowd_head_yolov8_v2.pt")

FRAME_SKIP = 3       # MOT20 is dense + large, skip more frames on CPU
MAX_FRAMES = 300     # limit for a quick test run; remove/raise later for full run


def main():
    detector = CrowdDetector(
        weights_path=WEIGHTS_PATH,
        confidence_threshold=0.15,   # lowered to catch more uncertain/small detections
        imgsz=1280,                  # raised resolution to catch small/distant heads
    )

    frame_files = sorted(glob.glob(os.path.join(SEQUENCE_DIR, "*.jpg")))
    if not frame_files:
        print(f"ERROR: no frames found in {SEQUENCE_DIR}")
        return

    print(f"Found {len(frame_files)} frames. Processing up to {MAX_FRAMES} (every {FRAME_SKIP}th frame)...")

    first_frame = cv2.imread(frame_files[0])
    height, width = first_frame.shape[:2]
    fourcc = cv2.VideoWriter_fourcc(*"mp4v")
    out = cv2.VideoWriter(OUTPUT_PATH, fourcc, 10, (width, height))

    processed = 0
    for idx, frame_path in enumerate(frame_files):
        if idx % FRAME_SKIP != 0:
            continue
        if processed >= MAX_FRAMES:
            break

        frame = cv2.imread(frame_path)
        detections = detector.detect_and_track(frame)
        count = detector.unique_count(detections)

        for det in detections:
            cv2.rectangle(frame, (int(det.x1), int(det.y1)), (int(det.x2), int(det.y2)), (0, 255, 0), 2)
            cv2.putText(frame, f"ID:{det.track_id}", (int(det.x1), int(det.y1) - 5),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 1)

        cv2.putText(frame, f"Count: {count}", (20, 40),
                    cv2.FONT_HERSHEY_SIMPLEX, 1.0, (0, 0, 255), 2)

        out.write(frame)
        print(f"Frame {idx}: {count} people detected")
        processed += 1

    out.release()
    print(f"\nDone. Annotated video saved to: {OUTPUT_PATH}")


if __name__ == "__main__":
    main()
