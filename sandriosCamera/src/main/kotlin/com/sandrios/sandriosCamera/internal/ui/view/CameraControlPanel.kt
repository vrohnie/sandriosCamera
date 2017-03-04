package com.sandrios.sandriosCamera.internal.ui.view

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView

import com.sandrios.sandriosCamera.R
import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration
import com.sandrios.sandriosCamera.internal.utils.DateTimeUtils

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Created by Arpit Gandhi on 7/6/16.
 */
class CameraControlPanel @JvmOverloads constructor(private val context: Context, attrs: AttributeSet? = null) : RelativeLayout(context, attrs), RecordButton.RecordButtonListener, MediaActionSwitchView.OnMediaActionStateChangeListener {

    private var cameraSwitchView: CameraSwitchView? = null
    private var recordButton: RecordButton? = null
    private var mediaActionSwitchView: MediaActionSwitchView? = null
    private var flashSwitchView: FlashSwitchView? = null
    private var recordDurationText: TextView? = null
    private var recordSizeText: TextView? = null
    private var settingsButton: ImageButton? = null
    private var recyclerView: RecyclerView? = null

    private var imageGalleryAdapter: ImageGalleryAdapter? = null
    private var recordButtonListener: RecordButton.RecordButtonListener? = null
    private var onMediaActionStateChangeListener: MediaActionSwitchView.OnMediaActionStateChangeListener? = null
    private var onCameraTypeChangeListener: CameraSwitchView.OnCameraTypeChangeListener? = null
    private var flashModeSwitchListener: FlashSwitchView.FlashModeSwitchListener? = null
    private var settingsClickListener: SettingsClickListener? = null
    private var pickerItemClickListener: PickerItemClickListener? = null

    private var countDownTimer: TimerTaskBase? = null
    private var maxVideoFileSize: Long = 0
    private var mediaFilePath: String? = null
    private var hasFlash = false
    @MediaActionSwitchView.MediaActionState
    private var mediaActionState: Int = 0
    private var mediaAction: Int = 0
    private var showImageCrop = false
    private var fileObserver: FileObserver? = null

    init {
        init()
    }

    private fun init() {
        hasFlash = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

        LayoutInflater.from(context).inflate(R.layout.camera_control_panel_layout, this)
        setBackgroundColor(Color.TRANSPARENT)
        imageGalleryAdapter = ImageGalleryAdapter(context)

        settingsButton = findViewById(R.id.settings_view) as ImageButton
        cameraSwitchView = findViewById(R.id.front_back_camera_switcher) as CameraSwitchView
        mediaActionSwitchView = findViewById(R.id.photo_video_camera_switcher) as MediaActionSwitchView
        recordButton = findViewById(R.id.record_button) as RecordButton
        flashSwitchView = findViewById(R.id.flash_switch_view) as FlashSwitchView
        recordDurationText = findViewById(R.id.record_duration_text) as TextView
        recordSizeText = findViewById(R.id.record_size_mb_text) as TextView
        recyclerView = findViewById(R.id.recycler_view) as RecyclerView
        recyclerView!!.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView!!.adapter = imageGalleryAdapter
        cameraSwitchView!!.setOnCameraTypeChangeListener(onCameraTypeChangeListener)
        mediaActionSwitchView!!.setOnMediaActionStateChangeListener(this)

        setOnCameraTypeChangeListener(onCameraTypeChangeListener)
        setOnMediaActionStateChangeListener(onMediaActionStateChangeListener)
        setFlashModeSwitchListener(flashModeSwitchListener)
        setRecordButtonListener(recordButtonListener)

        settingsButton!!.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_settings_white_24dp))
        settingsButton!!.setOnClickListener { if (settingsClickListener != null) settingsClickListener!!.onSettingsClick() }

        if (hasFlash)
            flashSwitchView!!.visibility = View.VISIBLE
        else
            flashSwitchView!!.visibility = View.GONE

        countDownTimer = TimerTask(recordDurationText)

        imageGalleryAdapter!!.setOnItemClickListener { view, position -> pickerItemClickListener!!.onItemClick(imageGalleryAdapter!!.getItem(position).imageUri) }
    }

    fun lockControls() {
        cameraSwitchView!!.isEnabled = false
        recordButton!!.isEnabled = false
        settingsButton!!.isEnabled = false
        flashSwitchView!!.isEnabled = false
    }

    fun unLockControls() {
        cameraSwitchView!!.isEnabled = true
        recordButton!!.isEnabled = true
        settingsButton!!.isEnabled = true
        flashSwitchView!!.isEnabled = true
    }

    fun setup(mediaAction: Int) {
        this.mediaAction = mediaAction
        if (CameraConfiguration.MEDIA_ACTION_VIDEO == mediaAction) {
            recordButton!!.setup(mediaAction, this)
            flashSwitchView!!.visibility = View.GONE
        } else {
            recordButton!!.setup(CameraConfiguration.MEDIA_ACTION_PHOTO, this)
        }

        if (CameraConfiguration.MEDIA_ACTION_BOTH != mediaAction) {
            mediaActionSwitchView!!.visibility = View.GONE
        } else
            mediaActionSwitchView!!.visibility = View.VISIBLE
    }

    fun setMediaFilePath(mediaFile: File) {
        this.mediaFilePath = mediaFile.toString()
    }

    fun setMaxVideoFileSize(maxVideoFileSize: Long) {
        this.maxVideoFileSize = maxVideoFileSize
    }

    fun setMaxVideoDuration(maxVideoDurationInMillis: Int) {
        if (maxVideoDurationInMillis > 0)
            countDownTimer = CountdownTask(recordDurationText, maxVideoDurationInMillis)
        else
            countDownTimer = TimerTask(recordDurationText)
    }

    fun setFlasMode(@FlashSwitchView.FlashMode flashMode: Int) {
        flashSwitchView!!.setFlashMode(flashMode)
    }

    fun setMediaActionState(@MediaActionSwitchView.MediaActionState actionState: Int) {
        if (mediaActionState == actionState) return
        if (MediaActionSwitchView.ACTION_PHOTO == actionState) {
            recordButton!!.setMediaAction(CameraConfiguration.MEDIA_ACTION_PHOTO)
            if (hasFlash)
                flashSwitchView!!.visibility = View.VISIBLE
        } else {
            recordButton!!.setMediaAction(CameraConfiguration.MEDIA_ACTION_VIDEO)
            flashSwitchView!!.visibility = View.GONE
        }
        mediaActionState = actionState
        mediaActionSwitchView!!.setMediaActionState(actionState)
    }

    fun setRecordButtonListener(recordButtonListener: RecordButton.RecordButtonListener) {
        this.recordButtonListener = recordButtonListener
    }

    fun rotateControls(rotation: Int) {
        if (Build.VERSION.SDK_INT > 10) {
            cameraSwitchView!!.rotation = rotation.toFloat()
            mediaActionSwitchView!!.rotation = rotation.toFloat()
            flashSwitchView!!.rotation = rotation.toFloat()
            recordDurationText!!.rotation = rotation.toFloat()
            recordSizeText!!.rotation = rotation.toFloat()
        }
    }

    fun setOnMediaActionStateChangeListener(onMediaActionStateChangeListener: MediaActionSwitchView.OnMediaActionStateChangeListener) {
        this.onMediaActionStateChangeListener = onMediaActionStateChangeListener
    }

    fun setOnCameraTypeChangeListener(onCameraTypeChangeListener: CameraSwitchView.OnCameraTypeChangeListener) {
        this.onCameraTypeChangeListener = onCameraTypeChangeListener
        if (cameraSwitchView != null)
            cameraSwitchView!!.setOnCameraTypeChangeListener(this.onCameraTypeChangeListener)
    }

    fun setFlashModeSwitchListener(flashModeSwitchListener: FlashSwitchView.FlashModeSwitchListener) {
        this.flashModeSwitchListener = flashModeSwitchListener
        if (flashSwitchView != null)
            flashSwitchView!!.setFlashSwitchListener(this.flashModeSwitchListener!!)
    }

    fun setSettingsClickListener(settingsClickListener: SettingsClickListener) {
        this.settingsClickListener = settingsClickListener
    }

    fun setPickerItemClickListener(pickerItemClickListener: PickerItemClickListener) {
        this.pickerItemClickListener = pickerItemClickListener
    }

    override fun onTakePhotoButtonPressed() {
        if (recordButtonListener != null)
            recordButtonListener!!.onTakePhotoButtonPressed()
    }

    fun onStartVideoRecord(mediaFile: File) {
        setMediaFilePath(mediaFile)
        if (maxVideoFileSize > 0) {
            recordSizeText!!.text = "1Mb" + " / " + maxVideoFileSize / (1024 * 1024) + "Mb"
            recordSizeText!!.visibility = View.VISIBLE
            try {
                fileObserver = object : FileObserver(this.mediaFilePath) {
                    private var lastUpdateSize: Long = 0

                    override fun onEvent(event: Int, path: String) {
                        val fileSize = mediaFile.length() / (1024 * 1024)
                        if (fileSize - lastUpdateSize >= 1) {
                            lastUpdateSize = fileSize
                            recordSizeText!!.post { recordSizeText!!.text = fileSize.toString() + "Mb" + " / " + maxVideoFileSize / (1024 * 1024) + "Mb" }
                        }
                    }
                }
                fileObserver!!.startWatching()
            } catch (e: Exception) {
                Log.e("FileObserver", "setMediaFilePath: ", e)
            }

        }
        countDownTimer!!.start()

    }

    fun allowRecord(isAllowed: Boolean) {
        recordButton!!.isEnabled = isAllowed
    }

    fun showPicker(isShown: Boolean) {
        recyclerView!!.visibility = if (isShown) View.VISIBLE else View.GONE
    }

    fun showCrop(): Boolean {
        return showImageCrop
    }

    fun shouldShowCrop(showImageCrop: Boolean) {
        this.showImageCrop = showImageCrop
    }

    fun allowCameraSwitching(isAllowed: Boolean) {
        cameraSwitchView!!.visibility = if (isAllowed) View.VISIBLE else View.GONE
    }

    fun onStopVideoRecord() {
        if (fileObserver != null)
            fileObserver!!.stopWatching()
        countDownTimer!!.stop()
        recyclerView!!.visibility = View.VISIBLE
        recordSizeText!!.visibility = View.GONE
        cameraSwitchView!!.visibility = View.VISIBLE
        settingsButton!!.visibility = View.VISIBLE

        if (CameraConfiguration.MEDIA_ACTION_BOTH != mediaAction) {
            mediaActionSwitchView!!.visibility = View.GONE
        } else
            mediaActionSwitchView!!.visibility = View.VISIBLE
        recordButton!!.recordState = RecordButton.READY_FOR_RECORD_STATE
    }

    override fun onStartRecordingButtonPressed() {
        cameraSwitchView!!.visibility = View.GONE
        mediaActionSwitchView!!.visibility = View.GONE
        settingsButton!!.visibility = View.GONE
        recyclerView!!.visibility = View.GONE

        if (recordButtonListener != null)
            recordButtonListener!!.onStartRecordingButtonPressed()
    }

    override fun onStopRecordingButtonPressed() {
        onStopVideoRecord()
        if (recordButtonListener != null)
            recordButtonListener!!.onStopRecordingButtonPressed()
    }

    override fun onMediaActionChanged(mediaActionState: Int) {
        setMediaActionState(mediaActionState)
        if (onMediaActionStateChangeListener != null)
            onMediaActionStateChangeListener!!.onMediaActionChanged(this.mediaActionState)
    }

    interface SettingsClickListener {
        fun onSettingsClick()
    }

    interface PickerItemClickListener {
        fun onItemClick(filePath: Uri)
    }

    internal abstract inner class TimerTaskBase(protected var timerView: TextView) {
        protected var handler = Handler(Looper.getMainLooper())
        protected var alive = false
        protected var recordingTimeSeconds: Long = 0
        protected var recordingTimeMinutes: Long = 0

        internal abstract fun stop()

        internal abstract fun start()
    }

    private inner class CountdownTask(timerView: TextView, maxDurationMilliseconds: Int) : TimerTaskBase(timerView), Runnable {

        private val maxDurationMilliseconds = 0

        init {
            this.maxDurationMilliseconds = maxDurationMilliseconds
        }

        override fun run() {

            recordingTimeSeconds--

            val millis = recordingTimeSeconds.toInt() * 1000

            timerView.text = String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(millis.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(millis.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis.toLong()))
            )

            if (recordingTimeSeconds < 10) {
                timerView.setTextColor(Color.RED)
            }

            if (alive && recordingTimeSeconds > 0) handler.postDelayed(this, DateTimeUtils.SECOND)
        }

        override fun stop() {
            timerView.visibility = View.INVISIBLE
            alive = false
        }

        override fun start() {
            alive = true
            recordingTimeSeconds = (maxDurationMilliseconds / 1000).toLong()
            timerView.setTextColor(Color.WHITE)
            timerView.text = String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(maxDurationMilliseconds.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(maxDurationMilliseconds.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(maxDurationMilliseconds.toLong()))
            )
            timerView.visibility = View.VISIBLE
            handler.postDelayed(this, DateTimeUtils.SECOND)
        }
    }

    private inner class TimerTask(timerView: TextView) : TimerTaskBase(timerView), Runnable {

        override fun run() {
            recordingTimeSeconds++

            if (recordingTimeSeconds == 60) {
                recordingTimeSeconds = 0
                recordingTimeMinutes++
            }
            timerView.text = String.format("%02d:%02d", recordingTimeMinutes, recordingTimeSeconds)
            if (alive) handler.postDelayed(this, DateTimeUtils.SECOND)
        }

        public override fun start() {
            alive = true
            recordingTimeMinutes = 0
            recordingTimeSeconds = 0
            timerView.text = String.format("%02d:%02d", recordingTimeMinutes, recordingTimeSeconds)
            timerView.visibility = View.VISIBLE
            handler.postDelayed(this, DateTimeUtils.SECOND)
        }

        public override fun stop() {
            timerView.visibility = View.INVISIBLE
            alive = false
        }
    }

}
