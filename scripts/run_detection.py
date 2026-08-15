"""
run_detection.py
Quick end-to-end test: runs the CrowdDetector on a video file,
draws bounding boxes + unique IDs on each frame, prints live count,
and saves an annotated output video so you can visually verify it works.
"""

import sys
import os
import cv2

# Allow importing from src/
sys.path.append(os.path.join(os.path.dirname(__file__), ".."))
from src.detection.detector import CrowdDetector

VIDEO_PATH = os.path.join(os.path.dirname(__file__), "..", "data", "raw_videos", "railway_sample.mp4")
OUTPUT_PATH = os.path.join(os.path.dirname(__file__), "..", "outputs", "railway_sample_annotated.mp4")
WEIGHTS_PATH = os.path.join(os.path.dirname(__file__), "..", "models", "crowd_head_yolov8.pt") # auto-downloads stock model; swap later for fine-tuned head model

FRAME_SKIP = 2  # process every 2nd frame (CPU-friendly)


def main():
    detector = CrowdDetector(weights_path=WEIGHTS_PATH, confidence_threshold=0.35)

    cap = cv2.VideoCapture(VIDEO_PATH)
    if not cap.isOpened():
        print(f"ERROR: could not open video at {VIDEO_PATH}")
        return

    fps = cap.get(cv2.CAP_PROP_FPS) or 20
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))

    fourcc = cv2.VideoWriter_fourcc(*"mp4v")
    out = cv2.VideoWriter(OUTPUT_PATH, fourcc, fps, (width, height))

    frame_idx = 0
    while True:
        ret, frame = cap.read()
        if not ret:
            break

        frame_idx += 1
        if frame_idx % FRAME_SKIP != 0:
            continue

        detections = detector.detect_and_track(frame)
        count = detector.unique_count(detections)

        for det in detections:
            cv2.rectangle(frame, (int(det.x1), int(det.y1)), (int(det.x2), int(det.y2)), (0, 255, 0), 2)
            cv2.putText(frame, f"ID:{det.track_id}", (int(det.x1), int(det.y1) - 5),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 1)

        cv2.putText(frame, f"Count: {count}", (20, 40),
                    cv2.FONT_HERSHEY_SIMPLEX, 1.0, (0, 0, 255), 2)

        out.write(frame)
        print(f"Frame {frame_idx}: {count} people detected")

    cap.release()
    out.release()
    print(f"\nDone. Annotated video saved to: {OUTPUT_PATH}")


if __name__ == "__main__":
    main()
