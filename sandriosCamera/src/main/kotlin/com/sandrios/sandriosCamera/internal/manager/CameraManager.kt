package com.sandrios.sandriosCamera.internal.manager

import android.content.Context

import com.sandrios.sandriosCamera.internal.configuration.ConfigurationProvider
import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration
import com.sandrios.sandriosCamera.internal.manager.listener.CameraCloseListener
import com.sandrios.sandriosCamera.internal.manager.listener.CameraOpenListener
import com.sandrios.sandriosCamera.internal.manager.listener.CameraPhotoListener
import com.sandrios.sandriosCamera.internal.manager.listener.CameraVideoListener
import com.sandrios.sandriosCamera.internal.utils.Size

import java.io.File

/**
 * Created by Arpit Gandhi on 8/14/16.
 */
interface CameraManager<CameraId, SurfaceListener> {

    fun initializeCameraManager(configurationProvider: ConfigurationProvider, context: Context)

    fun openCamera(cameraId: CameraId, cameraOpenListener: CameraOpenListener<CameraId, SurfaceListener>)

    fun closeCamera(cameraCloseListener: CameraCloseListener<CameraId>)

    fun takePhoto(photoFile: File, cameraPhotoListener: CameraPhotoListener)

    fun startVideoRecord(videoFile: File, cameraVideoListener: CameraVideoListener)

    fun getPhotoSizeForQuality(@CameraConfiguration.MediaQuality mediaQuality: Int): Size

    fun setFlashMode(@CameraConfiguration.FlashMode flashMode: Int)

    fun stopVideoRecord()

    fun releaseCameraManager()

    val currentCameraId: CameraId

    val faceFrontCameraId: CameraId

    val faceBackCameraId: CameraId

    val numberOfCameras: Int

    val faceFrontCameraOrientation: Int

    val faceBackCameraOrientation: Int

    val isVideoRecording: Boolean
}
