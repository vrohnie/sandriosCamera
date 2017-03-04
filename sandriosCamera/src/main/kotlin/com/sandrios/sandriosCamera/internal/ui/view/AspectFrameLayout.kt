package com.sandrios.sandriosCamera.internal.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * Layout that adjusts to maintain a specific aspect ratio.
 */
class AspectFrameLayout : FrameLayout {

    private var targetAspectRatio = -1.0        // initially use default window size

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    fun setAspectRatio(aspectRatio: Double) {
        if (aspectRatio < 0) {
            throw IllegalArgumentException()
        }

        if (targetAspectRatio != aspectRatio) {
            targetAspectRatio = aspectRatio
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec

        if (targetAspectRatio > 0) {
            var initialWidth = View.MeasureSpec.getSize(widthMeasureSpec)
            var initialHeight = View.MeasureSpec.getSize(heightMeasureSpec)

            // padding
            val horizontalPadding = paddingLeft + paddingRight
            val verticalPadding = paddingTop + paddingBottom
            initialWidth -= horizontalPadding
            initialHeight -= verticalPadding

            val viewAspectRatio = initialWidth.toDouble() / initialHeight
            val aspectDifference = targetAspectRatio / viewAspectRatio - 1

            if (Math.abs(aspectDifference) < 0.01) {
                //no changes
            } else {
                if (aspectDifference > 0) {
                    initialHeight = (initialWidth / targetAspectRatio).toInt()
                } else {
                    initialWidth = (initialHeight * targetAspectRatio).toInt()
                }
                initialWidth += horizontalPadding
                initialHeight += verticalPadding
                widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(initialWidth, View.MeasureSpec.EXACTLY)
                heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(initialHeight, View.MeasureSpec.EXACTLY)
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    companion object {

        private val TAG = "AspectFrameLayout"
    }
}
