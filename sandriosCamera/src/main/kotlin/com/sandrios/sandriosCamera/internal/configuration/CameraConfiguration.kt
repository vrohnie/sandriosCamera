package com.sandrios.sandriosCamera.internal.configuration

import android.support.annotation.IntDef

/**
 * Created by Arpit Gandhi on 7/6/16.
 */
object CameraConfiguration {

    const val MEDIA_QUALITY_AUTO = 10
    const val MEDIA_QUALITY_LOWEST = 15
    const val MEDIA_QUALITY_LOW = 11
    const val MEDIA_QUALITY_MEDIUM = 12
    const val MEDIA_QUALITY_HIGH = 13
    const val MEDIA_QUALITY_HIGHEST = 14

    const val MEDIA_ACTION_VIDEO = 100
    const val MEDIA_ACTION_PHOTO = 101
    const val MEDIA_ACTION_BOTH = 102

    const val CAMERA_FACE_FRONT = 0x6
    const val CAMERA_FACE_REAR = 0x7

    const val SENSOR_POSITION_UP = 90
    const val SENSOR_POSITION_UP_SIDE_DOWN = 270
    const val SENSOR_POSITION_LEFT = 0
    const val SENSOR_POSITION_RIGHT = 180
    const val SENSOR_POSITION_UNSPECIFIED = -1

    const val DISPLAY_ROTATION_0 = 0
    const val DISPLAY_ROTATION_90 = 90
    const val DISPLAY_ROTATION_180 = 180
    const val DISPLAY_ROTATION_270 = 270

    const val ORIENTATION_PORTRAIT = 0x111
    const val ORIENTATION_LANDSCAPE = 0x222

    const val FLASH_MODE_ON = 1
    const val FLASH_MODE_OFF = 2
    const val FLASH_MODE_AUTO = 3

    interface Arguments {
        companion object {
            val REQUEST_CODE = "com.sandrios.sandriosCamera.request_code"
            val MEDIA_ACTION = "com.sandrios.sandriosCamera.media_action"
            val MEDIA_QUALITY = "com.sandrios.sandriosCamera.camera_media_quality"
            val VIDEO_DURATION = "com.sandrios.sandriosCamera.video_duration"
            val MINIMUM_VIDEO_DURATION = "com.sandrios.sandriosCamera.minimum.video_duration"
            val VIDEO_FILE_SIZE = "com.sandrios.sandriosCamera.camera_video_file_size"
            val FILE_PATH = "com.sandrios.sandriosCamera.camera_video_file_path"
            val FLASH_MODE = "com.sandrios.sandriosCamera.camera_flash_mode"
            val SHOW_PICKER = "com.sandrios.sandriosCamera.show_picker"
            val ENABLE_CROP = "com.sandrios.sandriosCamera.enable_crop"
        }
    }

    @IntDef(MEDIA_QUALITY_AUTO.toLong(), MEDIA_QUALITY_LOWEST.toLong(), MEDIA_QUALITY_LOW.toLong(), MEDIA_QUALITY_MEDIUM.toLong(), MEDIA_QUALITY_HIGH.toLong(), MEDIA_QUALITY_HIGHEST.toLong())
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class MediaQuality

    @IntDef(MEDIA_ACTION_VIDEO.toLong(), MEDIA_ACTION_PHOTO.toLong(), MEDIA_ACTION_BOTH.toLong())
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class MediaAction

    @IntDef(CAMERA_FACE_FRONT.toLong(), CAMERA_FACE_REAR.toLong())
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class CameraFace

    @IntDef(SENSOR_POSITION_UP.toLong(), SENSOR_POSITION_UP_SIDE_DOWN.toLong(), SENSOR_POSITION_LEFT.toLong(), SENSOR_POSITION_RIGHT.toLong(), SENSOR_POSITION_UNSPECIFIED.toLong())
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class SensorPosition

    @IntDef(DISPLAY_ROTATION_0.toLong(), DISPLAY_ROTATION_90.toLong(), DISPLAY_ROTATION_180.toLong(), DISPLAY_ROTATION_270.toLong())
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class DisplayRotation

    @IntDef(ORIENTATION_PORTRAIT.toLong(), ORIENTATION_LANDSCAPE.toLong())
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class DeviceDefaultOrientation

    @IntDef(FLASH_MODE_ON.toLong(), FLASH_MODE_OFF.toLong(), FLASH_MODE_AUTO.toLong())
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class FlashMode
}
