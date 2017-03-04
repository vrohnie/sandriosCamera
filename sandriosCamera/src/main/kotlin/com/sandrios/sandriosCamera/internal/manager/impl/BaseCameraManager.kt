package com.sandrios.sandriosCamera.internal.manager.impl

import android.content.Context
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.util.Log

import com.sandrios.sandriosCamera.internal.configuration.ConfigurationProvider
import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration
import com.sandrios.sandriosCamera.internal.manager.CameraManager
import com.sandrios.sandriosCamera.internal.utils.Size

/**
 * Created by Arpit Gandhi on 8/14/16.
 */
internal abstract class BaseCameraManager<CameraId, SurfaceListener> : CameraManager<CameraId, SurfaceListener>, MediaRecorder.OnInfoListener {

    protected var context: Context? = null
    var configurationProvider: ConfigurationProvider

    var videoRecorder: MediaRecorder? = null
    override var isVideoRecording = false

    override var currentCameraId: CameraId? = null
    override var faceFrontCameraId: CameraId? = null
    override var faceBackCameraId: CameraId? = null
    override var numberOfCameras = 0
    override var faceFrontCameraOrientation: Int = 0
    override var faceBackCameraOrientation: Int = 0

    var camcorderProfile: CamcorderProfile? = null
    var photoSize: Size? = null
    var videoSize: Size? = null
    var previewSize: Size? = null
    var windowSize: Size? = null

    var backgroundThread: HandlerThread? = null
    var backgroundHandler: Handler? = null
    var uiHandler = Handler(Looper.getMainLooper())

    override fun initializeCameraManager(configurationProvider: ConfigurationProvider, context: Context) {
        this.context = context
        this.configurationProvider = configurationProvider
        startBackgroundThread()
    }

    override fun releaseCameraManager() {
        this.context = null
        stopBackgroundThread()
    }

    protected abstract fun prepareCameraOutputs()

    protected abstract fun prepareVideoRecorder(): Boolean

    protected abstract fun onMaxDurationReached()

    protected abstract fun onMaxFileSizeReached()

    protected abstract fun getPhotoOrientation(@CameraConfiguration.SensorPosition sensorPosition: Int): Int

    protected abstract fun getVideoOrientation(@CameraConfiguration.SensorPosition sensorPosition: Int): Int

    protected open fun releaseVideoRecorder() {
        try {
            if (videoRecorder != null) {
                videoRecorder!!.reset()
                videoRecorder!!.release()
            }
        } catch (ignore: Exception) {

        } finally {
            videoRecorder = null
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND)
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        if (Build.VERSION.SDK_INT > 17) {
            backgroundThread!!.quitSafely()
        } else
            backgroundThread!!.quit()

        try {
            backgroundThread!!.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, "stopBackgroundThread: ", e)
        } finally {
            backgroundThread = null
            backgroundHandler = null
        }
    }

    override fun onInfo(mediaRecorder: MediaRecorder, what: Int, extra: Int) {
        if (MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED == what) {
            onMaxDurationReached()
        } else if (MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED == what) {
            onMaxFileSizeReached()
        }
    }

    companion object {

        private val TAG = "BaseCameraManager"
    }


}
