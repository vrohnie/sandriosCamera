package com.sandrios.sandriosCamera.internal.manager.impl

import android.annotation.TargetApi
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Build
import android.support.annotation.IntDef
import android.text.TextUtils
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager

import com.sandrios.sandriosCamera.internal.configuration.ConfigurationProvider
import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration
import com.sandrios.sandriosCamera.internal.manager.listener.CameraCloseListener
import com.sandrios.sandriosCamera.internal.manager.listener.CameraOpenListener
import com.sandrios.sandriosCamera.internal.manager.listener.CameraPhotoListener
import com.sandrios.sandriosCamera.internal.manager.listener.CameraVideoListener
import com.sandrios.sandriosCamera.internal.utils.CameraHelper
import com.sandrios.sandriosCamera.internal.utils.ImageSaver
import com.sandrios.sandriosCamera.internal.utils.Size

import java.io.File
import java.io.IOException
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.util.ArrayList
import java.util.Arrays
import java.util.Objects

/**
 * Created by Arpit Gandhi on 8/9/16.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2Manager private constructor() : BaseCameraManager<String, TextureView.SurfaceTextureListener>(), ImageReader.OnImageAvailableListener, TextureView.SurfaceTextureListener {
    private var cameraOpenListener: CameraOpenListener<String, TextureView.SurfaceTextureListener>? = null
    private var cameraPhotoListener: CameraPhotoListener? = null
    private var cameraVideoListener: CameraVideoListener? = null
    private var outputPath: File? = null
    @CameraPreviewState
    private var previewState = STATE_PREVIEW
    private var manager: CameraManager? = null
    private var cameraDevice: CameraDevice? = null
    private var previewRequest: CaptureRequest? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var captureSession: CameraCaptureSession? = null
    private var frontCameraCharacteristics: CameraCharacteristics? = null
    private var backCameraCharacteristics: CameraCharacteristics? = null
    private var frontCameraStreamConfigurationMap: StreamConfigurationMap? = null
    private var backCameraStreamConfigurationMap: StreamConfigurationMap? = null
    private var texture: SurfaceTexture? = null
    private var workingSurface: Surface? = null
    private var imageReader: ImageReader? = null
    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            currentInstance!!.cameraDevice = cameraDevice
            if (cameraOpenListener != null) {
                uiHandler.post {
                    if (!TextUtils.isEmpty(currentCameraId) && previewSize != null && currentInstance != null)
                        cameraOpenListener!!.onCameraOpened(currentCameraId, previewSize, currentInstance)
                }
            }
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraDevice.close()
            currentInstance!!.cameraDevice = null

            uiHandler.post { cameraOpenListener!!.onCameraOpenError() }
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            cameraDevice.close()
            currentInstance!!.cameraDevice = null

            uiHandler.post { cameraOpenListener!!.onCameraOpenError() }
        }
    }
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureProgressed(session: CameraCaptureSession,
                                         request: CaptureRequest,
                                         partialResult: CaptureResult) {
            processCaptureResult(partialResult)
        }

        override fun onCaptureCompleted(session: CameraCaptureSession,
                                        request: CaptureRequest,
                                        result: TotalCaptureResult) {
            processCaptureResult(result)
        }

    }

    override fun initializeCameraManager(configurationProvider: ConfigurationProvider, context: Context) {
        super.initializeCameraManager(configurationProvider, context)

        this.manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        windowSize = Size(size.x, size.y)

        try {
            val ids = manager!!.cameraIdList
            numberOfCameras = ids.size
            for (id in ids) {
                val characteristics = manager!!.getCameraCharacteristics(id)

                val orientation = characteristics.get(CameraCharacteristics.LENS_FACING)!!
                if (orientation == CameraCharacteristics.LENS_FACING_FRONT) {
                    faceFrontCameraId = id
                    faceFrontCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
                    frontCameraCharacteristics = characteristics
                } else {
                    faceBackCameraId = id
                    faceBackCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
                    backCameraCharacteristics = characteristics
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during camera init")
        }

    }

    override fun openCamera(cameraId: String, cameraOpenListener: CameraOpenListener<String, TextureView.SurfaceTextureListener>?) {
        this.currentCameraId = cameraId
        this.cameraOpenListener = cameraOpenListener
        backgroundHandler!!.post(Runnable {
            if (context == null || configurationProvider == null) {
                if (cameraOpenListener != null) {
                    uiHandler.post { cameraOpenListener.onCameraOpenError() }
                }
                return@Runnable
            }
            prepareCameraOutputs()
            try {
                manager!!.openCamera(currentCameraId!!, stateCallback, backgroundHandler)
            } catch (e: Exception) {
                if (cameraOpenListener != null) {
                    uiHandler.post { cameraOpenListener.onCameraOpenError() }
                }
            }
        })
    }

    override fun closeCamera(cameraCloseListener: CameraCloseListener<String>?) {
        backgroundHandler!!.post {
            closeCamera()
            if (cameraCloseListener != null) {
                uiHandler.post { cameraCloseListener.onCameraClosed(currentCameraId) }
            }
        }
    }

    override fun setFlashMode(@CameraConfiguration.FlashMode flashMode: Int) {
        setFlashModeAndBuildPreviewRequest(flashMode)
    }

    override fun takePhoto(photoFile: File, cameraPhotoListener: CameraPhotoListener) {
        this.outputPath = photoFile
        this.cameraPhotoListener = cameraPhotoListener

        backgroundHandler!!.post { lockFocus() }

    }

    override fun getPhotoSizeForQuality(@CameraConfiguration.MediaQuality mediaQuality: Int): Size {
        val map = if (currentCameraId == faceBackCameraId) backCameraStreamConfigurationMap else frontCameraStreamConfigurationMap
        return CameraHelper.getPictureSize(Size.fromArray2(map.getOutputSizes(ImageFormat.JPEG)), mediaQuality)
    }

    override fun startVideoRecord(videoFile: File, cameraVideoListener: CameraVideoListener?) {
        if (isVideoRecording || texture == null) return

        this.outputPath = videoFile
        this.cameraVideoListener = cameraVideoListener

        if (cameraVideoListener != null)
            backgroundHandler!!.post {
                closePreviewSession()
                if (prepareVideoRecorder()) {

                    val texture = currentInstance!!.texture
                    texture!!.setDefaultBufferSize(videoSize!!.width, videoSize!!.height)

                    try {
                        previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                        val surfaces = ArrayList<Surface>()

                        val previewSurface = workingSurface
                        surfaces.add(previewSurface)
                        previewRequestBuilder!!.addTarget(previewSurface!!)

                        workingSurface = videoRecorder!!.surface
                        surfaces.add(workingSurface)
                        previewRequestBuilder!!.addTarget(workingSurface!!)

                        cameraDevice!!.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                                captureSession = cameraCaptureSession

                                previewRequestBuilder!!.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                                try {
                                    captureSession!!.setRepeatingRequest(previewRequestBuilder!!.build(), null, backgroundHandler)
                                } catch (e: Exception) {
                                }

                                try {
                                    videoRecorder!!.start()
                                } catch (ignore: Exception) {
                                    Log.e(TAG, "videoRecorder.start(): ", ignore)
                                }

                                isVideoRecording = true

                                uiHandler.post { cameraVideoListener.onVideoRecordStarted(videoSize) }
                            }

                            override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                                Log.d(TAG, "onConfigureFailed")
                            }
                        }, backgroundHandler)
                    } catch (e: Exception) {
                        Log.e(TAG, "startVideoRecord: ", e)
                    }

                }
            }
    }

    override fun stopVideoRecord() {
        if (isVideoRecording)
            backgroundHandler!!.post {
                closePreviewSession()

                if (videoRecorder != null) {
                    try {
                        videoRecorder!!.stop()
                    } catch (ignore: Exception) {
                    }

                }
                isVideoRecording = false
                releaseVideoRecorder()

                if (cameraVideoListener != null) {
                    uiHandler.post { cameraVideoListener!!.onVideoRecordStopped(outputPath) }
                }
            }
    }

    private fun startPreview(texture: SurfaceTexture?) {
        try {
            if (texture == null) return

            this.texture = texture

            texture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)

            workingSurface = Surface(texture)

            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder!!.addTarget(workingSurface!!)

            cameraDevice!!.createCaptureSession(Arrays.asList<Surface>(workingSurface, imageReader!!.surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                            updatePreview(cameraCaptureSession)
                        }

                        override fun onConfigureFailed(
                                cameraCaptureSession: CameraCaptureSession) {
                            Log.d(TAG, "Fail while starting preview: ")
                        }
                    }, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error while preparing surface for preview: ", e)
        }

    }

    //--------------------Internal methods------------------

    override fun onMaxDurationReached() {
        stopVideoRecord()
    }

    override fun onMaxFileSizeReached() {
        stopVideoRecord()
    }

    override fun getPhotoOrientation(@CameraConfiguration.SensorPosition sensorPosition: Int): Int {
        return getVideoOrientation(sensorPosition)
    }

    override fun getVideoOrientation(@CameraConfiguration.SensorPosition sensorPosition: Int): Int {
        var degrees = 0
        when (sensorPosition) {
            CameraConfiguration.SENSOR_POSITION_UP -> degrees = 0
            CameraConfiguration.SENSOR_POSITION_LEFT -> degrees = 90
            CameraConfiguration.SENSOR_POSITION_UP_SIDE_DOWN -> degrees = 180
            CameraConfiguration.SENSOR_POSITION_RIGHT -> degrees = 270
            CameraConfiguration.SENSOR_POSITION_UNSPECIFIED -> {
            }
        }// Natural orientation
        // Landscape left
        // Upside down
        // Landscape right

        val rotate: Int
        if (currentCameraId == faceFrontCameraId) {
            rotate = (360 + faceFrontCameraOrientation + degrees) % 360
        } else {
            rotate = (360 + faceBackCameraOrientation - degrees) % 360
        }
        return rotate
    }

    private fun closeCamera() {
        closePreviewSession()
        releaseTexture()
        closeCameraDevice()
        closeImageReader()
        releaseVideoRecorder()
    }

    private fun releaseTexture() {
        if (null != texture) {
            texture!!.release()
            texture = null
        }
    }

    private fun closeImageReader() {
        if (null != imageReader) {
            imageReader!!.close()
            imageReader = null
        }
    }

    private fun closeCameraDevice() {
        if (cameraDevice != null) {
            cameraDevice!!.close()
            cameraDevice = null
        }
    }

    override fun prepareCameraOutputs() {
        try {
            val characteristics = if (currentCameraId == faceBackCameraId) backCameraCharacteristics else frontCameraCharacteristics

            if (currentCameraId == faceFrontCameraId && frontCameraStreamConfigurationMap == null)
                frontCameraStreamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            else if (currentCameraId == faceBackCameraId && backCameraStreamConfigurationMap == null)
                backCameraStreamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            val map = if (currentCameraId == faceBackCameraId) backCameraStreamConfigurationMap else frontCameraStreamConfigurationMap
            if (configurationProvider.mediaQuality == CameraConfiguration.MEDIA_QUALITY_AUTO) {
                camcorderProfile = CameraHelper.getCamcorderProfile(currentCameraId, configurationProvider.videoFileSize, configurationProvider.minimumVideoDuration)
            } else
                camcorderProfile = CameraHelper.getCamcorderProfile(configurationProvider.mediaQuality, currentCameraId)

            videoSize = CameraHelper.chooseOptimalSize(Size.fromArray2(map.getOutputSizes(MediaRecorder::class.java)),
                    windowSize!!.width, windowSize!!.height, Size(camcorderProfile!!.videoFrameWidth, camcorderProfile!!.videoFrameHeight))

            if (videoSize == null || videoSize!!.width > camcorderProfile!!.videoFrameWidth
                    || videoSize!!.height > camcorderProfile!!.videoFrameHeight)
                videoSize = CameraHelper.getSizeWithClosestRatio(Size.fromArray2(map.getOutputSizes(MediaRecorder::class.java)), camcorderProfile!!.videoFrameWidth, camcorderProfile!!.videoFrameHeight)
            else if (videoSize == null || videoSize!!.width > camcorderProfile!!.videoFrameWidth
                    || videoSize!!.height > camcorderProfile!!.videoFrameHeight)
                videoSize = CameraHelper.getSizeWithClosestRatio(Size.fromArray2(map.getOutputSizes(MediaRecorder::class.java)), camcorderProfile!!.videoFrameWidth, camcorderProfile!!.videoFrameHeight)

            photoSize = CameraHelper.getPictureSize(Size.fromArray2(map.getOutputSizes(ImageFormat.JPEG)),
                    if (configurationProvider.mediaQuality == CameraConfiguration.MEDIA_QUALITY_AUTO)
                        CameraConfiguration.MEDIA_QUALITY_HIGHEST
                    else
                        configurationProvider.mediaQuality)

            imageReader = ImageReader.newInstance(photoSize!!.width, photoSize!!.height,
                    ImageFormat.JPEG, 2)
            imageReader!!.setOnImageAvailableListener(this, backgroundHandler)

            if (configurationProvider.mediaAction == CameraConfiguration.MEDIA_ACTION_PHOTO || configurationProvider.mediaAction == CameraConfiguration.MEDIA_ACTION_BOTH) {

                if (windowSize!!.height * windowSize!!.width > photoSize!!.width * photoSize!!.height) {
                    previewSize = CameraHelper.getOptimalPreviewSize(Size.fromArray2(map.getOutputSizes(SurfaceTexture::class.java)), photoSize!!.width, photoSize!!.height)
                } else {
                    previewSize = CameraHelper.getOptimalPreviewSize(Size.fromArray2(map.getOutputSizes(SurfaceTexture::class.java)), windowSize!!.width, windowSize!!.height)
                }

                if (previewSize == null)
                    previewSize = CameraHelper.chooseOptimalSize(Size.fromArray2(map.getOutputSizes(SurfaceTexture::class.java)), windowSize!!.width, windowSize!!.height, photoSize)

            } else {
                if (windowSize!!.height * windowSize!!.width > videoSize!!.width * videoSize!!.height) {
                    previewSize = CameraHelper.getOptimalPreviewSize(Size.fromArray2(map.getOutputSizes(SurfaceTexture::class.java)), videoSize!!.width, videoSize!!.height)
                } else {
                    previewSize = CameraHelper.getOptimalPreviewSize(Size.fromArray2(map.getOutputSizes(SurfaceTexture::class.java)), windowSize!!.width, windowSize!!.height)
                }

                if (previewSize == null)
                    previewSize = CameraHelper.getSizeWithClosestRatio(Size.fromArray2(map.getOutputSizes(SurfaceTexture::class.java)), videoSize!!.width, videoSize!!.height)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while setup camera sizes.", e)
        }

    }

    override fun prepareVideoRecorder(): Boolean {
        videoRecorder = MediaRecorder()
        try {
            videoRecorder!!.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
            videoRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)

            videoRecorder!!.setOutputFormat(camcorderProfile!!.fileFormat)
            videoRecorder!!.setVideoFrameRate(camcorderProfile!!.videoFrameRate)
            videoRecorder!!.setVideoSize(videoSize!!.width, videoSize!!.height)
            videoRecorder!!.setVideoEncodingBitRate(camcorderProfile!!.videoBitRate)
            videoRecorder!!.setVideoEncoder(camcorderProfile!!.videoCodec)

            videoRecorder!!.setAudioEncodingBitRate(camcorderProfile!!.audioBitRate)
            videoRecorder!!.setAudioChannels(camcorderProfile!!.audioChannels)
            videoRecorder!!.setAudioSamplingRate(camcorderProfile!!.audioSampleRate)
            videoRecorder!!.setAudioEncoder(camcorderProfile!!.audioCodec)

            val outputFile = outputPath
            val outputFilePath = outputFile!!.toString()
            videoRecorder!!.setOutputFile(outputFilePath)

            if (configurationProvider.videoFileSize > 0) {
                videoRecorder!!.setMaxFileSize(configurationProvider.videoFileSize)
                videoRecorder!!.setOnInfoListener(this)
            }
            if (configurationProvider.videoDuration > 0) {
                videoRecorder!!.setMaxDuration(configurationProvider.videoDuration)
                videoRecorder!!.setOnInfoListener(this)
            }
            videoRecorder!!.setOrientationHint(getVideoOrientation(configurationProvider.sensorPosition))

            videoRecorder!!.prepare()

            return true
        } catch (error: IllegalStateException) {
            Log.e(TAG, "IllegalStateException preparing MediaRecorder: " + error.message)
        } catch (error: IOException) {
            Log.e(TAG, "IOException preparing MediaRecorder: " + error.message)
        } catch (error: Throwable) {
            Log.e(TAG, "Error during preparing MediaRecorder: " + error.message)
        }

        releaseVideoRecorder()
        return false
    }

    private fun updatePreview(cameraCaptureSession: CameraCaptureSession) {
        if (null == cameraDevice) {
            return
        }
        captureSession = cameraCaptureSession
        previewRequestBuilder!!.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        previewRequest = previewRequestBuilder!!.build()

        try {
            captureSession!!.setRepeatingRequest(previewRequest!!, captureCallback, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating preview: ", e)
        }

        setFlashModeAndBuildPreviewRequest(configurationProvider.flashMode)
    }

    private fun closePreviewSession() {
        if (captureSession != null) {
            captureSession!!.close()
            try {
                captureSession!!.abortCaptures()
            } catch (ignore: Exception) {
            } finally {
                captureSession = null
            }
        }
    }

    private fun lockFocus() {
        try {
            previewRequestBuilder!!.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)

            previewState = STATE_WAITING_LOCK
            captureSession!!.capture(previewRequestBuilder!!.build(), captureCallback, backgroundHandler)
        } catch (ignore: Exception) {
        }

    }

    private fun processCaptureResult(result: CaptureResult) {
        when (previewState) {
            STATE_PREVIEW -> {
            }
            STATE_WAITING_LOCK -> {
                val afState = result.get(CaptureResult.CONTROL_AF_STATE)
                if (afState == null) {
                    captureStillPicture()
                } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState
                        || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState
                        || CaptureResult.CONTROL_AF_STATE_INACTIVE == afState
                        || CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN == afState) {
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null || aeState === CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                        previewState = STATE_PICTURE_TAKEN
                        captureStillPicture()
                    } else {
                        runPreCaptureSequence()
                    }
                }
            }
            STATE_WAITING_PRE_CAPTURE -> {
                val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                if (aeState == null ||
                        aeState === CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState === CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                    previewState = STATE_WAITING_NON_PRE_CAPTURE
                }
            }
            STATE_WAITING_NON_PRE_CAPTURE -> {
                val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                if (aeState == null || aeState !== CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                    previewState = STATE_PICTURE_TAKEN
                    captureStillPicture()
                }
            }
            STATE_PICTURE_TAKEN -> {
            }
        }
    }

    private fun runPreCaptureSequence() {
        try {
            previewRequestBuilder!!.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START)
            previewState = STATE_WAITING_PRE_CAPTURE
            captureSession!!.capture(previewRequestBuilder!!.build(), captureCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
        }

    }

    private fun setFlashModeAndBuildPreviewRequest(@CameraConfiguration.FlashMode flashMode: Int) {
        try {

            when (flashMode) {
                CameraConfiguration.FLASH_MODE_AUTO -> {
                    previewRequestBuilder!!.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                    previewRequestBuilder!!.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE)
                }
                CameraConfiguration.FLASH_MODE_ON -> {
                    previewRequestBuilder!!.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                    previewRequestBuilder!!.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE)
                }
                CameraConfiguration.FLASH_MODE_OFF -> {
                    previewRequestBuilder!!.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                    previewRequestBuilder!!.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF)
                }
                else -> {
                    previewRequestBuilder!!.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                    previewRequestBuilder!!.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE)
                }
            }

            previewRequest = previewRequestBuilder!!.build()

            try {
                captureSession!!.setRepeatingRequest(previewRequest!!, captureCallback, backgroundHandler)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating preview: ", e)
            }

        } catch (ignore: Exception) {
            Log.e(TAG, "Error setting flash: ", ignore)
        }

    }

    private fun captureStillPicture() {
        try {
            if (null == cameraDevice) {
                return
            }
            val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(imageReader!!.surface)

            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getPhotoOrientation(configurationProvider.sensorPosition))

            val CaptureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession,
                                                request: CaptureRequest,
                                                result: TotalCaptureResult) {
                    Log.d(TAG, "onCaptureCompleted: ")
                }
            }

            captureSession!!.stopRepeating()
            captureSession!!.capture(captureBuilder.build(), CaptureCallback, null)

        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error during capturing picture")
        }

    }

    private fun unlockFocus() {
        try {
            previewRequestBuilder!!.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
            captureSession!!.capture(previewRequestBuilder!!.build(), captureCallback, backgroundHandler)
            previewState = STATE_PREVIEW
            captureSession!!.setRepeatingRequest(previewRequest!!, captureCallback, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Error during focus unlocking")
        }

    }

    override fun onImageAvailable(imageReader: ImageReader) {
        val outputFile = outputPath
        backgroundHandler!!.post(ImageSaver(imageReader.acquireNextImage(), outputFile, object : ImageSaver.ImageSaverCallback {
            override fun onSuccessFinish() {
                Log.d(TAG, "onPhotoSuccessFinish: ")
                if (cameraPhotoListener != null) {
                    uiHandler.post { cameraPhotoListener!!.onPhotoTaken(outputPath) }
                }
                unlockFocus()
            }

            override fun onError() {
                Log.d(TAG, "onPhotoError: ")
                uiHandler.post { cameraPhotoListener!!.onPhotoTakeError() }
            }
        }))
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture?, width: Int, height: Int) {
        if (surfaceTexture != null) startPreview(surfaceTexture)
    }

    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture?, width: Int, height: Int) {
        if (surfaceTexture != null) startPreview(surfaceTexture)
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        return true
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}

    @IntDef(STATE_PREVIEW.toLong(), STATE_WAITING_LOCK.toLong(), STATE_WAITING_PRE_CAPTURE.toLong(), STATE_WAITING_NON_PRE_CAPTURE.toLong(), STATE_PICTURE_TAKEN.toLong())
    @Retention(RetentionPolicy.SOURCE)
    internal annotation class CameraPreviewState

    companion object {

        private val TAG = "Camera2Manager"
        private val STATE_PREVIEW = 0
        private val STATE_WAITING_LOCK = 1
        private val STATE_WAITING_PRE_CAPTURE = 2
        private val STATE_WAITING_NON_PRE_CAPTURE = 3
        private val STATE_PICTURE_TAKEN = 4
        private var currentInstance: Camera2Manager? = null

        val instance: Camera2Manager
            get() {
                if (currentInstance == null) currentInstance = Camera2Manager()
                return currentInstance
            }
    }

}
