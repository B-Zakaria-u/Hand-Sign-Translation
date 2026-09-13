package com.handsign.poc.inference.adapter

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import com.handsign.poc.data.model.ModelConfig
import com.handsign.poc.inference.GestureRecognizer
import com.handsign.poc.inference.NormalizedLandmark
import com.handsign.poc.inference.RecognitionResult

/**
 * Adapter for MediaPipe GestureRecognizer .task bundles.
 * Uses the MediaPipe GestureRecognizer API directly — no custom classifier needed.
 * Pipeline: CameraX ImageProxy → MediaPipe GestureRecognizer → category + landmarks
 */
class MediaPipeTaskAdapter(
    private val context: Context,
    private val config: ModelConfig,
    private val taskFile: java.io.File
) : GestureRecognizer {

    private lateinit var gestureRecognizer: com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
    @Volatile private var lastResult: GestureRecognizerResult? = null

    override fun initialize() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(taskFile.absolutePath)
            .build()
        val options = com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
            .GestureRecognizerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ -> lastResult = result }
            .build()
        gestureRecognizer = com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
            .createFromOptions(context, options)
    }

    override fun recognize(imageProxy: ImageProxy): RecognitionResult? {
        try {
            val bitmap = imageProxy.toBitmap()
            val mpImage = BitmapImageBuilder(bitmap).build()
            gestureRecognizer.recognizeAsync(mpImage, SystemClock.uptimeMillis())
        } finally {
            imageProxy.close()
        }

        val result = lastResult ?: return null
        val gesture = result.gestures().firstOrNull()?.firstOrNull() ?: return null
        val confidence = gesture.score()

        if (confidence < config.confidenceThreshold) return null

        val landmarks = result.landmarks().firstOrNull()?.map {
            NormalizedLandmark(it.x(), it.y(), it.z())
        }

        return RecognitionResult(
            letter      = gesture.categoryName(),
            confidence  = confidence,
            landmarks   = landmarks,
            inferenceMs = 0L  // MediaPipe doesn't expose per-frame timing
        )
    }

    override fun labels() = config.labels

    override fun close() {
        if (::gestureRecognizer.isInitialized) gestureRecognizer.close()
    }
}
