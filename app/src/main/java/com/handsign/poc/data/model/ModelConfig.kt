package com.handsign.poc.data.model

/**
 * Parsed representation of the model_config.json sidecar file that
 * accompanies every model file. This is what drives the adapter selection
 * and pre/post-processing parameters.
 */
data class ModelConfig(
    /** Human-readable model name */
    val name: String,
    /** Which adapter to use for inference */
    val format: ModelFormat,
    /** Output class labels, e.g. ["A","B",...,"Z","SPACE"] */
    val labels: List<String>,
    /** Input image width (only used for pixel-based models) */
    val inputWidth: Int = 224,
    /** Input image height (only used for pixel-based models) */
    val inputHeight: Int = 224,
    /** Number of image channels (3 = RGB, 1 = grayscale) */
    val inputChannels: Int = 3,
    /** Normalization range: "0_1" (÷255) or "-1_1" (MobileNet style) */
    val normalizeRange: String = "0_1",
    /** Minimum confidence to emit a recognition event */
    val confidenceThreshold: Float = 0.75f,
    /** Number of consecutive frames needed to "commit" a letter */
    val stableFrames: Int = 10,
    /** Semantic version of this model config */
    val version: String = "1.0.0",
    /** Expected SHA-256 hex digest of the model file, null = skip check */
    val sha256: String? = null
) {
    companion object {
        /** Default config for the bundled ASL 27-letter model */
        fun bundledDefault() = ModelConfig(
            name   = "ASL 27-Letter (Bundled)",
            format = ModelFormat.TFLITE_LANDMARK,
            labels = ('A'..'Z').map { it.toString() } + listOf("SPACE")
        )
    }
}
