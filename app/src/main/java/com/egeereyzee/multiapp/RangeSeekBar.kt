package com.egeereyzee.multiapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class RangeSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var minValue: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var maxValue: Int = 100
        set(value) {
            field = value
            invalidate()
        }

    var leftThumbValue: Int = 0
        set(value) {
            field = value.coerceIn(minValue, rightThumbValue)
            onRangeChangeListener?.onRangeChanged(field, rightThumbValue)
            invalidate()
        }

    var rightThumbValue: Int = 100
        set(value) {
            field = value.coerceIn(leftThumbValue, maxValue)
            onRangeChangeListener?.onRangeChanged(leftThumbValue, field)
            invalidate()
        }

    private var trackColor: Int = Color.GRAY
    private var trackProgressColor: Int = Color.BLUE
    private var thumbColor: Int = Color.WHITE
    private var thumbBorderColor: Int = Color.BLUE

    private var trackHeight: Float = dpToPx(4f)
    private var thumbRadius: Float = dpToPx(12f)
    private var thumbBorderWidth: Float = dpToPx(2f)

    private var leftThumbPosition: Float = 0f
    private var rightThumbPosition: Float = 0f

    private var isLeftThumbDragging: Boolean = false
    private var isRightThumbDragging: Boolean = false

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    interface OnRangeChangeListener {
        fun onRangeChanged(leftValue: Int, rightValue: Int)
    }

    var onRangeChangeListener: OnRangeChangeListener? = null

    init {
        setupAttributes(attrs)
        setupPaints()
    }

    private fun setupAttributes(attrs: AttributeSet?) {
        if (attrs == null) return

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RangeSeekBar)

        trackColor = typedArray.getColor(
            R.styleable.RangeSeekBar_trackColor,
            Color.GRAY
        )
        trackProgressColor = typedArray.getColor(
            R.styleable.RangeSeekBar_trackProgressColor,
            Color.BLUE
        )
        thumbColor = typedArray.getColor(
            R.styleable.RangeSeekBar_thumbColor,
            Color.WHITE
        )
        thumbBorderColor = typedArray.getColor(
            R.styleable.RangeSeekBar_thumbBorderColor,
            Color.BLUE
        )

        trackHeight = typedArray.getDimension(
            R.styleable.RangeSeekBar_trackHeight,
            dpToPx(4f)
        )
        thumbRadius = typedArray.getDimension(
            R.styleable.RangeSeekBar_thumbRadius,
            dpToPx(12f)
        )

        minValue = typedArray.getInt(R.styleable.RangeSeekBar_minValue, 0)
        maxValue = typedArray.getInt(R.styleable.RangeSeekBar_maxValue, 100)
        leftThumbValue = typedArray.getInt(R.styleable.RangeSeekBar_leftThumbValue, 0)
        rightThumbValue = typedArray.getInt(R.styleable.RangeSeekBar_rightThumbValue, 100)

        typedArray.recycle()
    }

    private fun setupPaints() {
        trackPaint.color = trackColor
        trackPaint.strokeWidth = trackHeight
        trackPaint.style = Paint.Style.STROKE
        trackPaint.strokeCap = Paint.Cap.ROUND

        trackProgressPaint.color = trackProgressColor
        trackProgressPaint.strokeWidth = trackHeight
        trackProgressPaint.style = Paint.Style.STROKE
        trackProgressPaint.strokeCap = Paint.Cap.ROUND

        thumbPaint.color = thumbColor
        thumbPaint.style = Paint.Style.FILL

        thumbBorderPaint.color = thumbBorderColor
        thumbBorderPaint.style = Paint.Style.STROKE
        thumbBorderPaint.strokeWidth = thumbBorderWidth
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateThumbPositions()
    }

    private fun updateThumbPositions() {
        val trackLength = width - 2 * thumbRadius
        leftThumbPosition = thumbRadius +
                (leftThumbValue - minValue).toFloat() / (maxValue - minValue) * trackLength
        rightThumbPosition = thumbRadius +
                (rightThumbValue - minValue).toFloat() / (maxValue - minValue) * trackLength
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        updateThumbPositions()

        val centerY = height / 2f

        canvas.drawLine(
            thumbRadius,
            centerY,
            width - thumbRadius,
            centerY,
            trackPaint
        )

        canvas.drawLine(
            leftThumbPosition,
            centerY,
            rightThumbPosition,
            centerY,
            trackProgressPaint
        )

        canvas.drawCircle(leftThumbPosition, centerY, thumbRadius, thumbBorderPaint)
        canvas.drawCircle(leftThumbPosition, centerY, thumbRadius - thumbBorderWidth, thumbPaint)

        canvas.drawCircle(rightThumbPosition, centerY, thumbRadius, thumbBorderPaint)
        canvas.drawCircle(rightThumbPosition, centerY, thumbRadius - thumbBorderWidth, thumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                handleActionDown(event.x)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                handleActionMove(event.x)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isLeftThumbDragging = false
                isRightThumbDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleActionDown(x: Float) {
        val leftThumbRect = RectF(
            leftThumbPosition - thumbRadius * 2,
            height / 2f - thumbRadius * 2,
            leftThumbPosition + thumbRadius * 2,
            height / 2f + thumbRadius * 2
        )

        val rightThumbRect = RectF(
            rightThumbPosition - thumbRadius * 2,
            height / 2f - thumbRadius * 2,
            rightThumbPosition + thumbRadius * 2,
            height / 2f + thumbRadius * 2
        )

        isLeftThumbDragging = leftThumbRect.contains(x, height / 2f)
        isRightThumbDragging = rightThumbRect.contains(x, height / 2f)

        if (!isLeftThumbDragging && !isRightThumbDragging) {
            val distanceToLeft = Math.abs(x - leftThumbPosition)
            val distanceToRight = Math.abs(x - rightThumbPosition)

            if (distanceToLeft < distanceToRight) {
                isLeftThumbDragging = true
            } else {
                isRightThumbDragging = true
            }
        }
    }

    private fun handleActionMove(x: Float) {
        val trackLength = width - 2 * thumbRadius
        val clampedX = x.coerceIn(thumbRadius, width - thumbRadius)

        if (isLeftThumbDragging) {
            val newValue = minValue +
                    ((clampedX - thumbRadius) / trackLength * (maxValue - minValue)).toInt()
            leftThumbValue = newValue.coerceAtMost(rightThumbValue - 1)
        } else if (isRightThumbDragging) {
            val newValue = minValue +
                    ((clampedX - thumbRadius) / trackLength * (maxValue - minValue)).toInt()
            rightThumbValue = newValue.coerceAtLeast(leftThumbValue + 1)
        }

        invalidate()
    }

    fun setRange(leftValue: Int, rightValue: Int) {
        leftThumbValue = leftValue.coerceIn(minValue, maxValue)
        rightThumbValue = rightValue.coerceIn(minValue, maxValue)
        if (leftThumbValue >= rightThumbValue) {
            leftThumbValue = rightThumbValue - 1
        }
        invalidate()
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}