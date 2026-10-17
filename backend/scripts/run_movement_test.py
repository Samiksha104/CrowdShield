"""
run_movement_test.py
Runs YOLO+ByteTrack detection across an MOT20 sequence, feeding each
frame's tracked positions into MovementAnalyzer to compute per-zone
movement risk (speed, direction chaos, convergence).
"""

import sys
import os
import glob
import cv2

sys.path.append(os.path.join(os.path.dirname(__file__), ".."))
from src.detection.detector import CrowdDetector
from src.behavior.optical_flow import OpticalFlowAnalyzer
from src.behavior.social_force import MovementAnalyzer

SEQUENCE_DIR = os.path.join(os.path.dirname(__file__), "..", "data", "raw_videos", "MOT20-02", "img1")
WEIGHTS_PATH = os.path.join(os.path.dirname(__file__), "..", "models", "crowd_head_yolov8.pt")
OUTPUT_PATH = os.path.join(os.path.dirname(__file__), "..", "outputs", "movement_risk_test.mp4")

FRAME_SKIP = 2
MAX_FRAMES = 150


def main():
    detector = CrowdDetector(weights_path=WEIGHTS_PATH, confidence_threshold=0.15, imgsz=1280)
    flow_analyzer = OpticalFlowAnalyzer(grid_rows=4, grid_cols=4)
    analyzer = MovementAnalyzer(history_length=5, grid_rows=4, grid_cols=4)

    frame_files = sorted(glob.glob(os.path.join(SEQUENCE_DIR, "*.jpg")))
    first_frame = cv2.imread(frame_files[0])
    height, width = first_frame.shape[:2]

    fourcc = cv2.VideoWriter_fourcc(*"mp4v")
    out = cv2.VideoWriter(OUTPUT_PATH, fourcc, 5, (width, height))

    processed = 0
    for idx, frame_path in enumerate(frame_files):
        if idx % FRAME_SKIP != 0:
            continue
        if processed >= MAX_FRAMES:
            break
        frame = cv2.imread(frame_path)
        detections = detector.detect_and_track(frame)
        analyzer.update(detections)
        zone_movements = analyzer.analyze(detections, frame.shape)
        flow_results = flow_analyzer.compute_zone_flow(frame)

        # Fallback: where tracked-person count is too low to trust, use
        # optical flow's pixel-level motion instead of defaulting to 0.
        for zm in zone_movements:
            if zm.person_count < 2 and (zm.row, zm.col) in flow_results:
                avg_mag, dir_var = flow_results[(zm.row, zm.col)]
                speed_risk = min(avg_mag / 5.0, 1.0)  # optical flow magnitudes are smaller scale than tracked pixel/frame speed
                chaos_risk = dir_var
                zm.movement_risk = float(min(0.6 * speed_risk + 0.4 * chaos_risk, 1.0))

        zone_h = height // 4
        zone_w = width // 4
        for zm in zone_movements:
            y1, y2 = zm.row * zone_h, (zm.row + 1) * zone_h
            x1, x2 = zm.col * zone_w, (zm.col + 1) * zone_w

            if zm.movement_risk > 0.6:
                color = (0, 0, 255)
            elif zm.movement_risk > 0.3:
                color = (0, 165, 255)
            else:
                color = (0, 200, 0)

            cv2.rectangle(frame, (x1, y1), (x2, y2), color, 2)
            cv2.putText(frame, f"risk:{zm.movement_risk:.2f}", (x1 + 5, y1 + 20),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 1)

        out.write(frame)
        max_risk = max(zm.movement_risk for zm in zone_movements)
        print(f"Frame {idx}: max zone movement risk = {max_risk:.2f}")
        processed += 1

    out.release()
    print(f"\nDone. Movement risk video saved to: {OUTPUT_PATH}")


if __name__ == "__main__":
    main()
