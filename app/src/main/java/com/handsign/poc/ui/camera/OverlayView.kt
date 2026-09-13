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

    /** The 20 MediaPipe hand connections (pairs of landmark indices) */
    private val CONNECTIONS = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 4,        // Thumb
        0 to 5, 5 to 6, 6 to 7, 7 to 8,        // Index
        0 to 9, 9 to 10, 10 to 11, 11 to 12,    // Middle
        0 to 13, 13 to 14, 14 to 15, 15 to 16,  // Ring
        0 to 17, 17 to 18, 18 to 19, 19 to 20   // Pinky
    )

    private val bonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = 0xFF00E5FF.toInt()  // Cyan
        strokeWidth = 6f
        style       = Paint.Style.STROKE
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()        // White dots
        style = Paint.Style.FILL
    }
    private val wristPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00E676.toInt()        // Green wrist
        style = Paint.Style.FILL
    }

    private var landmarks: List<NormalizedLandmark> = emptyList()
    private var imageWidth  = 1
    private var imageHeight = 1
    private var isFrontCamera = true

    fun setResults(
        landmarks: List<NormalizedLandmark>,
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
        if (landmarks.size < 21) return

        val scaleX = width.toFloat()  / imageWidth
        val scaleY = height.toFloat() / imageHeight

        fun lmToScreen(lm: NormalizedLandmark): PointF {
            val sx = lm.x * imageWidth * scaleX
            // Mirror X for front camera
            val screenX = if (isFrontCamera) width - sx else sx
            val screenY = lm.y * imageHeight * scaleY
            return PointF(screenX, screenY)
        }

        // Draw connections
        CONNECTIONS.forEach { (a, b) ->
            val pa = lmToScreen(landmarks[a])
            val pb = lmToScreen(landmarks[b])
            canvas.drawLine(pa.x, pa.y, pb.x, pb.y, bonePaint)
        }

        // Draw dots
        landmarks.forEachIndexed { i, lm ->
            val p = lmToScreen(lm)
            val paint = if (i == 0) wristPaint else dotPaint
            canvas.drawCircle(p.x, p.y, if (i == 0) 14f else 9f, paint)
        }
    }
}
