package com.sandrios.sandriosCamera.internal.utils

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.CamcorderProfile
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.util.Log

import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration

import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Comparator
import java.util.Date

/**
 * Created by Arpit Gandhi on 7/6/16.
 *
 *
 * Class with some common methods to work with camera.
 */
object CameraHelper {

    val TAG = "CameraHelper"

    fun hasCamera(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA) || context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun hasCamera2(context: Context?): Boolean {
        if (context == null) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false
        try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val idList = manager.cameraIdList
            var notNull = true
            if (idList.size == 0) {
                notNull = false
            } else {
                for (str in idList) {
                    if (str == null || str.trim { it <= ' ' }.isEmpty()) {
                        notNull = false
                        break
                    }
                    val characteristics = manager.getCameraCharacteristics(str)

                    val supportLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)!!
                    if (supportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                        notNull = false
                        break
                    }
                }
            }
            return notNull
        } catch (ignore: Throwable) {
            return false
        }

    }

    fun getOutputMediaFile(context: Context, @CameraConfiguration.MediaAction mediaAction: Int): File? {
        val mediaStorageDir = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), context.packageName)

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Failed to create directory.")
                return null
            }
        }

        val timeStamp = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Date())
        val mediaFile: File
        if (mediaAction == CameraConfiguration.MEDIA_ACTION_PHOTO) {
            mediaFile = File(mediaStorageDir.path + File.separator +
                    "IMG_" + timeStamp + ".jpg")
        } else if (mediaAction == CameraConfiguration.MEDIA_ACTION_VIDEO) {
            mediaFile = File(mediaStorageDir.path + File.separator +
                    "VID_" + timeStamp + ".mp4")
        } else {
            return null
        }

        return mediaFile
    }

    fun getPictureSize(choices: List<Size>?, @CameraConfiguration.MediaQuality mediaQuality: Int): Size? {
        if (choices == null || choices.isEmpty()) return null
        if (choices.size == 1) return choices[0]

        var result: Size? = null
        val maxPictureSize = Collections.max(choices, CompareSizesByArea2())
        val minPictureSize = Collections.min(choices, CompareSizesByArea2())

        Collections.sort(choices, CompareSizesByArea2())

        if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_HIGHEST) {
            result = maxPictureSize
        } else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_LOW) {
            if (choices.size == 2)
                result = minPictureSize
            else {
                val half = choices.size / 2
                val lowQualityIndex = (choices.size - half) / 2
                result = choices[lowQualityIndex + 1]
            }
        } else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_HIGH) {
            if (choices.size == 2)
                result = maxPictureSize
            else {
                val half = choices.size / 2
                val highQualityIndex = (choices.size - half) / 2
                result = choices[choices.size - highQualityIndex - 1]
            }
        } else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_MEDIUM) {
            if (choices.size == 2)
                result = minPictureSize
            else {
                val mediumQualityIndex = choices.size / 2
                result = choices[mediumQualityIndex]
            }
        } else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_LOWEST) {
            result = minPictureSize
        }

        return result
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun getPictureSize(sizes: Array<Size>?, @CameraConfiguration.MediaQuality mediaQuality: Int): Size? {
        if (sizes == null || sizes.size == 0) return null

        val choices = Arrays.asList(*sizes)

        if (choices.size == 1) return choices[0]

        var result: Size? = null
        val maxPictureSize = Collections.max(choices, CompareSizesByArea2())
        val minPictureSize = Collections.min(choices, CompareSizesByArea2())

        Collections.sort(choices, CompareSizesByArea2())

        if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_HIGHEST) {
            result = maxPictureSize
        } else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_LOW) {
            if (choices.size == 2)
                result = minPictureSize
            else {
                val half = choices.size / 2
                val lowQualityIndex = (choices.size - half) / 2
                result = choices[lowQualityIndex + 1]
            }
        } else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_HIGH) {
            if (choices.size == 2)
                result = maxPictureSize
            else {
                val half = choices.size / 2
                val highQualityIndex = (choices.size - half) / 2
                result = choices[choices.size - highQualityIndex - 1]
            }
        } else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_MEDIUM) {
            if (choices.size == 2)
                result = minPictureSize
            else {
                val mediumQualityIndex = choices.size / 2
                result = choices[mediumQualityIndex]
            }
        } else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_LOWEST) {
            result = minPictureSize
        }

        return result
    }

    fun getOptimalPreviewSize(sizes: List<Size>?, width: Int, height: Int): Size? {
        val ASPECT_TOLERANCE = 0.1
        val targetRatio = height.toDouble() / width

        if (sizes == null) return null

        var optimalSize: Size? = null
        var minDiff = java.lang.Double.MAX_VALUE

        val targetHeight = height

        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size
                minDiff = Math.abs(size.height - targetHeight).toDouble()
            }
        }

        if (optimalSize == null) {
            minDiff = java.lang.Double.MAX_VALUE
            for (size in sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size
                    minDiff = Math.abs(size.height - targetHeight).toDouble()
                }
            }
        }
        return optimalSize
    }

    fun getSizeWithClosestRatio(sizes: List<Size>?, width: Int, height: Int): Size? {

        if (sizes == null) return null

        var MIN_TOLERANCE = 100.0
        val targetRatio = height.toDouble() / width
        var optimalSize: Size? = null
        var minDiff = java.lang.Double.MAX_VALUE

        val targetHeight = height

        for (size in sizes) {
            if (size.width == width && size.height == height)
                return size

            val ratio = size.height.toDouble() / size.width

            if (Math.abs(ratio - targetRatio) < MIN_TOLERANCE)
                MIN_TOLERANCE = ratio
            else
                continue

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size
                minDiff = Math.abs(size.height - targetHeight).toDouble()
            }
        }

        if (optimalSize == null) {
            minDiff = java.lang.Double.MAX_VALUE
            for (size in sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size
                    minDiff = Math.abs(size.height - targetHeight).toDouble()
                }
            }
        }
        return optimalSize
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun getOptimalPreviewSize(sizes: Array<Size>?, width: Int, height: Int): Size? {

        if (sizes == null) return null

        val ASPECT_TOLERANCE = 0.1
        val targetRatio = height.toDouble() / width
        var optimalSize: Size? = null
        var minDiff = java.lang.Double.MAX_VALUE

        val targetHeight = height

        for (size in sizes) {
            //            if (size.getWidth() == width && size.getHeight() == height)
            //                return size;
            val ratio = size.width.toDouble() / size.height
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size
                minDiff = Math.abs(size.height - targetHeight).toDouble()
            }
        }

        if (optimalSize == null) {
            minDiff = java.lang.Double.MAX_VALUE
            for (size in sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size
                    minDiff = Math.abs(size.height - targetHeight).toDouble()
                }
            }
        }
        return optimalSize
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun getSizeWithClosestRatio(sizes: Array<Size>?, width: Int, height: Int): Size? {

        if (sizes == null) return null

        var MIN_TOLERANCE = 100.0
        val targetRatio = height.toDouble() / width
        var optimalSize: Size? = null
        var minDiff = java.lang.Double.MAX_VALUE

        val targetHeight = height

        for (size in sizes) {
            //            if (size.getWidth() == width && size.getHeight() == height)
            //                return size;

            val ratio = size.height.toDouble() / size.width

            if (Math.abs(ratio - targetRatio) < MIN_TOLERANCE)
                MIN_TOLERANCE = ratio
            else
                continue

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size
                minDiff = Math.abs(size.height - targetHeight).toDouble()
            }
        }

        if (optimalSize == null) {
            minDiff = java.lang.Double.MAX_VALUE
            for (size in sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size
                    minDiff = Math.abs(size.height - targetHeight).toDouble()
                }
            }
        }
        return optimalSize
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun chooseOptimalSize(choices: Array<Size>, width: Int, height: Int, aspectRatio: Size): Size? {
        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough = ArrayList<Size>()
        val w = aspectRatio.width
        val h = aspectRatio.height
        for (option in choices) {
            if (option.height == option.width * h / w &&
                    option.width >= width && option.height >= height) {
                bigEnough.add(option)
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size > 0) {
            return Collections.min(bigEnough, CompareSizesByArea2())
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size")
            return null
        }
    }

    private fun calculateApproximateVideoSize(camcorderProfile: CamcorderProfile, seconds: Int): Double {
        return ((camcorderProfile.videoBitRate / 1.toFloat() + camcorderProfile.audioBitRate / 1.toFloat()) * seconds / 8.toFloat()).toDouble()
    }

    fun calculateApproximateVideoDuration(camcorderProfile: CamcorderProfile, maxFileSize: Long): Double {
        return (8 * maxFileSize / (camcorderProfile.videoBitRate + camcorderProfile.audioBitRate)).toDouble()
    }

    private fun calculateMinimumRequiredBitRate(camcorderProfile: CamcorderProfile, maxFileSize: Long, seconds: Int): Long {
        return 8 * maxFileSize / seconds - camcorderProfile.audioBitRate
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun getCamcorderProfile(cameraId: String, maximumFileSize: Long, minimumDurationInSeconds: Int): CamcorderProfile? {
        if (TextUtils.isEmpty(cameraId)) {
            return null
        }
        val cameraIdInt = Integer.parseInt(cameraId)
        return getCamcorderProfile(cameraIdInt, maximumFileSize, minimumDurationInSeconds)
    }

    fun getCamcorderProfile(currentCameraId: Int, maximumFileSize: Long, minimumDurationInSeconds: Int): CamcorderProfile {
        if (maximumFileSize <= 0)
            return CamcorderProfile.get(currentCameraId, CameraConfiguration.MEDIA_QUALITY_HIGHEST)

        val qualities = intArrayOf(CameraConfiguration.MEDIA_QUALITY_HIGHEST, CameraConfiguration.MEDIA_QUALITY_HIGH, CameraConfiguration.MEDIA_QUALITY_MEDIUM, CameraConfiguration.MEDIA_QUALITY_LOW, CameraConfiguration.MEDIA_QUALITY_LOWEST)

        var camcorderProfile: CamcorderProfile
        for (i in qualities.indices) {
            camcorderProfile = CameraHelper.getCamcorderProfile(qualities[i], currentCameraId)
            val fileSize = CameraHelper.calculateApproximateVideoSize(camcorderProfile, minimumDurationInSeconds)

            if (fileSize > maximumFileSize) {
                val minimumRequiredBitRate = calculateMinimumRequiredBitRate(camcorderProfile, maximumFileSize, minimumDurationInSeconds)

                if (minimumRequiredBitRate >= camcorderProfile.videoBitRate / 4 && minimumRequiredBitRate <= camcorderProfile.videoBitRate) {
                    camcorderProfile.videoBitRate = minimumRequiredBitRate.toInt()
                    return camcorderProfile
                }
            } else
                return camcorderProfile
        }
        return CameraHelper.getCamcorderProfile(CameraConfiguration.MEDIA_QUALITY_LOWEST, currentCameraId)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun getCamcorderProfile(@CameraConfiguration.MediaQuality mediaQuality: Int, cameraId: String): CamcorderProfile? {
        if (TextUtils.isEmpty(cameraId)) {
            return null
        }
        val cameraIdInt = Integer.parseInt(cameraId)
        return getCamcorderProfile(mediaQuality, cameraIdInt)
    }

    fun getCamcorderProfile(@CameraConfiguration.MediaQuality mediaQuality: Int, cameraId: Int): CamcorderProfile {
        if (Build.VERSION.SDK_INT > 10) {
            if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_HIGHEST) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH)
            } else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_HIGH) {
                if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_1080P)) {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_1080P)
                } else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P)
                } else {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH)
                }
            } else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_MEDIUM) {
                if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P)
                } else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P)
                } else {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW)
                }
            } else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_LOW) {
                if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P)
                } else {
                    return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW)
                }
            } else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_LOWEST) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW)
            } else {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH)
            }
        } else {
            if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_HIGHEST) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH)
            } else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_HIGH) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH)
            } else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_MEDIUM) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW)
            } else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_LOW) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW)
            } else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_LOWEST) {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW)
            } else {
                return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH)
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private class CompareSizesByArea2 : Comparator<Size> {
        override fun compare(lhs: Size, rhs: Size): Int {
            // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
        }

    }
}
