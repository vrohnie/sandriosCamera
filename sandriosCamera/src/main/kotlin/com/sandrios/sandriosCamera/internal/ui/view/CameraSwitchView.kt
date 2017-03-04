package com.sandrios.sandriosCamera.internal.ui.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.IntDef
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.AppCompatImageButton
import android.util.AttributeSet
import android.view.View

import com.sandrios.sandriosCamera.R
import com.sandrios.sandriosCamera.internal.utils.Utils

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Created by Arpit Gandhi on 6/24/16.
 */
class CameraSwitchView @JvmOverloads constructor(private val context: Context, attrs: AttributeSet? = null) : AppCompatImageButton(context, attrs) {
    private var onCameraTypeChangeListener: OnCameraTypeChangeListener? = null
    private var frontCameraDrawable: Drawable? = null
    private var rearCameraDrawable: Drawable? = null
    private var padding = 5
    @CameraType
    private var currentCameraType = CAMERA_TYPE_REAR

    init {
        initializeView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : this(context, attrs) {}

    private fun initializeView() {
        frontCameraDrawable = ContextCompat.getDrawable(context, R.drawable.ic_camera_front_white_24dp)
        frontCameraDrawable = DrawableCompat.wrap(frontCameraDrawable!!)
        DrawableCompat.setTintList(frontCameraDrawable!!.mutate(), ContextCompat.getColorStateList(context, R.drawable.switch_camera_mode_selector))

        rearCameraDrawable = ContextCompat.getDrawable(context, R.drawable.ic_camera_rear_white_24dp)
        rearCameraDrawable = DrawableCompat.wrap(rearCameraDrawable!!)
        DrawableCompat.setTintList(rearCameraDrawable!!.mutate(), ContextCompat.getColorStateList(context, R.drawable.switch_camera_mode_selector))

        setBackgroundResource(R.drawable.circle_frame_background_dark)
        setOnClickListener(CameraTypeClickListener())
        setIcons()
        padding = Utils.convertDipToPixels(context, padding)
        setPadding(padding, padding, padding, padding)
    }

    private fun setIcons() {
        if (currentCameraType == CAMERA_TYPE_REAR) {
            setImageDrawable(frontCameraDrawable)
        } else
            setImageDrawable(rearCameraDrawable)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (Build.VERSION.SDK_INT > 10) {
            if (enabled) {
                alpha = 1f
            } else {
                alpha = 0.5f
            }
        }
    }

    var cameraType: Int
        @CameraType
        get() = currentCameraType
        set(@CameraType cameraType) {
            this.currentCameraType = cameraType
            setIcons()
        }

    fun setOnCameraTypeChangeListener(onCameraTypeChangeListener: OnCameraTypeChangeListener) {
        this.onCameraTypeChangeListener = onCameraTypeChangeListener
    }

    @IntDef(CAMERA_TYPE_FRONT.toLong(), CAMERA_TYPE_REAR.toLong())
    @Retention(RetentionPolicy.SOURCE)
    annotation class CameraType

    interface OnCameraTypeChangeListener {
        fun onCameraTypeChanged(@CameraType cameraType: Int)
    }

    private inner class CameraTypeClickListener : View.OnClickListener {

        override fun onClick(view: View) {
            if (currentCameraType == CAMERA_TYPE_REAR) {
                currentCameraType = CAMERA_TYPE_FRONT
            } else
                currentCameraType = CAMERA_TYPE_REAR

            setIcons()

            if (onCameraTypeChangeListener != null)
                onCameraTypeChangeListener!!.onCameraTypeChanged(currentCameraType)
        }
    }

    companion object {

        val CAMERA_TYPE_FRONT = 0
        val CAMERA_TYPE_REAR = 1
    }
}
