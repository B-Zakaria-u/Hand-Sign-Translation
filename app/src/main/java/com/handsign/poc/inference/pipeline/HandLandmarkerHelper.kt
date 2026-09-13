package com.handsign.poc.inference.pipeline

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.handsign.poc.inference.NormalizedLandmark

/**
 * Wraps MediaPipe HandLandmarker in LIVE_STREAM mode.
 * Call [detect] from the CameraX image analysis callback;
 * results are delivered asynchronously to [onResult].
 */
class HandLandmarkerHelper(
    private val context: Context,
    private val onResult: (HandLandmarkerResult) -> Unit,
    private val onError: (Exception) -> Unit
) {
    companion object {
        private const val HAND_LANDMARKER_ASSET = "hand_landmarker.task"
        private const val NUM_HANDS = 1
        private const val MIN_HAND_DETECTION_CONFIDENCE = 0.5f
        private const val MIN_HAND_TRACKING_CONFIDENCE  = 0.5f
        private const val MIN_HAND_PRESENCE_CONFIDENCE  = 0.5f
    }

    private var landmarker: HandLandmarker? = null

    fun setup() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(HAND_LANDMARKER_ASSET)
                .build()
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setNumHands(NUM_HANDS)
                .setMinHandDetectionConfidence(MIN_HAND_DETECTION_CONFIDENCE)
                .setMinTrackingConfidence(MIN_HAND_TRACKING_CONFIDENCE)
                .setMinHandPresenceConfidence(MIN_HAND_PRESENCE_CONFIDENCE)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, _ -> onResult(result) }
                .setErrorListener { err -> onError(err) }
                .build()
            landmarker = HandLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            onError(e)
        }
    }

    @Volatile private var isProcessing = false

    fun detectAsync(imageProxy: ImageProxy) {
        val lm = landmarker
        if (lm == null || isProcessing) {
            imageProxy.close()
            return
        }
        isProcessing = true
        try {
            val bitmap = imageProxy.toBitmap()
            val mpImage = BitmapImageBuilder(bitmap).build()
            
            // imageProxy.toBitmap() returns an upright bitmap, so we don't pass rotationDegrees to MediaPipe
            lm.detectAsync(mpImage, SystemClock.uptimeMillis())
        } catch (e: Exception) {
            onError(e)
        } finally {
            imageProxy.close()
            isProcessing = false
        }
    }

    fun close() {
        landmarker?.close()
        landmarker = null
    }
}

/** Converts MediaPipe HandLandmarkerResult to our domain NormalizedLandmark list */
fun HandLandmarkerResult.toNormalizedLandmarks(): List<NormalizedLandmark> =
    landmarks().firstOrNull()?.map {
        NormalizedLandmark(it.x(), it.y(), it.z())
    } ?: emptyList()

/** Flattens 21 landmarks to a float[63] array for TFLite/ONNX input */
fun List<NormalizedLandmark>.toFloatArray63(): FloatArray {
    val arr = FloatArray(63)
    forEachIndexed { i, lm ->
        arr[i * 3]     = lm.x
        arr[i * 3 + 1] = lm.y
        arr[i * 3 + 2] = lm.z
    }
    return arr
}
