package com.sandrios.sandriosCamera.internal.controller.view

import android.app.Activity
import android.view.View

import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration
import com.sandrios.sandriosCamera.internal.utils.Size

/**
 * Created by Arpit Gandhi on 7/6/16.
 */
interface CameraView {

    val activity: Activity

    fun updateCameraPreview(size: Size, cameraPreview: View)

    fun updateUiForMediaAction(@CameraConfiguration.MediaAction mediaAction: Int)

    fun updateCameraSwitcher(numberOfCameras: Int)

    fun onPhotoTaken()

    fun onVideoRecordStart(width: Int, height: Int)

    fun onVideoRecordStop()

    fun releaseCameraPreview()

}
