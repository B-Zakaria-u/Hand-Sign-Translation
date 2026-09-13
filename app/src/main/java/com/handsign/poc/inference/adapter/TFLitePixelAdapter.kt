package com.handsign.poc.inference.adapter

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.handsign.poc.data.model.ModelConfig
import com.handsign.poc.inference.GestureRecognizer
import com.handsign.poc.inference.RecognitionResult
import com.handsign.poc.inference.pipeline.ImagePreprocessor
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileChannel

/**
 * Adapter for TFLite models that take a raw image tensor (H×W×C float) as input.
 * Suitable for CNN-based models (MobileNet, EfficientNet, etc.).
 * Pipeline: CameraX ImageProxy → Bitmap → resize+normalize → TFLite → letter
 */
class TFLitePixelAdapter(
    private val context: Context,
    private val config: ModelConfig,
    private val modelFile: File
) : GestureRecognizer {

    private lateinit var interpreter: Interpreter
    private lateinit var preprocessor: ImagePreprocessor

    override fun initialize() {
        val opts = Interpreter.Options().apply {
            try { addDelegate(GpuDelegate()) } catch (_: Exception) {}
            numThreads = 4
        }
        val buffer = FileInputStream(modelFile).channel.map(
            FileChannel.MapMode.READ_ONLY, 0, modelFile.length()
        )
        interpreter = Interpreter(buffer, opts)
        preprocessor = ImagePreprocessor(
            width     = config.inputWidth,
            height    = config.inputHeight,
            normalize = config.normalizeRange
        )
    }

    override fun recognize(imageProxy: ImageProxy): RecognitionResult? {
        val bitmap = try {
            imageProxy.toBitmap()
        } finally {
            imageProxy.close()
        }

        val inputBuffer = preprocessor.process(bitmap)
        val output = Array(1) { FloatArray(config.labels.size) }

        val t0 = SystemClock.elapsedRealtime()
        interpreter.run(inputBuffer, output)
        val inferenceMs = SystemClock.elapsedRealtime() - t0

        val scores = output[0]
        val maxIdx = scores.indices.maxByOrNull { scores[it] } ?: return null
        val confidence = scores[maxIdx]

        if (confidence < config.confidenceThreshold) return null

        return RecognitionResult(
            letter      = config.labels[maxIdx],
            confidence  = confidence,
            landmarks   = null,      // pixel-based: no landmarks
            inferenceMs = inferenceMs
        )
    }

    override fun labels() = config.labels

    override fun close() {
        if (::interpreter.isInitialized) interpreter.close()
    }
}
