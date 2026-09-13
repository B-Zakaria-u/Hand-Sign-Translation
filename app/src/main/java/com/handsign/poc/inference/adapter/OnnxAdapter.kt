package com.handsign.poc.inference.adapter

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.handsign.poc.data.model.ModelConfig
import com.handsign.poc.inference.GestureRecognizer
import com.handsign.poc.inference.NormalizedLandmark
import com.handsign.poc.inference.RecognitionResult
import com.handsign.poc.inference.pipeline.HandLandmarkerHelper
import com.handsign.poc.inference.pipeline.toFloatArray63
import com.handsign.poc.inference.pipeline.toNormalizedLandmarks
import java.io.File
import java.nio.FloatBuffer

/**
 * Adapter for ONNX models via ONNX Runtime for Android.
 * Uses the landmark pipeline (MediaPipe Hands → float[63]) for input.
 */
class OnnxAdapter(
    private val context: Context,
    private val config: ModelConfig,
    private val modelFile: File
) : GestureRecognizer {

    private lateinit var env: OrtEnvironment
    private lateinit var session: OrtSession
    private lateinit var handLandmarkerHelper: HandLandmarkerHelper
    @Volatile private var lastResult: HandLandmarkerResult? = null

    override fun initialize() {
        env     = OrtEnvironment.getEnvironment()
        session = env.createSession(modelFile.absolutePath, OrtSession.SessionOptions())
        handLandmarkerHelper = HandLandmarkerHelper(
            context  = context,
            onResult = { result -> lastResult = result },
            onError  = { }
        )
        handLandmarkerHelper.setup()
    }

    override fun recognize(imageProxy: ImageProxy): RecognitionResult? {
        var imgWidth = imageProxy.width
        var imgHeight = imageProxy.height
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        if (rotationDegrees % 180 != 0) {
            imgWidth = imageProxy.height
            imgHeight = imageProxy.width
        }

        handLandmarkerHelper.detectAsync(imageProxy)
        val result    = lastResult ?: return null
        val landmarks = result.toNormalizedLandmarks()
        if (landmarks.size < 21) return null

        val floatArr  = landmarks.toFloatArray63()
        val shape     = longArrayOf(1, 63)
        val tensor    = OnnxTensor.createTensor(env, FloatBuffer.wrap(floatArr), shape)

        val t0 = SystemClock.elapsedRealtime()
        val outputs = session.run(mapOf(session.inputNames.first() to tensor))
        val inferenceMs = SystemClock.elapsedRealtime() - t0

        tensor.close()

        val scores = (outputs.first().value as Array<FloatArray>)[0]
        val maxIdx = scores.indices.maxByOrNull { scores[it] } ?: return null
        val confidence = scores[maxIdx]
        if (confidence < config.confidenceThreshold) return null

        return RecognitionResult(
            letter      = config.labels[maxIdx],
            confidence  = confidence,
            landmarks   = emptyList(),
            imageWidth  = imgWidth,
            imageHeight = imgHeight,
            inferenceMs = inferenceMs
        )
    }

    override fun labels() = config.labels

    override fun close() {
        handLandmarkerHelper.close()
        if (::session.isInitialized) session.close()
        if (::env.isInitialized) env.close()
    }
}
