# CrowdShield – AI-Powered Crowd Monitoring and Safety

CrowdShield is an AI-powered crowd monitoring backend for real-time crowd density estimation, movement analysis, risk classification, and evacuation routing. It is designed to work with the CrowdShield Android application.

## Project Structure

```text
CrowdShield/
├── backend/
│   ├── api_server.py
│   ├── requirements.txt
│   ├── config/
│   │   └── config.yaml
│   ├── data/
│   │   └── blueprints/
│   │       └── example_venue.json
│   ├── models/
│   ├── scripts/
│   └── src/
├── frontend/
├── README.md
└── .gitignore
```

## Backend Setup

### 1. Create a virtual environment

From the repository root:

```bash
python -m venv backend/venv
```

### 2. Activate the environment

Windows Command Prompt:

```cmd
backend\venv\Scripts\activate
```

macOS / Linux:

```bash
source backend/venv/bin/activate
```

### 3. Install dependencies

From the repository root:

```bash
pip install -r backend/requirements.txt
```

## Running the API Server

From the repository root, run:

```bash
cd backend
uvicorn api_server:app --host 0.0.0.0 --port 8000
```

The server may take approximately 10–20 seconds to start while the YOLO and CSRNet models load.

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/health` | Server status |
| GET | `/api/zones` | Per-zone risk data for the heatmap |
| GET | `/api/cameras` | Camera status; currently a single simulated camera |
| GET | `/api/alerts` | Alert history, including alerts generated when a zone becomes CRITICAL |
| GET | `/api/evacuation/routes` | Available exits and estimated arrival times; optionally accepts `zone_row` and `zone_col` |
| GET | `/api/risk-prediction` | Aggregate risk score and recommendations |

## Switching the Video Source

Send a POST request to `/api/set-video-source` with an absolute video-file path:

```bash
curl -X POST http://localhost:8000/api/set-video-source \
  -H "Content-Type: application/json" \
  -d '{"path": "/absolute/path/to/video.mp4"}'
```

Replace the example path with the location of your video file.

## Android Frontend

The Android application is located in the `frontend/` directory.

1. Open the `frontend/` folder in Android Studio.
2. Allow Gradle synchronization to complete.
3. Configure the backend URL in the application as required.
4. Run the app on an emulator or Android device.

When testing on a physical phone, use your computer's reachable local network IP address instead of `localhost` for backend requests.

## Known Limitations

- `ground_area_sqm` in `backend/config/config.yaml` is a placeholder. Real deployment requires site-specific measurements for accurate density-per-square-metre calibration.
- Evacuation routing uses a synthetic example blueprint in `backend/data/blueprints/example_venue.json`; it is not yet connected to a real venue.
- `/api/cameras` returns a single simulated camera. True multi-camera support is a planned extension.
- The density and head-detection models are described as trained on ShanghaiTech and SCUT-HEAD datasets, respectively. Accuracy under unseen camera angles and lighting conditions requires further real-world validation.


## Repository

GitHub: https://github.com/Samiksha104/CrowdShield
