"""
blueprint_router.py
Real evacuation routing over an actual venue blueprint (gates, junctions,
walkable paths with real distances) — not an abstract grid. Each node is
tagged with which zone (row, col) of our risk grid it physically sits in,
so real-time Safe/Warning/Critical data drives the routing decision, on
top of real physical distances.

Supports resolving a route request from ANY camera zone (not just a
fixed start node) via nearest_node_to_zone() — this is what lets the API
answer "what's the safest route for the crowd in THIS camera's zone."
"""

import json
import networkx as nx
from typing import List, Tuple, Optional, Dict


class BlueprintRouter:
    def __init__(self, blueprint_path: str):
        with open(blueprint_path) as f:
            self.blueprint = json.load(f)

        self.node_zone_map: Dict[str, Tuple[int, int]] = {
            node["id"]: (node["zone_row"], node["zone_col"]) for node in self.blueprint["nodes"]
        }
        self.exit_nodes = [node["id"] for node in self.blueprint["nodes"] if node["type"] == "exit"]

        # Reverse lookup: zone (row, col) -> nearest blueprint node.
        self.zone_to_node = {v: k for k, v in self.node_zone_map.items()}

        # Risk level -> edge cost multiplier applied to real distance.
        self.risk_multiplier = {"SAFE": 1.0, "WARNING": 5.0, "CRITICAL": 200.0}

    def nearest_node_to_zone(self, zone: Tuple[int, int]) -> Optional[str]:
        """
        Finds the blueprint node closest to a given (row, col) zone.
        Exact match first; otherwise picks the node with the smallest
        grid distance, so ANY camera zone can be used as a routing
        start point, not just zones with a node exactly on them.
        """
        if zone in self.zone_to_node:
            return self.zone_to_node[zone]

        if not self.node_zone_map:
            return None

        best_node = None
        best_distance = float("inf")
        for node_id, node_zone in self.node_zone_map.items():
            distance = abs(node_zone[0] - zone[0]) + abs(node_zone[1] - zone[1])
            if distance < best_distance:
                best_distance = distance
                best_node = node_id

        return best_node

    def build_graph(self, overall_risks: list) -> nx.Graph:
        risk_lookup = {(r.row, r.col): r.overall_level for r in overall_risks}

        G = nx.Graph()
        for node in self.blueprint["nodes"]:
            G.add_node(node["id"])

        for edge in self.blueprint["edges"]:
            from_id, to_id = edge["from"], edge["to"]
            distance = edge["distance_m"]

            dest_zone = self.node_zone_map.get(to_id)
            risk_level = risk_lookup.get(dest_zone, "SAFE")
            multiplier = self.risk_multiplier[risk_level]

            weighted_cost = distance * multiplier
            G.add_edge(from_id, to_id, weight=weighted_cost, real_distance=distance)

        return G

    def find_safest_route(self, start_node: str, overall_risks: list) -> Optional[dict]:
        G = self.build_graph(overall_risks)

        best_path = None
        best_cost = float("inf")
        best_real_distance = None
        best_exit = None

        for exit_node in self.exit_nodes:
            if exit_node not in G or start_node not in G:
                continue
            try:
                path = nx.dijkstra_path(G, start_node, exit_node, weight="weight")
                cost = nx.dijkstra_path_length(G, start_node, exit_node, weight="weight")
                if cost < best_cost:
                    best_cost = cost
                    best_path = path
                    best_exit = exit_node
                    real_distance = sum(
                        G[path[i]][path[i + 1]]["real_distance"] for i in range(len(path) - 1)
                    )
                    best_real_distance = real_distance
            except nx.NetworkXNoPath:
                continue

        if best_path is None:
            return None

        return {
            "path": best_path,
            "exit": best_exit,
            "total_distance_m": best_real_distance,
            "risk_adjusted_cost": best_cost,
        }
