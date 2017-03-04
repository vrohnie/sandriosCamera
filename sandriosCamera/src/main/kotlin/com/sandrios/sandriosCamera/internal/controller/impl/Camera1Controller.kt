package com.sandrios.sandriosCamera.internal.controller.impl

import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder

import com.sandrios.sandriosCamera.internal.configuration.ConfigurationProvider
import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration
import com.sandrios.sandriosCamera.internal.controller.CameraController
import com.sandrios.sandriosCamera.internal.controller.view.CameraView
import com.sandrios.sandriosCamera.internal.manager.CameraManager
import com.sandrios.sandriosCamera.internal.manager.impl.Camera1Manager
import com.sandrios.sandriosCamera.internal.manager.listener.CameraCloseListener
import com.sandrios.sandriosCamera.internal.manager.listener.CameraOpenListener
import com.sandrios.sandriosCamera.internal.manager.listener.CameraPhotoListener
import com.sandrios.sandriosCamera.internal.manager.listener.CameraVideoListener
import com.sandrios.sandriosCamera.internal.ui.view.AutoFitSurfaceView
import com.sandrios.sandriosCamera.internal.utils.CameraHelper
import com.sandrios.sandriosCamera.internal.utils.Size

import java.io.File

/**
 * Created by Arpit Gandhi on 7/7/16.
 */

class Camera1Controller(private val cameraView: CameraView, private val configurationProvider: ConfigurationProvider) : CameraController<Int>, CameraOpenListener<Int, SurfaceHolder.Callback>, CameraPhotoListener, CameraCloseListener<Int>, CameraVideoListener {

    override var currentCameraId: Int? = null
        private set
    private var cameraManager: CameraManager<Int, SurfaceHolder.Callback>? = null

    override var outputFile: File? = null
        private set

    override fun onCreate(savedInstanceState: Bundle) {
        cameraManager = Camera1Manager.instance
        cameraManager!!.initializeCameraManager(configurationProvider, cameraView.activity)
        currentCameraId = cameraManager!!.faceBackCameraId
    }

    override fun onResume() {
        cameraManager!!.openCamera(currentCameraId, this)
    }

    override fun onPause() {
        cameraManager!!.closeCamera(null)
    }

    override fun onDestroy() {
        cameraManager!!.releaseCameraManager()
    }

    override fun takePhoto() {
        outputFile = CameraHelper.getOutputMediaFile(cameraView.activity, CameraConfiguration.MEDIA_ACTION_PHOTO)
        cameraManager!!.takePhoto(outputFile, this)
    }

    override fun startVideoRecord() {
        outputFile = CameraHelper.getOutputMediaFile(cameraView.activity, CameraConfiguration.MEDIA_ACTION_VIDEO)
        cameraManager!!.startVideoRecord(outputFile, this)
    }

    override fun setFlashMode(@CameraConfiguration.FlashMode flashMode: Int) {
        cameraManager!!.setFlashMode(flashMode)
    }

    override fun stopVideoRecord() {
        cameraManager!!.stopVideoRecord()
    }

    override val isVideoRecording: Boolean
        get() = cameraManager!!.isVideoRecording

    override fun switchCamera(@CameraConfiguration.CameraFace cameraFace: Int) {
        currentCameraId = if (cameraManager!!.currentCameraId == cameraManager!!.faceFrontCameraId)
            cameraManager!!.faceBackCameraId
        else
            cameraManager!!.faceFrontCameraId

        cameraManager!!.closeCamera(this)
    }

    override fun switchQuality() {
        cameraManager!!.closeCamera(this)
    }

    override val numberOfCameras: Int
        get() = cameraManager!!.numberOfCameras

    override val mediaAction: Int
        get() = configurationProvider.mediaAction


    override fun onCameraOpened(cameraId: Int?, previewSize: Size, surfaceCallback: SurfaceHolder.Callback) {
        cameraView.updateUiForMediaAction(configurationProvider.mediaAction)
        cameraView.updateCameraPreview(previewSize, AutoFitSurfaceView(cameraView.activity, surfaceCallback))
        cameraView.updateCameraSwitcher(numberOfCameras)
    }

    override fun onCameraOpenError() {
        Log.e(TAG, "onCameraOpenError")
    }

    override fun onCameraClosed(closedCameraId: Int?) {
        cameraView.releaseCameraPreview()

        cameraManager!!.openCamera(currentCameraId, this)
    }

    override fun onPhotoTaken(photoFile: File) {
        cameraView.onPhotoTaken()
    }

    override fun onPhotoTakeError() {}

    override fun onVideoRecordStarted(videoSize: Size) {
        cameraView.onVideoRecordStart(videoSize.width, videoSize.height)
    }

    override fun onVideoRecordStopped(videoFile: File) {
        cameraView.onVideoRecordStop()
    }

    override fun onVideoRecordError() {

    }

    override fun getCameraManager(): CameraManager<*, *> {
        return cameraManager
    }

    companion object {

        private val TAG = "Camera1Controller"
    }
}
