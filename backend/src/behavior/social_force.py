"""
social_force.py
Movement analysis inspired by the Social Force Model concept: uses YOLO's
tracked positions (per-person, across frames) to detect crowd movement
patterns that precede stampedes — sudden speed increases, people
converging toward one point, and counter-flow (opposing movement).

This does NOT reimplement full SFM physics (repulsive/attractive force
equations) — that's a heavier research-grade simulation. Instead, this
extracts the same KEY WARNING SIGNALS that SFM-based papers look for,
computed directly from tracked trajectories. This is a legitimate,
documented simplification — worth stating clearly as a design choice.
"""

from collections import defaultdict, deque
from dataclasses import dataclass
from typing import List, Dict, Tuple
import numpy as np


@dataclass
class ZoneMovement:
    row: int
    col: int
    avg_speed: float           # pixels/frame, average movement speed in zone
    direction_variance: float  # 0 = everyone moving same direction, 1 = totally chaotic
    convergence_score: float   # -1 (spreading out) to +1 (converging to one point)
    movement_risk: float       # combined 0-1 risk score from the three signals above
    person_count: int = 0

class MovementAnalyzer:
    def __init__(self, history_length: int = 5, grid_rows: int = 4, grid_cols: int = 4):
        """
        history_length: how many past positions to remember per person,
                        used to smooth velocity estimation.
        """
        self.history_length = history_length
        self.grid_rows = grid_rows
        self.grid_cols = grid_cols
        # track_id -> deque of (x, y) positions, most recent last
        self.position_history: Dict[int, deque] = defaultdict(lambda: deque(maxlen=history_length))

    def update(self, detections: List) -> None:
        """Call this once per frame with the current frame's Detection list
        (from detector.py) to update tracked position history."""
        for det in detections:
            cx, cy = det.centroid
            self.position_history[det.track_id].append((cx, cy))

    def _compute_velocity(self, track_id: int, real_frame_gap: int = 1) -> Tuple[float, float]:
        """Returns (vx, vy) PER REAL VIDEO FRAME — critical when frames are
        being skipped (FRAME_SKIP > 1), since the raw history only stores
        one entry per PROCESSED frame, not per real frame. Without this
        correction, movement_risk scales incorrectly with whatever
        FRAME_SKIP happens to be set to, which is a methodological bug,
        not a reflection of real crowd speed."""
        history = self.position_history[track_id]
        if len(history) < 2:
            return (0.0, 0.0)
        (x1, y1), (x2, y2) = history[0], history[-1]
        processed_steps = len(history) - 1
        total_real_frames = processed_steps * real_frame_gap
        return ((x2 - x1) / total_real_frames, (y2 - y1) / total_real_frames)

    def analyze(self, detections: List, frame_shape: tuple, real_frame_gap: int = 1) -> List[ZoneMovement]:
        """
        Computes per-zone movement risk for the CURRENT frame's detections.
        Call update() first, every frame, before calling this.
        """
        h, w = frame_shape[:2]
        zone_h = h // self.grid_rows
        zone_w = w // self.grid_cols

        # Bucket each currently-detected person into a zone, with their velocity
        zone_data = defaultdict(list)  # (row, col) -> list of (vx, vy, speed, angle)

        for det in detections:
            cx, cy = det.centroid
            row = min(int(cy // zone_h), self.grid_rows - 1)
            col = min(int(cx // zone_w), self.grid_cols - 1)

            vx, vy = self._compute_velocity(det.track_id, real_frame_gap=real_frame_gap)
            speed = np.hypot(vx, vy)
            angle = np.arctan2(vy, vx)

            zone_data[(row, col)].append((vx, vy, speed, angle, cx, cy))

        results = []
        for row in range(self.grid_rows):
            for col in range(self.grid_cols):
                people = zone_data.get((row, col), [])

                if len(people) < 2:
                    # Not enough people to assess movement pattern meaningfully
                    results.append(ZoneMovement(row, col, 0.0, 0.0, 0.0, 0.0, person_count=len(people)))
                    continue

                speeds = np.array([p[2] for p in people])
                angles = np.array([p[3] for p in people])

                avg_speed = float(np.mean(speeds))

                # Direction variance: circular variance (0 = aligned, 1 = chaotic)
                mean_cos = np.mean(np.cos(angles))
                mean_sin = np.mean(np.sin(angles))
                resultant_length = np.hypot(mean_cos, mean_sin)
                direction_variance = float(1 - resultant_length)

                # Convergence: are people moving TOWARD the zone's own centroid?
                centroid_x = np.mean([p[4] for p in people])
                centroid_y = np.mean([p[5] for p in people])

                convergence_values = []
                for vx, vy, speed, angle, cx, cy in people:
                    if speed < 1e-3:
                        continue
                    to_centroid = np.array([centroid_x - cx, centroid_y - cy])
                    to_centroid_norm = to_centroid / (np.linalg.norm(to_centroid) + 1e-6)
                    velocity_norm = np.array([vx, vy]) / speed
                    convergence_values.append(np.dot(to_centroid_norm, velocity_norm))

                convergence_score = float(np.mean(convergence_values)) if convergence_values else 0.0

                # Combine into one risk score (0-1). Weights are a documented
                # design choice, not derived from data — reasonable starting
                # point, tunable later against real incident data if available.
                speed_risk = min(avg_speed / 15.0, 1.0)       # normalize; >15 px/frame ~ running
                chaos_risk = direction_variance                 # already 0-1
                converge_risk = max(convergence_score, 0.0)      # only positive (converging) is risky

                movement_risk = float(np.clip(
                    0.4 * speed_risk + 0.3 * chaos_risk + 0.3 * converge_risk, 0.0, 1.0
                ))

                results.append(ZoneMovement(
                    row, col, avg_speed, direction_variance, convergence_score, movement_risk,person_count=len(people)

                ))

        return results
 
