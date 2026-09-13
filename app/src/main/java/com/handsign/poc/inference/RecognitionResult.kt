package com.handsign.poc.inference

/**
 * Represents a single inference output from a [GestureRecognizer].
 */
data class RecognitionResult(
    /** The predicted letter label (e.g. "A", "SPACE") */
    val letter: String,
    /** Confidence score in the range [0.0, 1.0] */
    val confidence: Float,
    /** Normalized hand landmarks from MediaPipe (null for pixel-based models) */
    val landmarks: List<NormalizedLandmark>?,
    /** Wall-clock time taken to run the inference, in milliseconds */
    val inferenceMs: Long
)

/**
 * A single MediaPipe hand landmark in normalized image coordinates.
 * x and y are in [0, 1] relative to the image dimensions.
 * z represents depth relative to the wrist.
 */
data class NormalizedLandmark(
    val x: Float,
    val y: Float,
    val z: Float
)
