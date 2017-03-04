package com.sandrios.sandriosCamera.internal.controller

import android.os.Bundle

import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration
import com.sandrios.sandriosCamera.internal.manager.CameraManager

import java.io.File

/**
 * Created by Arpit Gandhi on 7/6/16.
 */
interface CameraController<CameraId> {

    fun onCreate(savedInstanceState: Bundle)

    fun onResume()

    fun onPause()

    fun onDestroy()

    fun takePhoto()

    fun startVideoRecord()

    fun stopVideoRecord()

    val isVideoRecording: Boolean

    fun switchCamera(@CameraConfiguration.CameraFace cameraFace: Int)

    fun switchQuality()

    val numberOfCameras: Int

    val mediaAction: Int

    val currentCameraId: CameraId

    val outputFile: File

    val cameraManager: CameraManager<*, *>

    fun setFlashMode(@CameraConfiguration.FlashMode flashMode: Int)

}
