package com.handsign.poc.inference

import androidx.camera.core.ImageProxy

/**
 * Core abstraction for hand gesture recognition.
 * Each model format (TFLite landmark, TFLite pixel, MediaPipe task, ONNX,
 * PyTorch Mobile) implements this interface so they can be swapped at runtime.
 */
interface GestureRecognizer {
    /**
     * Called once after construction. Loads model weights into memory,
     * initializes delegates (GPU/NNAPI/CPU), and prepares for inference.
     */
    fun initialize()

    /**
     * Runs inference on a single camera frame.
     * The implementation is responsible for closing [imageProxy].
     * Returns null if no hand is detected in the frame.
     */
    fun recognize(imageProxy: ImageProxy): RecognitionResult?

    /** Returns the ordered label list this model outputs (e.g. ["A","B",...,"SPACE"]) */
    fun labels(): List<String>

    /** Releases all native resources. Must be called when the ViewModel is cleared. */
    fun close()
}
