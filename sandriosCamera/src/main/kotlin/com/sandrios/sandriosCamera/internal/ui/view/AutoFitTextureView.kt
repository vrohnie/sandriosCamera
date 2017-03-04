package com.sandrios.sandriosCamera.internal.ui.view

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.view.TextureView

/**
 * Created by Arpit Gandhi on 7/6/16.
 */
@SuppressLint("ViewConstructor")
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class AutoFitTextureView(context: Context, surfaceTextureListener: TextureView.SurfaceTextureListener) : TextureView(context, null) {

    private var ratioWidth = 0
    private var ratioHeight = 0

    init {
        setSurfaceTextureListener(surfaceTextureListener)
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated fromList the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.

     * @param width  Relative horizontal size
     * *
     * @param height Relative vertical size
     */
    fun setAspectRatio(width: Int, height: Int) {
        if (width < 0 || height < 0) {
            throw IllegalArgumentException("Size cannot be negative.")
        }
        ratioWidth = width
        ratioHeight = height

        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        val height = View.resolveSize(suggestedMinimumHeight, heightMeasureSpec)

        if (0 == ratioWidth || 0 == ratioHeight) {
            setMeasuredDimension(width, height)
        } else {
            if (width < height * (ratioWidth / ratioHeight.toFloat())) {
                setMeasuredDimension(width, (width * (ratioWidth / ratioHeight.toFloat())).toInt())
            } else {
                setMeasuredDimension((height * (ratioWidth / ratioHeight.toFloat())).toInt(), height)
            }
        }
    }

    companion object {

        private val TAG = "AutoFitTextureView"
    }
}
