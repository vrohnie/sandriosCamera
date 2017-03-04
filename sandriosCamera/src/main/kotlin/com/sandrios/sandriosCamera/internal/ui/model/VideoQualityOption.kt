package com.sandrios.sandriosCamera.internal.ui.model

import android.media.CamcorderProfile

import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration

import java.util.concurrent.TimeUnit

/**
 * Created by Arpit Gandhi on 12/1/16.
 */

class VideoQualityOption(@CameraConfiguration.MediaQuality

                         @CameraConfiguration.MediaQuality
                         val mediaQuality: Int, camcorderProfile: CamcorderProfile, videoDuration: Double) : CharSequence {

    private var title: String? = null

    init {

        val minutes = TimeUnit.SECONDS.toMinutes(videoDuration.toLong())
        val seconds = videoDuration.toLong() - minutes * 60

        if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_AUTO) {
            title = "Auto " + ", (" + (if (minutes > 10) minutes else "0" + minutes) + ":" + (if (seconds > 10) seconds else "0" + seconds) + " min)"
        } else {
            title = camcorderProfile.videoFrameWidth.toString()
            +" x " + camcorderProfile.videoFrameHeight.toString()
            +if (videoDuration <= 0) "" else ", (" + (if (minutes > 10) minutes else "0" + minutes) + ":" + (if (seconds > 10) seconds else "0" + seconds) + " min)"
        }
    }

    override fun length(): Int {
        return title!!.length
    }

    override fun charAt(index: Int): Char {
        return title!![index]
    }

    override fun subSequence(start: Int, end: Int): CharSequence {
        return title!!.subSequence(start, end)
    }

    override fun toString(): String {
        return title
    }
}