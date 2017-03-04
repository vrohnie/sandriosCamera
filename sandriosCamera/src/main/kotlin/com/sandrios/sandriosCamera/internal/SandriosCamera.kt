package com.sandrios.sandriosCamera.internal

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.support.annotation.IntRange

import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration
import com.sandrios.sandriosCamera.internal.ui.camera.Camera1Activity
import com.sandrios.sandriosCamera.internal.ui.camera2.Camera2Activity
import com.sandrios.sandriosCamera.internal.utils.CameraHelper

import java.util.ArrayList

/**
 * Sandrios Camera Builder Class
 * Created by Arpit Gandhi on 7/6/16.
 */
class SandriosCamera
/***
 * Creates SandriosCamera instance with default configuration set to both.

 * @param activity - fromList which request was invoked
 * *
 * @param code     - request code which will return in onActivityForResult
 */
(private val mActivity: Activity, @IntRange(from = 0) private val requestCode: Int) {

    private var mInstance: SandriosCamera? = null
    private var mediaAction = CameraConfiguration.MEDIA_ACTION_BOTH
    private var showPicker = true
    private var enableImageCrop = false
    private var videoSize: Long = -1

    init {
        mInstance = this
    }

    fun setShowPicker(showPicker: Boolean): SandriosCamera {
        this.showPicker = showPicker
        return mInstance as SandriosCamera
    }

    fun setMediaAction(mediaAction: Int): SandriosCamera {
        this.mediaAction = mediaAction
        return mInstance as SandriosCamera
    }

    fun enableImageCropping(enableImageCrop: Boolean): SandriosCamera {
        this.enableImageCrop = enableImageCrop
        return mInstance as SandriosCamera
    }

    fun setVideoFileSize(fileSize: Int): SandriosCamera {
        this.videoSize = fileSize.toLong()
        return mInstance as SandriosCamera
    }

    fun launchCamera() {
        TedPermission(mActivity)
                .setPermissionListener(object : PermissionListener {
                    override fun onPermissionGranted() {
                        launchIntent()
                    }

                    override fun onPermissionDenied(deniedPermissions: ArrayList<String>) {

                    }
                })
                .setPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO)
                .check()
    }

    private fun launchIntent() {
        if (CameraHelper.hasCamera(mActivity)) {
            val cameraIntent: Intent
            if (CameraHelper.hasCamera2(mActivity)) {
                cameraIntent = Intent(mActivity, Camera2Activity::class.java)
            } else {
                cameraIntent = Intent(mActivity, Camera1Activity::class.java)
            }
            cameraIntent.putExtra(CameraConfiguration.Arguments.REQUEST_CODE, requestCode)
            cameraIntent.putExtra(CameraConfiguration.Arguments.SHOW_PICKER, showPicker)
            cameraIntent.putExtra(CameraConfiguration.Arguments.MEDIA_ACTION, mediaAction)
            cameraIntent.putExtra(CameraConfiguration.Arguments.ENABLE_CROP, enableImageCrop)
            if (videoSize > 0) {
                cameraIntent.putExtra(CameraConfiguration.Arguments.VIDEO_FILE_SIZE, videoSize * 1024 * 1024)
            }
            mActivity.startActivityForResult(cameraIntent, requestCode)
        }
    }
}
