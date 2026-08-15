"""
risk_engine.py
Combines density risk (from CSRNet + zone_classifier) and movement risk
(from social_force.py + optical_flow.py) into ONE overall risk score per
zone, using the weights defined in config.yaml. This is the final
Safe/Warning/Critical decision your system actually acts on.

Density weight (0.6) > movement weight (0.4) by default — the reasoning:
density is a more directly measurable, physically grounded signal (people
per sqm, tied to established crowd-safety research), while movement risk
is a heuristic behavioral signal. This weighting is a documented design
choice, tunable later against real incident data if available.
"""

from dataclasses import dataclass


@dataclass
class OverallRisk:
    row: int
    col: int
    density_per_sqm: float
    movement_risk: float
    density_risk_score: float   # normalized 0-1, for fair combination with movement
    overall_score: float        # combined 0-1 score
    overall_level: str          # SAFE / WARNING / CRITICAL


class RiskEngine:
    def __init__(self, density_weight: float, movement_weight: float,
                 safe_threshold: float, warning_threshold: float):
        """
        density_weight, movement_weight: from config.yaml, should sum to 1.0
        safe_threshold, warning_threshold: density people/sqm thresholds,
            reused here to normalize density into a comparable 0-1 score
            (density_risk_score = 1.0 means density is at/above the
            'warning' threshold, not an arbitrary scale).
        """
        self.density_weight = density_weight
        self.movement_weight = movement_weight
        self.safe_threshold = safe_threshold
        self.warning_threshold = warning_threshold

    def _normalize_density(self, density_per_sqm: float) -> float:
        """Maps real density (people/sqm) to a 0-1 risk score, anchored to
        the same safe/warning thresholds used elsewhere — so 1.0 always
        means 'at the warning threshold', not an arbitrary max."""
        if density_per_sqm <= 0:
            return 0.0
        return float(min(density_per_sqm / self.warning_threshold, 1.0))

    def combine(self, zone_results: list, zone_movements: list) -> list:
        """
        zone_results: list of ZoneResult from zone_classifier.py
        zone_movements: list of ZoneMovement from social_force.py (already
                         merged with optical flow fallback)
        Returns list of OverallRisk, one per zone.
        """
        movement_lookup = {(zm.row, zm.col): zm.movement_risk for zm in zone_movements}

        results = []
        for zr in zone_results:
            movement_risk = movement_lookup.get((zr.row, zr.col), 0.0)
            density_risk_score = self._normalize_density(zr.density_per_sqm)

            overall_score = (
                self.density_weight * density_risk_score +
                self.movement_weight * movement_risk
            )

            if overall_score <= 0.33:
                level = "SAFE"
            elif overall_score <= 0.66:
                level = "WARNING"
            else:
                level = "CRITICAL"

            results.append(OverallRisk(
                row=zr.row, col=zr.col,
                density_per_sqm=zr.density_per_sqm,
                movement_risk=movement_risk,
                density_risk_score=density_risk_score,
                overall_score=overall_score,
                overall_level=level,
            ))

        return results
