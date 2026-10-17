"""
optical_flow.py
Dense optical flow (Farneback method) — measures pixel-level motion
directly between consecutive frames, with NO dependency on detecting
individual people. This fills the exact gap where YOLO tracking misses
people in dense crowds: even if detection finds 0 heads in a zone,
optical flow still measures real motion there from raw pixel changes.
Classical, well-established CV technique; no training required.
"""

import cv2
import numpy as np


class OpticalFlowAnalyzer:
    def __init__(self, grid_rows: int = 4, grid_cols: int = 4):
        self.grid_rows = grid_rows
        self.grid_cols = grid_cols
        self.prev_gray = None

    def compute_zone_flow(self, frame: np.ndarray):
        """
        Returns dict (row, col) -> (avg_magnitude, direction_variance).
        Call once per frame, in sequential order. Returns {} on the very
        first call (no previous frame to compare against yet).
        """
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

        # First frame
        if self.prev_gray is None:
            self.prev_gray = gray
            return {}

        # If frame size changes (common when switching videos or restarting),
        # reset optical flow instead of crashing.
        if self.prev_gray.shape != gray.shape:
            self.prev_gray = gray
            return {}

        flow = cv2.calcOpticalFlowFarneback(
            self.prev_gray, gray, None,
            pyr_scale=0.5, levels=3, winsize=15,
            iterations=3, poly_n=5, poly_sigma=1.2, flags=0
        )
        self.prev_gray = gray

        h, w = gray.shape
        zone_h = h // self.grid_rows
        zone_w = w // self.grid_cols

        results = {}
        for row in range(self.grid_rows):
            for col in range(self.grid_cols):
                y1, y2 = row * zone_h, (row + 1) * zone_h
                x1, x2 = col * zone_w, (col + 1) * zone_w

                zone_flow = flow[y1:y2, x1:x2]
                fx = zone_flow[..., 0].flatten()
                fy = zone_flow[..., 1].flatten()

                magnitudes = np.hypot(fx, fy)
                avg_magnitude = float(np.mean(magnitudes))

                angles = np.arctan2(fy, fx)
                mean_cos = np.mean(np.cos(angles))
                mean_sin = np.mean(np.sin(angles))
                resultant_length = np.hypot(mean_cos, mean_sin)
                direction_variance = float(1 - resultant_length)

                results[(row, col)] = (avg_magnitude, direction_variance)

        return results

