package com.sandrios.sandriosCamera.internal.ui.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.IntDef
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton

import com.sandrios.sandriosCamera.R
import com.sandrios.sandriosCamera.internal.utils.Utils

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Created by Arpit Gandhi on 6/24/16.
 */
class MediaActionSwitchView @JvmOverloads constructor(private val context: Context, attrs: AttributeSet? = null) : ImageButton(context, attrs) {
    private var currentMediaActionState = ACTION_PHOTO
    private var onMediaActionStateChangeListener: OnMediaActionStateChangeListener? = null
    private var photoDrawable: Drawable? = null
    private var videoDrawable: Drawable? = null
    private var padding = 5

    init {
        initializeView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : this(context, attrs) {}

    private fun initializeView() {
        photoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_photo_camera_white_24dp)
        photoDrawable = DrawableCompat.wrap(photoDrawable!!)
        DrawableCompat.setTintList(photoDrawable!!.mutate(), ContextCompat.getColorStateList(context, R.drawable.switch_camera_mode_selector))

        videoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_videocam_white_24dp)
        videoDrawable = DrawableCompat.wrap(videoDrawable!!)
        DrawableCompat.setTintList(videoDrawable!!.mutate(), ContextCompat.getColorStateList(context, R.drawable.switch_camera_mode_selector))

        setBackgroundResource(R.drawable.circle_frame_background_dark)
        //        setBackgroundResource(R.drawable.circle_frame_background);

        setOnClickListener(MediaActionClickListener())
        setIcons()
        padding = Utils.convertDipToPixels(context, padding)
        setPadding(padding, padding, padding, padding)
    }

    private fun setIcons() {
        if (currentMediaActionState == ACTION_PHOTO) {
            setImageDrawable(videoDrawable)
        } else
            setImageDrawable(photoDrawable)
    }

    fun setMediaActionState(@MediaActionState currentMediaActionState: Int) {
        this.currentMediaActionState = currentMediaActionState
        setIcons()
    }

    fun setOnMediaActionStateChangeListener(onMediaActionStateChangeListener: OnMediaActionStateChangeListener) {
        this.onMediaActionStateChangeListener = onMediaActionStateChangeListener
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

    @IntDef(ACTION_PHOTO.toLong(), ACTION_VIDEO.toLong())
    @Retention(RetentionPolicy.SOURCE)
    annotation class MediaActionState

    interface OnMediaActionStateChangeListener {
        fun onMediaActionChanged(mediaActionState: Int)
    }

    private inner class MediaActionClickListener : View.OnClickListener {

        override fun onClick(view: View) {
            if (currentMediaActionState == ACTION_PHOTO) {
                currentMediaActionState = ACTION_VIDEO
            } else
                currentMediaActionState = ACTION_PHOTO

            setIcons()

            if (onMediaActionStateChangeListener != null)
                onMediaActionStateChangeListener!!.onMediaActionChanged(currentMediaActionState)
        }
    }

    companion object {

        val ACTION_PHOTO = 0
        val ACTION_VIDEO = 1
    }

}
