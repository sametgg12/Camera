package org.fossify.camera.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Thin rule-of-thirds style grid drawn over the preview. It is only shown on screen,
 * it is never burned into the captured photo or video.
 */
class GridOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var divisions = 3
        set(value) {
            field = value.coerceAtLeast(2)
            invalidate()
        }

    /**
     * Width / height of the camera image shown inside this view (for example 0.75 for 4:3 in portrait).
     * 0 means the image fills the whole view.
     */
    var contentAspect = 0f
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x55000000
        style = Paint.Style.STROKE
        strokeWidth = density * 2f
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xAAFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = density
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        var left = 0f
        var top = 0f
        var w = width.toFloat()
        var h = height.toFloat()

        if (contentAspect > 0f && w > 0f && h > 0f) {
            val viewAspect = w / h
            if (viewAspect > contentAspect) {
                val contentWidth = h * contentAspect
                left = (w - contentWidth) / 2f
                w = contentWidth
            } else {
                val contentHeight = w / contentAspect
                top = (h - contentHeight) / 2f
                h = contentHeight
            }
        }

        for (i in 1 until divisions) {
            val x = left + w * i / divisions
            val y = top + h * i / divisions
            canvas.drawLine(x, top, x, top + h, shadowPaint)
            canvas.drawLine(left, y, left + w, y, shadowPaint)
            canvas.drawLine(x, top, x, top + h, linePaint)
            canvas.drawLine(left, y, left + w, y, linePaint)
        }
    }
}
