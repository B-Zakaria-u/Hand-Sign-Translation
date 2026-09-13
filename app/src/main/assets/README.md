# HandSign POC — Model Assets

## Bundled model placeholder

`model.tflite` in this directory is a **placeholder stub** (4 bytes).
You must replace it with your actual 27-letter TFLite model before running the app.

## How to add your model

### Option A — Replace bundled model (rebuild required)
1. Copy your `model.tflite` (or `.task`/`.onnx`/`.ptl`) here, overwriting the placeholder
2. Edit `model_config.json` to match your model's:
   - `format`: `"TFLITE_LANDMARK"` | `"TFLITE_PIXEL"` | `"MEDIAPIPE_TASK"` | `"ONNX"` | `"TORCH_MOBILE"`
   - `labels`: your class labels array
   - `confidenceThreshold`: recommended 0.70–0.85
3. Run `./gradlew assembleDebug`

### Option B — Import at runtime (no rebuild)
1. Build and install the app as-is
2. Open **Model** tab → **Import File** → select your model file

### Option C — GitHub URL (no rebuild, no cable)
1. Push your model to a GitHub repo
2. Open **Model** tab → **GitHub URL**
3. Enter your repo URL + branch + file path

## MediaPipe hand_landmarker.task

This file is required for TFLITE_LANDMARK mode (the default).
Download it before the first build:

```powershell
curl.exe -L https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task -o app\src\main\assets\hand_landmarker.task
```

File size: ~8.3 MB. It is bundled with the APK.
