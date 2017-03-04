package com.sandrios.sandriosCamera.internal.manager.listener

import com.sandrios.sandriosCamera.internal.utils.Size

import java.io.File

/**
 * Created by Arpit Gandhi on 8/14/16.
 */
interface CameraVideoListener {
    fun onVideoRecordStarted(videoSize: Size)

    fun onVideoRecordStopped(videoFile: File)

    fun onVideoRecordError()
}
