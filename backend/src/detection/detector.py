"""
detector.py
Wraps YOLOv8 + ByteTrack tracking to produce per-frame detections with
STABLE unique IDs (so the same person isn't recounted as a new person
between frames). This is the foundation for accurate headcount/density.

Now configured to use the fine-tuned head-detection model
(crowd_head_yolov8.pt), trained on SCUT-HEAD. imgsz is adjustable so we
can boost resolution for small/distant heads in wide-angle CCTV footage.
"""

from dataclasses import dataclass
from typing import List
from ultralytics import YOLO
import numpy as np


@dataclass
class Detection:
    track_id: int
    x1: float
    y1: float
    x2: float
    y2: float
    confidence: float

    @property
    def centroid(self):
        return ((self.x1 + self.x2) / 2.0, (self.y1 + self.y2) / 2.0)


class CrowdDetector:
    def __init__(self, weights_path: str, confidence_threshold: float = 0.35,
                 tracker_config: str = "bytetrack.yaml", person_class_id: int = 0,
                 imgsz: int = 640):
        """
        weights_path: path to .pt file (your fine-tuned head model).
        person_class_id: class 0 = 'head' in the fine-tuned model.
        imgsz: inference resolution. Raise this (e.g. 1280) to catch
               small/distant heads in wide-angle CCTV shots, at the
               cost of slower inference on CPU.
        """
        self.model = YOLO(weights_path)
        self.confidence_threshold = confidence_threshold
        self.tracker_config = tracker_config
        self.person_class_id = person_class_id
        self.imgsz = imgsz

    def detect_and_track(self, frame: np.ndarray) -> List[Detection]:
        """
        Runs detection + tracking on a single frame.
        Returns a list of Detection objects, each with a STABLE track_id
        that persists across frames for the same physical person.
        """
        results = self.model.track(
            frame,
            persist=True,                     # keep ID memory across calls
            conf=self.confidence_threshold,
            tracker=self.tracker_config,
            classes=[self.person_class_id],
            imgsz=self.imgsz,
            verbose=False,
        )

        detections = []
        if results and results[0].boxes is not None and results[0].boxes.id is not None:
            boxes = results[0].boxes.xyxy.cpu().numpy()
            ids = results[0].boxes.id.cpu().numpy().astype(int)
            confs = results[0].boxes.conf.cpu().numpy()

            for (x1, y1, x2, y2), track_id, conf in zip(boxes, ids, confs):
                detections.append(Detection(
                    track_id=int(track_id),
                    x1=float(x1), y1=float(y1), x2=float(x2), y2=float(y2),
                    confidence=float(conf),
                ))

        return detections

    def unique_count(self, detections: List[Detection]) -> int:
        """Count of unique people detected in THIS frame (not cumulative)."""
        return len(detections)
