package com.handsign.poc.inference

import android.content.Context
import com.handsign.poc.data.model.ModelConfig
import com.handsign.poc.data.model.ModelFormat
import com.handsign.poc.inference.adapter.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory that creates the correct [GestureRecognizer] adapter
 * based on the [ModelConfig.format] field.
 * This is the key extension point — adding a new model format requires
 * only a new adapter class and one `when` branch here.
 */
@Singleton
class ModelAdapterFactory @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun create(config: ModelConfig, modelFile: File): GestureRecognizer =
        when (config.format) {
            ModelFormat.TFLITE_LANDMARK  -> TFLiteLandmarkAdapter(context, config, modelFile)
            ModelFormat.TFLITE_PIXEL     -> TFLitePixelAdapter(context, config, modelFile)
            ModelFormat.MEDIAPIPE_TASK   -> MediaPipeTaskAdapter(context, config, modelFile)
            ModelFormat.ONNX             -> OnnxAdapter(context, config, modelFile)
            ModelFormat.TORCH_MOBILE     -> TorchMobileAdapter(context, config, modelFile)
        }
}
