"""
density_estimator.py
CSRNet-based density estimation: takes a frame, outputs a density map
(heatmap where each pixel represents local crowd density) and a total
estimated headcount. This is the correct tool for ultra-dense crowds
where individual head detection (YOLO) breaks down.
"""

import torch
import torch.nn as nn
from torchvision import models
import numpy as np
import cv2


class CSRNet(nn.Module):
    def __init__(self, load_weights=False):
        super(CSRNet, self).__init__()
        self.frontend_feat = [64, 64, 'M', 128, 128, 'M', 256, 256, 256, 'M', 512, 512, 512]
        self.backend_feat = [512, 512, 512, 256, 128, 64]

        self.frontend = self._make_layers(self.frontend_feat)
        self.backend = self._make_layers(self.backend_feat, in_channels=512, dilation=True)
        self.output_layer = nn.Conv2d(64, 1, kernel_size=1)

        if load_weights:
            vgg16 = models.vgg16(weights=models.VGG16_Weights.IMAGENET1K_V1)
            self._load_vgg_weights(vgg16)

    def _make_layers(self, cfg, in_channels=3, dilation=False):
        d_rate = 2 if dilation else 1
        layers = []
        for v in cfg:
            if v == 'M':
                layers += [nn.MaxPool2d(kernel_size=2, stride=2)]
            else:
                conv2d = nn.Conv2d(in_channels, v, kernel_size=3, padding=d_rate, dilation=d_rate)
                layers += [conv2d, nn.ReLU(inplace=True)]
                in_channels = v
        return nn.Sequential(*layers)

    def _load_vgg_weights(self, vgg16):
        vgg_state = list(vgg16.features.state_dict().items())
        own_state = self.frontend.state_dict()
        own_keys = list(own_state.keys())
        for i, (key, _) in enumerate(vgg_state):
            if i >= len(own_keys):
                break
            own_state[own_keys[i]] = vgg_state[i][1]
        self.frontend.load_state_dict(own_state)

    def forward(self, x):
        x = self.frontend(x)
        x = self.backend(x)
        x = self.output_layer(x)
        return x


class DensityEstimator:
    def __init__(self, weights_path: str, device: str = "cpu"):
        self.device = torch.device(device)
        self.model = CSRNet(load_weights=False)
        self.model.load_state_dict(torch.load(weights_path, map_location=self.device))
        self.model.to(self.device)
        self.model.eval()

    def predict(self, frame: np.ndarray):
        """
        frame: BGR image (as read by cv2.imread/VideoCapture)
        Returns: (density_map, estimated_count)
        density_map is a low-res 2D array; resize it to frame size for overlay.
        """
        img = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB).astype(np.float32) / 255.0
        img_tensor = torch.from_numpy(img.transpose(2, 0, 1)).unsqueeze(0).to(self.device)

        with torch.no_grad():
            output = self.model(img_tensor)

        density_map = output.squeeze().cpu().numpy()
        estimated_count = density_map.sum()  # undo the *64 scaling from training

        return density_map, float(estimated_count)

    def generate_heatmap_overlay(self, frame: np.ndarray, density_map: np.ndarray, alpha: float = 0.5):
        """Resizes density map to frame size and overlays as a color heatmap."""
        h, w = frame.shape[:2]
        density_resized = cv2.resize(density_map, (w, h))
        density_norm = cv2.normalize(density_resized, None, 0, 255, cv2.NORM_MINMAX).astype(np.uint8)
        heatmap = cv2.applyColorMap(density_norm, cv2.COLORMAP_JET)
        overlay = cv2.addWeighted(frame, 1 - alpha, heatmap, alpha, 0)
        return overlay
