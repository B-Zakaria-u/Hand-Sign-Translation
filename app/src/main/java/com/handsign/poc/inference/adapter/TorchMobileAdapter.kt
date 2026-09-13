package com.handsign.poc.inference.adapter

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.handsign.poc.data.model.ModelConfig
import com.handsign.poc.inference.GestureRecognizer
import com.handsign.poc.inference.RecognitionResult
import com.handsign.poc.inference.pipeline.HandLandmarkerHelper
import com.handsign.poc.inference.pipeline.toFloatArray63
import com.handsign.poc.inference.pipeline.toNormalizedLandmarks
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File

/**
 * Adapter for PyTorch Mobile Lite (.ptl) models.
 * Uses the landmark pipeline (MediaPipe Hands → float[63]) for input.
 */
class TorchMobileAdapter(
    private val context: Context,
    private val config: ModelConfig,
    private val modelFile: File
) : GestureRecognizer {

    private lateinit var module: Module
    private lateinit var handLandmarkerHelper: HandLandmarkerHelper
    @Volatile private var lastResult: HandLandmarkerResult? = null

    override fun initialize() {
        module = LiteModuleLoader.load(modelFile.absolutePath)
        handLandmarkerHelper = HandLandmarkerHelper(
            context  = context,
            onResult = { result -> lastResult = result },
            onError  = { }
        )
        handLandmarkerHelper.setup()
    }

    override fun recognize(imageProxy: ImageProxy): RecognitionResult? {
        handLandmarkerHelper.detectAsync(imageProxy)
        val result    = lastResult ?: return null
        val landmarks = result.toNormalizedLandmarks()
        if (landmarks.size < 21) return null

        val floatArr = landmarks.toFloatArray63()
        val tensor   = Tensor.fromBlob(floatArr, longArrayOf(1, 63))

        val t0 = SystemClock.elapsedRealtime()
        val output = module.forward(IValue.from(tensor)).toTensor()
        val inferenceMs = SystemClock.elapsedRealtime() - t0

        val scores = output.dataAsFloatArray
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
        if (::module.isInitialized) module.destroy()
    }
}
