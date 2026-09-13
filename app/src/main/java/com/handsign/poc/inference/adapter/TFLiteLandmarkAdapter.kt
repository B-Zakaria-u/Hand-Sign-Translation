package com.handsign.poc.inference.adapter

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.handsign.poc.data.model.ModelConfig
import com.handsign.poc.inference.GestureRecognizer
import com.handsign.poc.inference.NormalizedLandmark
import com.handsign.poc.inference.RecognitionResult
import com.handsign.poc.inference.pipeline.HandLandmarkerHelper
import com.handsign.poc.inference.pipeline.toFloatArray63
import com.handsign.poc.inference.pipeline.toNormalizedLandmarks
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileChannel

/**
 * Adapter for TFLite models that take a 63-float landmark array as input.
 * Pipeline: CameraX ImageProxy → MediaPipe HandLandmarker → float[63] → TFLite → letter
 */
class TFLiteLandmarkAdapter(
    private val context: Context,
    private val config: ModelConfig,
    private val modelFile: File
) : GestureRecognizer {

    private lateinit var interpreter: Interpreter
    private lateinit var handLandmarkerHelper: HandLandmarkerHelper

    @Volatile private var lastResult: HandLandmarkerResult? = null

    override fun initialize() {
        // ── 1. TFLite Interpreter (GPU → CPU fallback) ─────────────────
        val opts = Interpreter.Options().apply {
            try {
                addDelegate(GpuDelegate())
            } catch (e: Exception) {
                // GPU not available — CPU fallback
            }
            numThreads = 4
        }
        val buffer = FileInputStream(modelFile).channel.map(
            FileChannel.MapMode.READ_ONLY, 0, modelFile.length()
        )
        interpreter = Interpreter(buffer, opts)

        // ── 2. MediaPipe HandLandmarker ────────────────────────────────
        handLandmarkerHelper = HandLandmarkerHelper(
            context  = context,
            onResult = { result -> lastResult = result },
            onError  = { /* silently ignore per-frame errors */ }
        )
        handLandmarkerHelper.setup()
    }

    override fun recognize(imageProxy: ImageProxy): RecognitionResult? {
        // Send frame to MediaPipe asynchronously, then close the proxy
        handLandmarkerHelper.detectAsync(imageProxy)

        // Use the most recently delivered landmark result
        val result = lastResult ?: return null
        val landmarks = result.toNormalizedLandmarks()
        if (landmarks.size < 21) return null

        // Build float[63] tensor
        val input = landmarks.toFloatArray63()

        // Run TFLite
        val output = Array(1) { FloatArray(config.labels.size) }
        val t0 = SystemClock.elapsedRealtime()
        interpreter.run(arrayOf(input), mapOf(0 to output))
        val inferenceMs = SystemClock.elapsedRealtime() - t0

        val scores = output[0]
        val maxIdx = scores.indices.maxByOrNull { scores[it] } ?: return null
        val confidence = scores[maxIdx]

        if (confidence < config.confidenceThreshold) return null

        return RecognitionResult(
            letter      = config.labels[maxIdx],
            confidence  = confidence,
            landmarks   = landmarks,
            inferenceMs = inferenceMs
        )
    }

    override fun labels() = config.labels

    override fun close() {
        handLandmarkerHelper.close()
        if (::interpreter.isInitialized) interpreter.close()
    }
}
