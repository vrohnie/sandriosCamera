package com.sandrios.sandriosCamera.internal.manager.impl

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.Camera
import android.media.ExifInterface
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.WindowManager

import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration
import com.sandrios.sandriosCamera.internal.configuration.ConfigurationProvider
import com.sandrios.sandriosCamera.internal.manager.listener.CameraCloseListener
import com.sandrios.sandriosCamera.internal.manager.listener.CameraOpenListener
import com.sandrios.sandriosCamera.internal.manager.listener.CameraPhotoListener
import com.sandrios.sandriosCamera.internal.manager.listener.CameraVideoListener
import com.sandrios.sandriosCamera.internal.utils.CameraHelper
import com.sandrios.sandriosCamera.internal.utils.Size

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * Created by Arpit Gandhi on 8/14/16.
 */
class Camera1Manager private constructor() : BaseCameraManager<Int, SurfaceHolder.Callback>(), SurfaceHolder.Callback, Camera.PictureCallback {
    private var camera: Camera? = null
    private var surface: Surface? = null
    private var orientation: Int = 0
    private var displayRotation = 0

    private var outputPath: File? = null
    private var videoListener: CameraVideoListener? = null
    private var photoListener: CameraPhotoListener? = null

    override fun openCamera(cameraId: Int?,
                            cameraOpenListener: CameraOpenListener<Int, SurfaceHolder.Callback>?) {
        this.currentCameraId = cameraId
        backgroundHandler!!.post {
            try {
                camera = Camera.open(cameraId!!)
                prepareCameraOutputs()
                if (cameraOpenListener != null) {
                    uiHandler.post { cameraOpenListener.onCameraOpened(cameraId, previewSize, currentInstance) }
                }
            } catch (error: Exception) {
                Log.d(TAG, "Can't open camera: " + error.message)
                if (cameraOpenListener != null) {
                    uiHandler.post { cameraOpenListener.onCameraOpenError() }
                }
            }
        }
    }

    override fun closeCamera(cameraCloseListener: CameraCloseListener<Int>?) {
        backgroundHandler!!.post {
            if (camera != null) {
                camera!!.release()
                camera = null
                if (cameraCloseListener != null) {
                    uiHandler.post { cameraCloseListener.onCameraClosed(currentCameraId) }
                }
            }
        }
    }

    override fun takePhoto(photoFile: File, cameraPhotoListener: CameraPhotoListener) {
        this.outputPath = photoFile
        this.photoListener = cameraPhotoListener
        backgroundHandler!!.post {
            setCameraPhotoQuality(camera)
            camera!!.takePicture(null, null, currentInstance)
        }
    }

    override fun startVideoRecord(videoFile: File, cameraVideoListener: CameraVideoListener) {
        if (isVideoRecording) return

        this.outputPath = videoFile
        this.videoListener = cameraVideoListener

        if (videoListener != null)
            backgroundHandler!!.post {
                if (prepareVideoRecorder()) {
                    videoRecorder!!.start()
                    isVideoRecording = true
                    uiHandler.post { videoListener!!.onVideoRecordStarted(videoSize) }
                }
            }
    }

    override fun stopVideoRecord() {
        if (isVideoRecording)
            backgroundHandler!!.post {
                try {
                    if (videoRecorder != null) videoRecorder!!.stop()
                } catch (ignore: Exception) {
                    // ignore illegal state.
                    // appear in case time or file size reach limit and stop already called.
                }

                isVideoRecording = false
                releaseVideoRecorder()

                if (videoListener != null) {
                    uiHandler.post { videoListener!!.onVideoRecordStopped(outputPath) }
                }
            }
    }

    override fun releaseCameraManager() {
        super.releaseCameraManager()
    }

    override fun initializeCameraManager(configurationProvider: ConfigurationProvider, context: Context) {
        super.initializeCameraManager(configurationProvider, context)

        numberOfCameras = Camera.getNumberOfCameras()

        for (i in 0..numberOfCameras - 1) {
            val cameraInfo = Camera.CameraInfo()

            Camera.getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                faceBackCameraId = i
                faceBackCameraOrientation = cameraInfo.orientation
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                faceFrontCameraId = i
                faceFrontCameraOrientation = cameraInfo.orientation
            }
        }
    }

    override fun getPhotoSizeForQuality(@CameraConfiguration.MediaQuality mediaQuality: Int): Size {
        return CameraHelper.getPictureSize(Size.fromList(camera!!.parameters.supportedPictureSizes), mediaQuality)
    }

    override fun setFlashMode(@CameraConfiguration.FlashMode flashMode: Int) {
        setFlashMode(camera, camera!!.parameters, flashMode)
    }

    override fun prepareCameraOutputs() {
        try {
            if (configurationProvider.mediaQuality == CameraConfiguration.MEDIA_QUALITY_AUTO) {
                camcorderProfile = CameraHelper.getCamcorderProfile(currentCameraId!!, configurationProvider.videoFileSize, configurationProvider.minimumVideoDuration)
            } else
                camcorderProfile = CameraHelper.getCamcorderProfile(configurationProvider.mediaQuality, currentCameraId!!)

            val previewSizes = Size.fromList(camera!!.parameters.supportedPreviewSizes)
            val pictureSizes = Size.fromList(camera!!.parameters.supportedPictureSizes)
            val videoSizes: List<Size>?
            if (Build.VERSION.SDK_INT > 10)
                videoSizes = Size.fromList(camera!!.parameters.supportedVideoSizes)
            else
                videoSizes = previewSizes

            videoSize = CameraHelper.getSizeWithClosestRatio(
                    if (videoSizes == null || videoSizes.isEmpty()) previewSizes else videoSizes,
                    camcorderProfile!!.videoFrameWidth, camcorderProfile!!.videoFrameHeight)

            photoSize = CameraHelper.getPictureSize(
                    if (pictureSizes == null || pictureSizes.isEmpty()) previewSizes else pictureSizes,
                    if (configurationProvider.mediaQuality == CameraConfiguration.MEDIA_QUALITY_AUTO)
                        CameraConfiguration.MEDIA_QUALITY_HIGHEST
                    else
                        configurationProvider.mediaQuality)

            if (configurationProvider.mediaAction == CameraConfiguration.MEDIA_ACTION_PHOTO || configurationProvider.mediaAction == CameraConfiguration.MEDIA_ACTION_BOTH) {
                previewSize = CameraHelper.getSizeWithClosestRatio(previewSizes, photoSize!!.width, photoSize!!.height)
            } else {
                previewSize = CameraHelper.getSizeWithClosestRatio(previewSizes, videoSize!!.width, videoSize!!.height)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while setup camera sizes.")
        }

    }

    override fun prepareVideoRecorder(): Boolean {
        videoRecorder = MediaRecorder()
        try {
            camera!!.lock()
            camera!!.unlock()
            videoRecorder!!.setCamera(camera)

            videoRecorder!!.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
            videoRecorder!!.setVideoSource(MediaRecorder.VideoSource.DEFAULT)

            videoRecorder!!.setOutputFormat(camcorderProfile!!.fileFormat)
            videoRecorder!!.setVideoFrameRate(camcorderProfile!!.videoFrameRate)
            videoRecorder!!.setVideoSize(videoSize!!.width, videoSize!!.height)
            videoRecorder!!.setVideoEncodingBitRate(camcorderProfile!!.videoBitRate)
            videoRecorder!!.setVideoEncoder(camcorderProfile!!.videoCodec)

            videoRecorder!!.setAudioEncodingBitRate(camcorderProfile!!.audioBitRate)
            videoRecorder!!.setAudioChannels(camcorderProfile!!.audioChannels)
            videoRecorder!!.setAudioSamplingRate(camcorderProfile!!.audioSampleRate)
            videoRecorder!!.setAudioEncoder(camcorderProfile!!.audioCodec)

            videoRecorder!!.setOutputFile(outputPath!!.toString())

            if (configurationProvider.videoFileSize > 0) {
                videoRecorder!!.setMaxFileSize(configurationProvider.videoFileSize)

                videoRecorder!!.setOnInfoListener(this)
            }
            if (configurationProvider.videoDuration > 0) {
                videoRecorder!!.setMaxDuration(configurationProvider.videoDuration)

                videoRecorder!!.setOnInfoListener(this)
            }

            videoRecorder!!.setOrientationHint(getVideoOrientation(configurationProvider.sensorPosition))
            videoRecorder!!.setPreviewDisplay(surface)

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

    override fun onMaxDurationReached() {
        stopVideoRecord()
    }

    override fun onMaxFileSizeReached() {
        stopVideoRecord()
    }

    override fun releaseVideoRecorder() {
        super.releaseVideoRecorder()

        try {
            camera!!.lock() // lock camera for later use
        } catch (ignore: Exception) {
        }

    }

    //------------------------Implementation------------------

    private fun startPreview(surfaceHolder: SurfaceHolder) {
        try {
            val cameraInfo = Camera.CameraInfo()
            Camera.getCameraInfo(currentCameraId!!, cameraInfo)
            val cameraRotationOffset = cameraInfo.orientation

            val parameters = camera!!.parameters
            setAutoFocus(camera, parameters)
            setFlashMode(configurationProvider.flashMode)

            if (configurationProvider.mediaAction == CameraConfiguration.MEDIA_ACTION_PHOTO || configurationProvider.mediaAction == CameraConfiguration.MEDIA_ACTION_BOTH)
                turnPhotoCameraFeaturesOn(camera, parameters)
            else if (configurationProvider.mediaAction == CameraConfiguration.MEDIA_ACTION_PHOTO)
                turnVideoCameraFeaturesOn(camera, parameters)

            val rotation = (context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
            var degrees = 0
            when (rotation) {
                Surface.ROTATION_0 -> degrees = 0
                Surface.ROTATION_90 -> degrees = 90
                Surface.ROTATION_180 -> degrees = 180
                Surface.ROTATION_270 -> degrees = 270
            }// Natural orientation
            // Landscape left
            // Upside down
            // Landscape right

            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                displayRotation = (cameraRotationOffset + degrees) % 360
                displayRotation = (360 - displayRotation) % 360 // compensate
            } else {
                displayRotation = (cameraRotationOffset - degrees + 360) % 360
            }

            this.camera!!.setDisplayOrientation(displayRotation)

            if (Build.VERSION.SDK_INT > 13 && (configurationProvider.mediaAction == CameraConfiguration.MEDIA_ACTION_VIDEO || configurationProvider.mediaAction == CameraConfiguration.MEDIA_ACTION_BOTH)) {
                //                parameters.setRecordingHint(true);
            }

            if (Build.VERSION.SDK_INT > 14
                    && parameters.isVideoStabilizationSupported
                    && (configurationProvider.mediaAction == CameraConfiguration.MEDIA_ACTION_VIDEO || configurationProvider.mediaAction == CameraConfiguration.MEDIA_ACTION_BOTH)) {
                parameters.videoStabilization = true
            }

            parameters.setPreviewSize(previewSize!!.width, previewSize!!.height)
            parameters.setPictureSize(photoSize!!.width, photoSize!!.height)

            camera!!.parameters = parameters
            camera!!.setPreviewDisplay(surfaceHolder)
            camera!!.startPreview()

        } catch (error: IOException) {
            Log.d(TAG, "Error setting camera preview: " + error.message)
        } catch (ignore: Exception) {
            Log.d(TAG, "Error starting camera preview: " + ignore.message)
        }

    }

    private fun turnPhotoCameraFeaturesOn(camera: Camera, parameters: Camera.Parameters) {
        if (parameters.supportedFocusModes.contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        }
        camera.parameters = parameters
    }

    private fun turnVideoCameraFeaturesOn(camera: Camera, parameters: Camera.Parameters) {
        if (parameters.supportedFocusModes.contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
        }
        camera.parameters = parameters
    }

    private fun setAutoFocus(camera: Camera, parameters: Camera.Parameters) {
        try {
            if (parameters.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
                camera.parameters = parameters
            }
        } catch (ignore: Exception) {
        }

    }

    private fun setFlashMode(camera: Camera, parameters: Camera.Parameters, @CameraConfiguration.FlashMode flashMode: Int) {
        try {
            when (flashMode) {
                CameraConfiguration.FLASH_MODE_AUTO -> parameters.flashMode = Camera.Parameters.FLASH_MODE_AUTO
                CameraConfiguration.FLASH_MODE_ON -> parameters.flashMode = Camera.Parameters.FLASH_MODE_ON
                CameraConfiguration.FLASH_MODE_OFF -> parameters.flashMode = Camera.Parameters.FLASH_MODE_OFF
                else -> parameters.flashMode = Camera.Parameters.FLASH_MODE_AUTO
            }
            camera.parameters = parameters
        } catch (ignore: Exception) {
        }

    }


    private fun setCameraPhotoQuality(camera: Camera) {
        val parameters = camera.parameters

        parameters.pictureFormat = PixelFormat.JPEG

        if (configurationProvider.mediaQuality == CameraConfiguration.MEDIA_QUALITY_LOW) {
            parameters.jpegQuality = 50
        } else if (configurationProvider.mediaQuality == CameraConfiguration.MEDIA_QUALITY_MEDIUM) {
            parameters.jpegQuality = 75
        } else if (configurationProvider.mediaQuality == CameraConfiguration.MEDIA_QUALITY_HIGH) {
            parameters.jpegQuality = 100
        } else if (configurationProvider.mediaQuality == CameraConfiguration.MEDIA_QUALITY_HIGHEST) {
            parameters.jpegQuality = 100
        }
        parameters.setPictureSize(photoSize!!.width, photoSize!!.height)

        camera.parameters = parameters
    }

    override fun getPhotoOrientation(@CameraConfiguration.SensorPosition sensorPosition: Int): Int {
        val rotate: Int
        if (currentCameraId == faceFrontCameraId) {
            rotate = (360 + faceFrontCameraOrientation + configurationProvider.degrees) % 360
        } else {
            rotate = (360 + faceBackCameraOrientation - configurationProvider.degrees) % 360
        }

        if (rotate == 0) {
            orientation = ExifInterface.ORIENTATION_NORMAL
        } else if (rotate == 90) {
            orientation = ExifInterface.ORIENTATION_ROTATE_90
        } else if (rotate == 180) {
            orientation = ExifInterface.ORIENTATION_ROTATE_180
        } else if (rotate == 270) {
            orientation = ExifInterface.ORIENTATION_ROTATE_270
        }

        return orientation
    }

    override fun getVideoOrientation(@CameraConfiguration.SensorPosition sensorPosition: Int): Int {
        var degrees = 0
        when (sensorPosition) {
            CameraConfiguration.SENSOR_POSITION_UP -> degrees = 0
            CameraConfiguration.SENSOR_POSITION_LEFT -> degrees = 90
            CameraConfiguration.SENSOR_POSITION_UP_SIDE_DOWN -> degrees = 180
            CameraConfiguration.SENSOR_POSITION_RIGHT -> degrees = 270
            CameraConfiguration.SENSOR_POSITION_UNSPECIFIED -> degrees = 0
        }// Natural orientation
        // Landscape left
        // Upside down
        // Landscape right
        // Natural orientation

        val rotate: Int
        if (currentCameraId == faceFrontCameraId) {
            rotate = (360 + faceFrontCameraOrientation + degrees) % 360
        } else {
            rotate = (360 + faceBackCameraOrientation - degrees) % 360
        }
        return rotate
    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        if (surfaceHolder.surface == null) {
            return
        }

        surface = surfaceHolder.surface

        try {
            camera!!.stopPreview()
        } catch (ignore: Exception) {
        }

        startPreview(surfaceHolder)
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (surfaceHolder.surface == null) {
            return
        }

        surface = surfaceHolder.surface

        try {
            camera!!.stopPreview()
        } catch (ignore: Exception) {
        }

        startPreview(surfaceHolder)
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {

    }

    override fun onPictureTaken(bytes: ByteArray, camera: Camera) {
        val pictureFile = outputPath
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions.")
            return
        }

        try {
            val fileOutputStream = FileOutputStream(pictureFile)
            fileOutputStream.write(bytes)
            fileOutputStream.close()
        } catch (error: FileNotFoundException) {
            Log.e(TAG, "File not found: " + error.message)
        } catch (error: IOException) {
            Log.e(TAG, "Error accessing file: " + error.message)
        } catch (error: Throwable) {
            Log.e(TAG, "Error saving file: " + error.message)
        }

        try {
            val exif = ExifInterface(pictureFile.absolutePath)
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, "" + getPhotoOrientation(configurationProvider.sensorPosition))
            exif.saveAttributes()

            if (photoListener != null) {
                uiHandler.post { photoListener!!.onPhotoTaken(outputPath) }
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Can't save exif info: " + error.message)
        }

    }

    companion object {

        private val TAG = "Camera1Manager"
        private var currentInstance: Camera1Manager? = null

        val instance: Camera1Manager
            get() {
                if (currentInstance == null) currentInstance = Camera1Manager()
                return currentInstance
            }
    }
}
