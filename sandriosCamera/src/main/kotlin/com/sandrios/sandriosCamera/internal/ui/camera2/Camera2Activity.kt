package com.sandrios.sandriosCamera.internal.ui.camera2

import android.annotation.TargetApi
import android.media.CamcorderProfile
import android.os.Build

import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration
import com.sandrios.sandriosCamera.internal.configuration.ConfigurationProvider
import com.sandrios.sandriosCamera.internal.controller.CameraController
import com.sandrios.sandriosCamera.internal.controller.impl.Camera2Controller
import com.sandrios.sandriosCamera.internal.controller.impl.Camera2ControllerAPI24
import com.sandrios.sandriosCamera.internal.controller.view.CameraView
import com.sandrios.sandriosCamera.internal.ui.BaseSandriosActivity
import com.sandrios.sandriosCamera.internal.ui.model.PhotoQualityOption
import com.sandrios.sandriosCamera.internal.ui.model.VideoQualityOption
import com.sandrios.sandriosCamera.internal.utils.CameraHelper

import java.util.ArrayList

/**
 * Created by Arpit Gandhi on 7/6/16.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2Activity : BaseSandriosActivity<String>() {

    override fun createCameraController(cameraView: CameraView, configurationProvider: ConfigurationProvider): CameraController<String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            return Camera2ControllerAPI24(cameraView, configurationProvider)
        else
            return Camera2Controller(cameraView, configurationProvider)
    }

    protected override val videoQualityOptions: Array<CharSequence>
        get() {
            val videoQualities = ArrayList<CharSequence>()

            if (getMinimumVideoDuration() > 0)
                videoQualities.add(VideoQualityOption(CameraConfiguration.MEDIA_QUALITY_AUTO, CameraHelper.getCamcorderProfile(CameraConfiguration.MEDIA_QUALITY_AUTO, cameraController.currentCameraId), getMinimumVideoDuration().toDouble()))


            var camcorderProfile: CamcorderProfile = CameraHelper.getCamcorderProfile(CameraConfiguration.MEDIA_QUALITY_HIGH, cameraController.currentCameraId)
            var videoDuration = CameraHelper.calculateApproximateVideoDuration(camcorderProfile, videoFileSize)
            videoQualities.add(VideoQualityOption(CameraConfiguration.MEDIA_QUALITY_HIGH, camcorderProfile, videoDuration))

            camcorderProfile = CameraHelper.getCamcorderProfile(CameraConfiguration.MEDIA_QUALITY_MEDIUM, cameraController.currentCameraId)
            videoDuration = CameraHelper.calculateApproximateVideoDuration(camcorderProfile, videoFileSize)
            videoQualities.add(VideoQualityOption(CameraConfiguration.MEDIA_QUALITY_MEDIUM, camcorderProfile, videoDuration))

            camcorderProfile = CameraHelper.getCamcorderProfile(CameraConfiguration.MEDIA_QUALITY_LOW, cameraController.currentCameraId)
            videoDuration = CameraHelper.calculateApproximateVideoDuration(camcorderProfile, videoFileSize)
            videoQualities.add(VideoQualityOption(CameraConfiguration.MEDIA_QUALITY_LOW, camcorderProfile, videoDuration))

            val array = arrayOfNulls<CharSequence>(videoQualities.size)
            videoQualities.toTypedArray()

            return array
        }

    protected override val photoQualityOptions: Array<CharSequence>
        get() {
            val photoQualities = ArrayList<CharSequence>()
            photoQualities.add(PhotoQualityOption(CameraConfiguration.MEDIA_QUALITY_HIGHEST, cameraController.cameraManager.getPhotoSizeForQuality(CameraConfiguration.MEDIA_QUALITY_HIGHEST)))
            photoQualities.add(PhotoQualityOption(CameraConfiguration.MEDIA_QUALITY_HIGH, cameraController.cameraManager.getPhotoSizeForQuality(CameraConfiguration.MEDIA_QUALITY_HIGH)))
            photoQualities.add(PhotoQualityOption(CameraConfiguration.MEDIA_QUALITY_MEDIUM, cameraController.cameraManager.getPhotoSizeForQuality(CameraConfiguration.MEDIA_QUALITY_MEDIUM)))
            photoQualities.add(PhotoQualityOption(CameraConfiguration.MEDIA_QUALITY_LOWEST, cameraController.cameraManager.getPhotoSizeForQuality(CameraConfiguration.MEDIA_QUALITY_LOWEST)))

            val array = arrayOfNulls<CharSequence>(photoQualities.size)
            photoQualities.toTypedArray()

            return array
        }

}
