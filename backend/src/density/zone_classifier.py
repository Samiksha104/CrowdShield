"""
zone_classifier.py
Takes a CSRNet density map, divides the frame into a grid of zones,
computes real-world density (people/sqm) per zone using area calibration,
and classifies each zone as SAFE / WARNING / CRITICAL.

Thresholds are based on established crowd-safety literature (Fruin/Still
levels of service), not arbitrary numbers — already defined in config.yaml.

IMPORTANT LIMITATION (documented honestly, not hidden): this assumes each
grid zone covers equal real-world ground area. In a real camera view with
perspective, distant zones (top of frame) cover MORE real ground area per
pixel than near zones (bottom of frame). This is a simplification for v1 —
a proper fix requires a homography/perspective calibration per camera,
noted as future work.
"""

from dataclasses import dataclass
from typing import List
import numpy as np
import cv2


@dataclass
class ZoneResult:
    row: int
    col: int
    x1: int
    y1: int
    x2: int
    y2: int
    estimated_count: float
    density_per_sqm: float
    risk_level: str  # "SAFE", "WARNING", "CRITICAL"


class ZoneClassifier:
    def __init__(self, grid_rows: int, grid_cols: int, total_ground_area_sqm: float,
                 safe_threshold: float, warning_threshold: float):
        self.grid_rows = grid_rows
        self.grid_cols = grid_cols
        self.total_area = total_ground_area_sqm
        self.zone_area = total_ground_area_sqm / (grid_rows * grid_cols)
        self.safe_threshold = safe_threshold
        self.warning_threshold = warning_threshold

    def classify(self, density_map: np.ndarray, frame_shape: tuple) -> List[ZoneResult]:
        """
        density_map: raw CSRNet output (low-res, e.g. 88x128)
        frame_shape: (height, width) of the ORIGINAL frame, for drawing coords
        """
        h, w = frame_shape[:2]
        density_resized = cv2.resize(density_map, (w, h))

        zone_h = h // self.grid_rows
        zone_w = w // self.grid_cols

        results = []
        for row in range(self.grid_rows):
            for col in range(self.grid_cols):
                y1, y2 = row * zone_h, (row + 1) * zone_h
                x1, x2 = col * zone_w, (col + 1) * zone_w

                zone_density_map = density_resized[y1:y2, x1:x2]
                # Sum over resized map overcounts vs original resolution;
                # scale by the resize ratio to keep count consistent.
                original_h, original_w = density_map.shape
                scale_factor = (original_h * original_w) / (h * w)
                estimated_count = zone_density_map.sum() * scale_factor

                density_per_sqm = estimated_count / self.zone_area

                if density_per_sqm <= self.safe_threshold:
                    risk = "SAFE"
                elif density_per_sqm <= self.warning_threshold:
                    risk = "WARNING"
                else:
                    risk = "CRITICAL"

                results.append(ZoneResult(
                    row=row, col=col, x1=x1, y1=y1, x2=x2, y2=y2,
                    estimated_count=estimated_count,
                    density_per_sqm=density_per_sqm,
                    risk_level=risk
                ))

        return results

    def draw_zones(self, frame: np.ndarray, zones: List[ZoneResult]) -> np.ndarray:
        """Draws zone boundaries, colored by risk level, with count labels."""
        color_map = {
            "SAFE": (0, 200, 0),
            "WARNING": (0, 165, 255),
            "CRITICAL": (0, 0, 255),
        }
        overlay = frame.copy()

        for zone in zones:
            color = color_map[zone.risk_level]
            cv2.rectangle(overlay, (zone.x1, zone.y1), (zone.x2, zone.y2), color, 2)
            label = f"{zone.estimated_count:.0f} ({zone.density_per_sqm:.1f}/sqm)"
            cv2.putText(overlay, label, (zone.x1 + 5, zone.y1 + 20),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 1)
            cv2.putText(overlay, zone.risk_level, (zone.x1 + 5, zone.y1 + 40),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 1)

        return overlay
