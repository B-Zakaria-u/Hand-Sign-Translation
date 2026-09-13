package com.handsign.poc.ui.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import com.handsign.poc.inference.NormalizedLandmark

/**
 * A transparent overlay [View] drawn on top of the camera [PreviewView].
 * Renders the 21 MediaPipe hand landmarks as dots and the 20 bone connections
 * as lines, mirrored for front-camera use.
 */
class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    init {
        // BlurMaskFilter requires software rendering on many Android versions/devices
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    /** The 20 MediaPipe hand connections (pairs of landmark indices) */
    private val CONNECTIONS = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 4,        // Thumb
        0 to 5, 5 to 6, 6 to 7, 7 to 8,        // Index
        0 to 9, 9 to 10, 10 to 11, 11 to 12,    // Middle
        0 to 13, 13 to 14, 14 to 15, 15 to 16,  // Ring
        0 to 17, 17 to 18, 18 to 19, 19 to 20   // Pinky
    )

    private val boneGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x9939FF14.toInt() // Translucent neon green
        strokeWidth = 16f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        maskFilter = android.graphics.BlurMaskFilter(15f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }

    private val bonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF39FF14.toInt() // Solid neon green
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val dotGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x9939FF14.toInt() // Translucent neon green
        style = Paint.Style.FILL
        maskFilter = android.graphics.BlurMaskFilter(15f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt() // Solid white core
        style = Paint.Style.FILL
    }

    private var landmarks: List<List<NormalizedLandmark>> = emptyList()
    private var imageWidth  = 1
    private var imageHeight = 1
    private var isFrontCamera = true

    fun setResults(
        landmarks: List<List<NormalizedLandmark>>,
        imageWidth: Int,
        imageHeight: Int,
        isFrontCamera: Boolean = true
    ) {
        this.landmarks    = landmarks
        this.imageWidth   = imageWidth
        this.imageHeight  = imageHeight
        this.isFrontCamera = isFrontCamera
        invalidate()
    }

    fun clear() {
        landmarks = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (landmarks.isEmpty()) return

        val imageAspectRatio = imageWidth.toFloat() / imageHeight.toFloat()
        val viewAspectRatio = width.toFloat() / height.toFloat()

        var scale = 1f
        var dx = 0f
        var dy = 0f

        if (imageAspectRatio > viewAspectRatio) {
            scale = height.toFloat() / imageHeight
            val scaledWidth = imageWidth * scale
            dx = -(scaledWidth - width) / 2f
        } else {
            scale = width.toFloat() / imageWidth
            val scaledHeight = imageHeight * scale
            dy = -(scaledHeight - height) / 2f
        }

        fun lmToScreen(lm: NormalizedLandmark): PointF {
            val sx = lm.x * imageWidth * scale + dx
            // Mirror X for front camera
            val screenX = if (isFrontCamera) width - sx else sx
            val screenY = lm.y * imageHeight * scale + dy
            return PointF(screenX, screenY)
        }

        landmarks.forEach { handLandmarks ->
            if (handLandmarks.size < 21) return@forEach

            // Draw connections (bones)
            CONNECTIONS.forEach { (a, b) ->
                val pa = lmToScreen(handLandmarks[a])
                val pb = lmToScreen(handLandmarks[b])
                // Draw glow first
                canvas.drawLine(pa.x, pa.y, pb.x, pb.y, boneGlowPaint)
                // Draw solid core
                canvas.drawLine(pa.x, pa.y, pb.x, pb.y, bonePaint)
            }

            // Draw joints (dots)
            handLandmarks.forEach { lm ->
                val p = lmToScreen(lm)
                // Draw glow first
                canvas.drawCircle(p.x, p.y, 18f, dotGlowPaint)
                // Draw solid white core
                canvas.drawCircle(p.x, p.y, 8f, dotPaint)
            }
        }
    }
}
