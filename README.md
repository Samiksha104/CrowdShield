# CrowdShield AI Backend

AI pipeline for real-time crowd density estimation, movement analysis, risk classification, and evacuation routing.

## Setup

```bash
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

## Running the API server

```bash
uvicorn api_server:app --host 0.0.0.0 --port 8000
```

Server takes ~10-20 seconds to boot (loading YOLO + CSRNet models).

## Endpoints (matches Android app contract)

- `GET /api/health` — server status
- `GET /api/zones` — per-zone risk data for heatmap
- `GET /api/cameras` — camera status (currently single camera; multi-camera is future work)
- `GET /api/alerts` — alert history (auto-populated when a zone goes CRITICAL)
- `GET /api/evacuation/routes` — all exits with ETA (optionally pass `?zone_row=X&zone_col=Y` for location-specific routing)
- `GET /api/risk-prediction` — aggregate risk score + recommendations

## Switching video source (for demo/testing)

```bash
curl -X POST http://localhost:8000/api/set-video-source \
  -H "Content-Type: application/json" \
  -d '{"path": "/absolute/path/to/video.mp4"}'
```

## Known limitations (documented honestly)

- `ground_area_sqm` in `config/config.yaml` is a placeholder — real deployment requires site-specific measurement for accurate density-per-sqm calibration.
- Evacuation routing uses a synthetic example blueprint (`data/blueprints/example_venue.json`) — not tied to a real venue yet.
- `/api/cameras` returns a single simulated camera; true multi-camera support is a planned extension.
- Models trained on ShanghaiTech (density) and SCUT-HEAD (detection) — accuracy on genuinely novel camera angles/lighting is untested until validated on real deployment footage.
