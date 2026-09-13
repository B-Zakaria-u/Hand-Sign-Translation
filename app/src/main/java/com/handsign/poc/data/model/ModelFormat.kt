package com.handsign.poc.data.model

/** Determines which GestureRecognizer adapter is instantiated for a model */
enum class ModelFormat {
    /** TFLite model taking 63-float landmark array (21 keypoints × 3) as input */
    TFLITE_LANDMARK,
    /** TFLite model taking raw image crop (H×W×C pixel tensor) as input */
    TFLITE_PIXEL,
    /** MediaPipe .task bundle using GestureRecognizer API */
    MEDIAPIPE_TASK,
    /** ONNX model via ONNX Runtime for Android */
    ONNX,
    /** PyTorch Mobile lite model (.ptl) */
    TORCH_MOBILE,
    /** Keras model (.keras or .h5) converted to TFLite */
    KERAS
}
