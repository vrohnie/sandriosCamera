package com.sandrios.sandriosCamera.internal.ui.view

import android.annotation.TargetApi
import android.content.Context
import android.graphics.drawable.Drawable
import android.media.MediaActionSound
import android.os.Build
import android.support.annotation.IntDef
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton

import com.sandrios.sandriosCamera.R
import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration
import com.sandrios.sandriosCamera.internal.utils.Utils

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Created by Arpit Gandhi on 7/6/16.
 */
class RecordButton @JvmOverloads constructor(private val context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ImageButton(context, attrs, defStyleAttr) {
    private var mediaAction = CameraConfiguration.MEDIA_ACTION_PHOTO
    @RecordState
    private var currentState = TAKE_PHOTO_STATE
    private val takePhotoDrawable: Drawable
    private val startRecordDrawable: Drawable
    private val stopRecordDrawable: Drawable
    private val iconPadding = 8
    private val iconPaddingStop = 18
    private var listener: RecordButtonListener? = null

    init {
        takePhotoDrawable = ContextCompat.getDrawable(context, R.drawable.take_photo_button)
        startRecordDrawable = ContextCompat.getDrawable(context, R.drawable.start_video_record_button)
        stopRecordDrawable = ContextCompat.getDrawable(context, R.drawable.stop_button_background)
    }

    fun setup(@CameraConfiguration.MediaAction mediaAction: Int, listener: RecordButtonListener) {
        setMediaAction(mediaAction)
        this.listener = listener

        //        setBackground(ContextCompat.getDrawable(context, R.drawable.circle_frame_background_dark));
        if (Build.VERSION.SDK_INT > 15)
            background = ContextCompat.getDrawable(context, R.drawable.circle_frame_background)
        else
            setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.circle_frame_background))

        setIcon()
        setOnClickListener(RecordClickListener())
        isSoundEffectsEnabled = false
        setIconPadding(iconPadding)
    }

    private fun setIconPadding(paddingDP: Int) {
        val padding = Utils.convertDipToPixels(context, paddingDP)
        setPadding(padding, padding, padding, padding)
    }

    fun setMediaAction(@CameraConfiguration.MediaAction mediaAction: Int) {
        this.mediaAction = mediaAction
        if (CameraConfiguration.MEDIA_ACTION_PHOTO == mediaAction)
            currentState = TAKE_PHOTO_STATE
        else
            currentState = READY_FOR_RECORD_STATE
        recordState = currentState
        setIcon()
    }

    var recordState: Int
        @RecordState
        get() = currentState
        set(@RecordState state) {
            currentState = state
            setIcon()
        }

    fun setRecordButtonListener(listener: RecordButtonListener) {
        this.listener = listener
    }

    private fun setIcon() {
        if (CameraConfiguration.MEDIA_ACTION_VIDEO == mediaAction) {
            if (READY_FOR_RECORD_STATE == currentState) {
                setImageDrawable(startRecordDrawable)
                setIconPadding(iconPadding)
            } else if (RECORD_IN_PROGRESS_STATE == currentState) {
                setImageDrawable(stopRecordDrawable)
                setIconPadding(iconPaddingStop)
            }
        } else {
            setImageDrawable(takePhotoDrawable)
            setIconPadding(iconPadding)
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun takePhoto(sound: MediaActionSound) {
        sound.play(MediaActionSound.SHUTTER_CLICK)
        takePhoto()
    }

    private fun takePhoto() {
        if (listener != null)
            listener!!.onTakePhotoButtonPressed()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun startRecording(sound: MediaActionSound) {
        sound.play(MediaActionSound.START_VIDEO_RECORDING)
        startRecording()
    }

    private fun startRecording() {
        currentState = RECORD_IN_PROGRESS_STATE
        if (listener != null) {
            listener!!.onStartRecordingButtonPressed()
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun stopRecording(sound: MediaActionSound) {
        sound.play(MediaActionSound.STOP_VIDEO_RECORDING)
        stopRecording()
    }

    private fun stopRecording() {
        currentState = READY_FOR_RECORD_STATE
        if (listener != null) {
            listener!!.onStopRecordingButtonPressed()
        }
    }

    @IntDef(TAKE_PHOTO_STATE.toLong(), READY_FOR_RECORD_STATE.toLong(), RECORD_IN_PROGRESS_STATE.toLong())
    @Retention(RetentionPolicy.SOURCE)
    annotation class RecordState

    interface RecordButtonListener {

        fun onTakePhotoButtonPressed()

        fun onStartRecordingButtonPressed()

        fun onStopRecordingButtonPressed()
    }

    private inner class RecordClickListener : View.OnClickListener {

        private var lastClickTime: Long = 0

        override fun onClick(view: View) {
            if (System.currentTimeMillis() - lastClickTime < CLICK_DELAY) {
                return
            } else
                lastClickTime = System.currentTimeMillis()

            if (Build.VERSION.SDK_INT > 15) {
                val sound = MediaActionSound()
                if (TAKE_PHOTO_STATE == currentState) {
                    takePhoto(sound)
                } else if (READY_FOR_RECORD_STATE == currentState) {
                    startRecording(sound)
                } else if (RECORD_IN_PROGRESS_STATE == currentState) {
                    stopRecording(sound)
                }
            } else {
                if (TAKE_PHOTO_STATE == currentState) {
                    takePhoto()
                } else if (READY_FOR_RECORD_STATE == currentState) {
                    startRecording()
                } else if (RECORD_IN_PROGRESS_STATE == currentState) {
                    stopRecording()
                }
            }
            setIcon()
        }

        companion object {

            private val CLICK_DELAY = 1000
        }
    }

    companion object {

        val TAKE_PHOTO_STATE = 0
        val READY_FOR_RECORD_STATE = 1
        val RECORD_IN_PROGRESS_STATE = 2
    }

}
