package com.sandrios.sandriosCamera.internal.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager

import com.sandrios.sandriosCamera.R
import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration
import com.sandrios.sandriosCamera.internal.ui.model.PhotoQualityOption
import com.sandrios.sandriosCamera.internal.ui.model.VideoQualityOption
import com.sandrios.sandriosCamera.internal.ui.preview.PreviewActivity
import com.sandrios.sandriosCamera.internal.ui.view.CameraControlPanel
import com.sandrios.sandriosCamera.internal.ui.view.CameraSwitchView
import com.sandrios.sandriosCamera.internal.ui.view.FlashSwitchView
import com.sandrios.sandriosCamera.internal.ui.view.MediaActionSwitchView
import com.sandrios.sandriosCamera.internal.ui.view.RecordButton
import com.sandrios.sandriosCamera.internal.utils.Size
import com.sandrios.sandriosCamera.internal.utils.Utils

/**
 * Created by Arpit Gandhi on 12/1/16.
 */

abstract class BaseSandriosActivity<CameraId> : SandriosCameraActivity<CameraId>(), RecordButton.RecordButtonListener, FlashSwitchView.FlashModeSwitchListener, MediaActionSwitchView.OnMediaActionStateChangeListener, CameraSwitchView.OnCameraTypeChangeListener, CameraControlPanel.SettingsClickListener, CameraControlPanel.PickerItemClickListener {
    override var requestCode = -1
        protected set
    @CameraConfiguration.MediaAction
    override var mediaAction = CameraConfiguration.MEDIA_ACTION_BOTH
        protected set
    @CameraConfiguration.MediaQuality
    override var mediaQuality = CameraConfiguration.MEDIA_QUALITY_HIGHEST
        protected set
    @CameraConfiguration.MediaQuality
    protected var passedMediaQuality = CameraConfiguration.MEDIA_QUALITY_HIGHEST
    protected var videoQualities: Array<CharSequence>
    protected var photoQualities: Array<CharSequence>
    protected var enableImageCrop = false
    override var videoDuration = -1
        protected set
    override var videoFileSize: Long = -1
        protected set
    protected var minimumVideoDuration = -1
    protected var showPicker = true
    @MediaActionSwitchView.MediaActionState
    protected var currentMediaActionState: Int = 0
    @CameraSwitchView.CameraType
    protected var currentCameraType = CameraSwitchView.CAMERA_TYPE_REAR
    @CameraConfiguration.MediaQuality
    protected var newQuality = -1
    private var cameraControlPanel: CameraControlPanel? = null
    private var settingsDialog: AlertDialog? = null

    @CameraConfiguration.FlashMode
    override var flashMode = CameraConfiguration.FLASH_MODE_AUTO
        protected set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onProcessBundle(savedInstanceState: Bundle) {
        super.onProcessBundle(savedInstanceState)

        extractConfiguration(intent.extras)
        currentMediaActionState = if (mediaAction == CameraConfiguration.MEDIA_ACTION_VIDEO)
            MediaActionSwitchView.ACTION_VIDEO
        else
            MediaActionSwitchView.ACTION_PHOTO
    }

    override fun onCameraControllerReady() {
        super.onCameraControllerReady()

        videoQualities = videoQualityOptions
        photoQualities = photoQualityOptions
    }

    override fun onResume() {
        super.onResume()

        cameraControlPanel!!.lockControls()
        cameraControlPanel!!.allowRecord(false)
        cameraControlPanel!!.showPicker(showPicker)
    }

    override fun onPause() {
        super.onPause()

        cameraControlPanel!!.lockControls()
        cameraControlPanel!!.allowRecord(false)
    }

    private fun extractConfiguration(bundle: Bundle?) {
        if (bundle != null) {
            if (bundle.containsKey(CameraConfiguration.Arguments.REQUEST_CODE))
                requestCode = bundle.getInt(CameraConfiguration.Arguments.REQUEST_CODE)

            if (bundle.containsKey(CameraConfiguration.Arguments.MEDIA_ACTION)) {
                when (bundle.getInt(CameraConfiguration.Arguments.MEDIA_ACTION)) {
                    CameraConfiguration.MEDIA_ACTION_PHOTO -> mediaAction = CameraConfiguration.MEDIA_ACTION_PHOTO
                    CameraConfiguration.MEDIA_ACTION_VIDEO -> mediaAction = CameraConfiguration.MEDIA_ACTION_VIDEO
                    else -> mediaAction = CameraConfiguration.MEDIA_ACTION_BOTH
                }
            }

            if (bundle.containsKey(CameraConfiguration.Arguments.MEDIA_QUALITY)) {
                when (bundle.getInt(CameraConfiguration.Arguments.MEDIA_QUALITY)) {
                    CameraConfiguration.MEDIA_QUALITY_AUTO -> mediaQuality = CameraConfiguration.MEDIA_QUALITY_AUTO
                    CameraConfiguration.MEDIA_QUALITY_HIGHEST -> mediaQuality = CameraConfiguration.MEDIA_QUALITY_HIGHEST
                    CameraConfiguration.MEDIA_QUALITY_HIGH -> mediaQuality = CameraConfiguration.MEDIA_QUALITY_HIGH
                    CameraConfiguration.MEDIA_QUALITY_MEDIUM -> mediaQuality = CameraConfiguration.MEDIA_QUALITY_MEDIUM
                    CameraConfiguration.MEDIA_QUALITY_LOW -> mediaQuality = CameraConfiguration.MEDIA_QUALITY_LOW
                    CameraConfiguration.MEDIA_QUALITY_LOWEST -> mediaQuality = CameraConfiguration.MEDIA_QUALITY_LOWEST
                    else -> mediaQuality = CameraConfiguration.MEDIA_QUALITY_MEDIUM
                }
                passedMediaQuality = mediaQuality
            }

            if (bundle.containsKey(CameraConfiguration.Arguments.VIDEO_DURATION))
                videoDuration = bundle.getInt(CameraConfiguration.Arguments.VIDEO_DURATION)

            if (bundle.containsKey(CameraConfiguration.Arguments.VIDEO_FILE_SIZE))
                videoFileSize = bundle.getLong(CameraConfiguration.Arguments.VIDEO_FILE_SIZE)

            if (bundle.containsKey(CameraConfiguration.Arguments.MINIMUM_VIDEO_DURATION))
                minimumVideoDuration = bundle.getInt(CameraConfiguration.Arguments.MINIMUM_VIDEO_DURATION)

            if (bundle.containsKey(CameraConfiguration.Arguments.SHOW_PICKER))
                showPicker = bundle.getBoolean(CameraConfiguration.Arguments.SHOW_PICKER)

            if (bundle.containsKey(CameraConfiguration.Arguments.ENABLE_CROP))
                enableImageCrop = bundle.getBoolean(CameraConfiguration.Arguments.ENABLE_CROP)

            if (bundle.containsKey(CameraConfiguration.Arguments.FLASH_MODE))
                when (bundle.getInt(CameraConfiguration.Arguments.FLASH_MODE)) {
                    CameraConfiguration.FLASH_MODE_AUTO -> flashMode = CameraConfiguration.FLASH_MODE_AUTO
                    CameraConfiguration.FLASH_MODE_ON -> flashMode = CameraConfiguration.FLASH_MODE_ON
                    CameraConfiguration.FLASH_MODE_OFF -> flashMode = CameraConfiguration.FLASH_MODE_OFF
                    else -> flashMode = CameraConfiguration.FLASH_MODE_AUTO
                }
        }
    }

    internal override fun getUserContentView(layoutInflater: LayoutInflater, parent: ViewGroup): View {
        cameraControlPanel = layoutInflater.inflate(R.layout.user_control_layout, parent, false) as CameraControlPanel

        if (cameraControlPanel != null) {
            cameraControlPanel!!.setup(mediaAction)

            when (flashMode) {
                CameraConfiguration.FLASH_MODE_AUTO -> cameraControlPanel!!.setFlasMode(FlashSwitchView.FLASH_AUTO)
                CameraConfiguration.FLASH_MODE_ON -> cameraControlPanel!!.setFlasMode(FlashSwitchView.FLASH_ON)
                CameraConfiguration.FLASH_MODE_OFF -> cameraControlPanel!!.setFlasMode(FlashSwitchView.FLASH_OFF)
            }

            cameraControlPanel!!.setRecordButtonListener(this)
            cameraControlPanel!!.setFlashModeSwitchListener(this)
            cameraControlPanel!!.setOnMediaActionStateChangeListener(this)
            cameraControlPanel!!.setOnCameraTypeChangeListener(this)
            cameraControlPanel!!.setMaxVideoDuration(videoDuration)
            cameraControlPanel!!.setMaxVideoFileSize(videoFileSize)
            cameraControlPanel!!.setSettingsClickListener(this)
            cameraControlPanel!!.setPickerItemClickListener(this)
            cameraControlPanel!!.shouldShowCrop(enableImageCrop)
        }
        return cameraControlPanel
    }

    override fun onSettingsClick() {
        val builder = AlertDialog.Builder(this)

        if (currentMediaActionState == MediaActionSwitchView.ACTION_VIDEO) {
            builder.setSingleChoiceItems(videoQualities, videoOptionCheckedIndex, videoOptionSelectedListener)
            if (videoFileSize > 0)
                builder.setTitle(String.format(getString(R.string.settings_video_quality_title),
                        "(Max " + ((videoFileSize / (1024 * 1024)).toString() + " MB)").toString()))
            else
                builder.setTitle(String.format(getString(R.string.settings_video_quality_title), ""))
        } else {
            builder.setSingleChoiceItems(photoQualities, photoOptionCheckedIndex, photoOptionSelectedListener)
            builder.setTitle(R.string.settings_photo_quality_title)
        }

        builder.setPositiveButton(R.string.ok_label) { dialogInterface, i ->
            if (newQuality > 0 && newQuality != mediaQuality) {
                mediaQuality = newQuality
                dialogInterface.dismiss()
                cameraControlPanel!!.lockControls()
                cameraController.switchQuality()
            }
        }
        builder.setNegativeButton(R.string.cancel_label) { dialogInterface, i -> dialogInterface.dismiss() }
        settingsDialog = builder.create()
        settingsDialog!!.show()
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(settingsDialog!!.window!!.attributes)
        layoutParams.width = Utils.convertDipToPixels(this, 350)
        layoutParams.height = Utils.convertDipToPixels(this, 350)
        settingsDialog!!.window!!.attributes = layoutParams
    }

    override fun onItemClick(filePath: Uri) {
        val resultIntent = Intent()
        resultIntent.putExtra(CameraConfiguration.Arguments.FILE_PATH,
                filePath.toString())
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onCameraTypeChanged(@CameraSwitchView.CameraType cameraType: Int) {
        if (currentCameraType == cameraType) return
        currentCameraType = cameraType

        cameraControlPanel!!.lockControls()
        cameraControlPanel!!.allowRecord(false)

        val cameraFace = if (cameraType == CameraSwitchView.CAMERA_TYPE_FRONT)
            CameraConfiguration.CAMERA_FACE_FRONT
        else
            CameraConfiguration.CAMERA_FACE_REAR

        cameraController.switchCamera(cameraFace)
    }


    override fun onFlashModeChanged(@FlashSwitchView.FlashMode mode: Int) {
        when (mode) {
            FlashSwitchView.FLASH_AUTO -> {
                flashMode = CameraConfiguration.FLASH_MODE_AUTO
                cameraController.setFlashMode(CameraConfiguration.FLASH_MODE_AUTO)
            }
            FlashSwitchView.FLASH_ON -> {
                flashMode = CameraConfiguration.FLASH_MODE_ON
                cameraController.setFlashMode(CameraConfiguration.FLASH_MODE_ON)
            }
            FlashSwitchView.FLASH_OFF -> {
                flashMode = CameraConfiguration.FLASH_MODE_OFF
                cameraController.setFlashMode(CameraConfiguration.FLASH_MODE_OFF)
            }
        }
    }


    override fun onMediaActionChanged(mediaActionState: Int) {
        if (currentMediaActionState == mediaActionState) return
        currentMediaActionState = mediaActionState
    }

    override fun onTakePhotoButtonPressed() {
        cameraController.takePhoto()
    }

    override fun onStartRecordingButtonPressed() {
        cameraController.startVideoRecord()
    }

    override fun onStopRecordingButtonPressed() {
        cameraController.stopVideoRecord()
    }

    override fun onScreenRotation(degrees: Int) {
        cameraControlPanel!!.rotateControls(degrees)
        rotateSettingsDialog(degrees)
    }

    override fun getMinimumVideoDuration(): Int {
        return minimumVideoDuration / 1000
    }

    override val activity: Activity
        get() = this

    override fun updateCameraPreview(size: Size, cameraPreview: View) {
        cameraControlPanel!!.unLockControls()
        cameraControlPanel!!.allowRecord(true)

        setCameraPreview(cameraPreview, size)
    }

    override fun updateUiForMediaAction(@CameraConfiguration.MediaAction mediaAction: Int) {

    }

    override fun updateCameraSwitcher(numberOfCameras: Int) {
        cameraControlPanel!!.allowCameraSwitching(numberOfCameras > 1)
    }

    override fun onPhotoTaken() {
        startPreviewActivity()
    }

    override fun onVideoRecordStart(width: Int, height: Int) {
        cameraControlPanel!!.onStartVideoRecord(cameraController.outputFile)
    }

    override fun onVideoRecordStop() {
        cameraControlPanel!!.allowRecord(false)
        cameraControlPanel!!.onStopVideoRecord()
        startPreviewActivity()
    }

    override fun releaseCameraPreview() {
        clearCameraPreview()
    }

    private fun startPreviewActivity() {
        val intent = PreviewActivity.newIntent(this,
                mediaAction, cameraController.outputFile.toString(), cameraControlPanel!!.showCrop())
        startActivityForResult(intent, REQUEST_PREVIEW_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PREVIEW_CODE) {
                if (PreviewActivity.isResultConfirm(data)) {
                    val resultIntent = Intent()
                    resultIntent.putExtra(CameraConfiguration.Arguments.FILE_PATH,
                            PreviewActivity.getMediaFilePatch(data))
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                } else if (PreviewActivity.isResultCancel(data)) {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                } else if (PreviewActivity.isResultRetake(data)) {
                    //ignore, just proceed the camera
                }
            }
        }
    }

    private fun rotateSettingsDialog(degrees: Int) {
        if (settingsDialog != null && settingsDialog!!.isShowing && Build.VERSION.SDK_INT > 10) {
            val dialogView = settingsDialog!!.window!!.decorView as ViewGroup
            for (i in 0..dialogView.childCount - 1) {
                dialogView.getChildAt(i).rotation = degrees.toFloat()
            }
        }
    }

    protected abstract val videoQualityOptions: Array<CharSequence>

    protected abstract val photoQualityOptions: Array<CharSequence>

    protected val videoOptionCheckedIndex: Int
        get() {
            var checkedIndex = -1
            if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_AUTO)
                checkedIndex = 0
            else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_HIGH)
                checkedIndex = 1
            else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_MEDIUM)
                checkedIndex = 2
            else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_LOW) checkedIndex = 3

            if (passedMediaQuality != CameraConfiguration.MEDIA_QUALITY_AUTO) checkedIndex--

            return checkedIndex
        }

    protected val photoOptionCheckedIndex: Int
        get() {
            var checkedIndex = -1
            if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_HIGHEST)
                checkedIndex = 0
            else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_HIGH)
                checkedIndex = 1
            else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_MEDIUM)
                checkedIndex = 2
            else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_LOWEST) checkedIndex = 3
            return checkedIndex
        }

    protected val videoOptionSelectedListener: DialogInterface.OnClickListener
        get() = DialogInterface.OnClickListener { dialogInterface, index -> newQuality = (videoQualities[index] as VideoQualityOption).mediaQuality }

    protected val photoOptionSelectedListener: DialogInterface.OnClickListener
        get() = DialogInterface.OnClickListener { dialogInterface, index -> newQuality = (photoQualities[index] as PhotoQualityOption).mediaQuality }

    companion object {

        val ACTION_CONFIRM = 900
        val ACTION_RETAKE = 901
        val ACTION_CANCEL = 902
        protected val REQUEST_PREVIEW_CODE = 1001
    }
}
