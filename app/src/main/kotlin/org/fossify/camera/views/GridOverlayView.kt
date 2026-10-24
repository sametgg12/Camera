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
        val w = width.toFloat()
        val h = height.toFloat()
        for (i in 1 until divisions) {
            val x = w * i / divisions
            val y = h * i / divisions
            canvas.drawLine(x, 0f, x, h, shadowPaint)
            canvas.drawLine(0f, y, w, y, shadowPaint)
            canvas.drawLine(x, 0f, x, h, linePaint)
            canvas.drawLine(0f, y, w, y, linePaint)
        }
    }
}
