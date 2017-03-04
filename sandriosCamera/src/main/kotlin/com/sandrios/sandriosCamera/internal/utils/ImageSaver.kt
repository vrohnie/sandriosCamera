package com.sandrios.sandriosCamera.internal.utils

import android.annotation.TargetApi
import android.media.Image
import android.os.Build
import android.util.Log

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Created by Arpit Gandhi on 7/6/16.
 */
class ImageSaver(private val image: Image, private val file: File, private val imageSaverCallback: ImageSaver.ImageSaverCallback) : Runnable {

    @TargetApi(Build.VERSION_CODES.KITKAT)
    override fun run() {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        var output: FileOutputStream? = null
        try {
            output = FileOutputStream(file)
            output.write(bytes)
            imageSaverCallback.onSuccessFinish()
        } catch (ignore: IOException) {
            Log.e(TAG, "Can't save the image file.")
            imageSaverCallback.onError()
        } finally {
            image.close()
            if (null != output) {
                try {
                    output.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Can't release image or close the output stream.")
                }

            }
        }
    }

    interface ImageSaverCallback {
        fun onSuccessFinish()

        fun onError()
    }

    companion object {

        private val TAG = "ImageSaver"
    }

}
