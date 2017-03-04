package com.sandrios.sandriosCamera.internal.manager.listener

import java.io.File

/**
 * Created by Arpit Gandhi on 8/14/16.
 */
interface CameraPhotoListener {
    fun onPhotoTaken(photoFile: File)

    fun onPhotoTakeError()
}
