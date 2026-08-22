package com.example.applock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat

/**
 * Minimal, dependency-free 3x3 pattern lock view.
 * Reports the finished pattern as a String of dot indices, e.g. "0,1,4,7".
 */
class PatternLockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onPatternComplete: ((String) -> Unit)? = null

    private val dotCount = 9
    private val dotPositions = FloatArray(dotCount * 2)
    private val selected = mutableListOf<Int>()
    private var currentX = 0f
    private var currentY = 0f
    private var tracking = false

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        style = Paint.Style.FILL
    }
    private val selectedDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.accent)
        style = Paint.Style.FILL
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.accent)
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val cellW = w / 3f
        val cellH = h / 3f
        for (row in 0..2) {
            for (col in 0..2) {
                val index = row * 3 + col
                dotPositions[index * 2] = cellW * col + cellW / 2
                dotPositions[index * 2 + 1] = cellH * row + cellH / 2
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = width.coerceAtMost(height) / 18f

        for (i in 0 until selected.size - 1) {
            val a = selected[i]
            val b = selected[i + 1]
            canvas.drawLine(
                dotPositions[a * 2], dotPositions[a * 2 + 1],
                dotPositions[b * 2], dotPositions[b * 2 + 1],
                linePaint
            )
        }
        if (tracking && selected.isNotEmpty()) {
            val last = selected.last()
            canvas.drawLine(
                dotPositions[last * 2], dotPositions[last * 2 + 1],
                currentX, currentY, linePaint
            )
        }

        for (i in 0 until dotCount) {
            val paint = if (selected.contains(i)) selectedDotPaint else dotPaint
            canvas.drawCircle(dotPositions[i * 2], dotPositions[i * 2 + 1], radius, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selected.clear()
                tracking = true
                handleTouch(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                currentX = event.x
                currentY = event.y
                handleTouch(event.x, event.y)
            }
            MotionEvent.ACTION_UP -> {
                tracking = false
                onPatternComplete?.invoke(selected.joinToString(","))
                postDelayed({ selected.clear(); invalidate() }, 300)
            }
        }
        invalidate()
        return true
    }

    private fun handleTouch(x: Float, y: Float) {
        currentX = x
        currentY = y
        val radius = width.coerceAtMost(height) / 9f
        for (i in 0 until dotCount) {
            if (selected.contains(i)) continue
            val dx = x - dotPositions[i * 2]
            val dy = y - dotPositions[i * 2 + 1]
            if (dx * dx + dy * dy < radius * radius) {
                selected.add(i)
            }
        }
    }

    fun reset() {
        selected.clear()
        invalidate()
    }
}
