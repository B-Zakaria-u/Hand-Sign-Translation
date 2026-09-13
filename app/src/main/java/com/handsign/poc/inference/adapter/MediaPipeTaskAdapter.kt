package com.handsign.poc.inference.adapter

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
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
        val assetPath = if (taskFile.name.contains("gesture_recognizer")) "gesture_recognizer.task" else taskFile.name
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(assetPath)
            .build()
        val options = com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
            .GestureRecognizerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumHands(2)
            .setResultListener { result, _ -> lastResult = result }
            .build()
        gestureRecognizer = com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
            .createFromOptions(context, options)
    }

    @Volatile private var isProcessing = false

    override fun recognize(imageProxy: ImageProxy): RecognitionResult? {
        if (isProcessing) {
            imageProxy.close()
            return null
        }
        isProcessing = true

        var imgWidth = imageProxy.width
        var imgHeight = imageProxy.height

        try {
            val bitmap = imageProxy.toBitmap()
            imgWidth = bitmap.width
            imgHeight = bitmap.height
            
            val mpImage = BitmapImageBuilder(bitmap).build()
            
            // imageProxy.toBitmap() returns an upright bitmap, so we don't pass rotationDegrees to MediaPipe
            gestureRecognizer.recognizeAsync(mpImage, SystemClock.uptimeMillis())
        } catch (e: Exception) {
            android.util.Log.e("MediaPipeTaskAdapter", "Error in recognizeAsync", e)
        } finally {
            imageProxy.close()
            isProcessing = false
        }

        val result = lastResult ?: return null
        val handsLandmarks = result.landmarks()?.map { lmList ->
            lmList.map { NormalizedLandmark(it.x(), it.y(), it.z()) }
        } ?: emptyList()

        if (handsLandmarks.isEmpty()) return null

        var bestGestureName = "—"
        var bestConfidence = 0f

        result.gestures()?.forEach { gestureList ->
            val gesture = gestureList.firstOrNull()
            if (gesture != null) {
                val name = gesture.categoryName()
                val score = gesture.score()
                if (score > bestConfidence && name != "None") {
                    bestConfidence = score
                    bestGestureName = name
                }
            }
        }

        val (finalLetter, finalConfidence) = if (bestConfidence >= config.confidenceThreshold) {
            bestGestureName to bestConfidence
        } else {
            "—" to bestConfidence
        }

        return RecognitionResult(
            letter      = finalLetter,
            confidence  = finalConfidence,
            landmarks   = handsLandmarks,
            imageWidth  = imgWidth,
            imageHeight = imgHeight,
            inferenceMs = 0L  // MediaPipe doesn't expose per-frame timing
        )
    }

    override fun labels() = config.labels

    override fun close() {
        if (::gestureRecognizer.isInitialized) gestureRecognizer.close()
    }
}
