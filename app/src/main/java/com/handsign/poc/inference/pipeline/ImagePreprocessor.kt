package com.handsign.poc.inference.pipeline

import android.graphics.Bitmap
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.DataType
import java.nio.ByteBuffer

/**
 * Pre-processes a camera frame [Bitmap] into a normalized [ByteBuffer]
 * suitable for pixel-based TFLite models.
 *
 * @param width  Target width  (from ModelConfig.inputWidth)
 * @param height Target height (from ModelConfig.inputHeight)
 * @param normalize "-1_1" for MobileNet-style normalization, "0_1" otherwise
 */
class ImagePreprocessor(
    private val width: Int,
    private val height: Int,
    private val normalize: String = "0_1"
) {
    private val processor: ImageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(height, width, ResizeOp.ResizeMethod.BILINEAR))
        .add(
            if (normalize == "-1_1") NormalizeOp(127.5f, 127.5f)
            else NormalizeOp(0f, 255f)
        )
        .build()

    fun process(bitmap: Bitmap): ByteBuffer {
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        return processor.process(tensorImage).buffer
    }
}
