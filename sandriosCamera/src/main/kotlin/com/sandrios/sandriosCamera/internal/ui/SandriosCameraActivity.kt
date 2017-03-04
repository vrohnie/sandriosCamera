package com.sandrios.sandriosCamera.internal.ui

import android.app.Activity
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.sandrios.sandriosCamera.R
import com.sandrios.sandriosCamera.internal.configuration.ConfigurationProvider
import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration
import com.sandrios.sandriosCamera.internal.controller.CameraController
import com.sandrios.sandriosCamera.internal.controller.view.CameraView
import com.sandrios.sandriosCamera.internal.ui.view.AspectFrameLayout
import com.sandrios.sandriosCamera.internal.utils.Size
import com.sandrios.sandriosCamera.internal.utils.Utils

/**
 * Created by Arpit Gandhi on 12/1/16.
 */

abstract class SandriosCameraActivity<CameraId> : Activity(), ConfigurationProvider, CameraView, SensorEventListener {

    protected var previewContainer: AspectFrameLayout? = null
    protected var userContainer: ViewGroup
    @CameraConfiguration.SensorPosition
    override var sensorPosition = CameraConfiguration.SENSOR_POSITION_UNSPECIFIED
        protected set
    @CameraConfiguration.DeviceDefaultOrientation
    protected var deviceDefaultOrientation: Int = 0
    private var sensorManager: SensorManager? = null
    var cameraController: CameraController<CameraId>? = null
        private set
    override var degrees = -1
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraController = createCameraController(this, this)
        cameraController!!.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val defaultOrientation = Utils.getDeviceDefaultOrientation(this)

        if (defaultOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            deviceDefaultOrientation = CameraConfiguration.ORIENTATION_LANDSCAPE
        } else if (defaultOrientation == Configuration.ORIENTATION_PORTRAIT) {
            deviceDefaultOrientation = CameraConfiguration.ORIENTATION_PORTRAIT
        }

        val decorView = window.decorView
        if (Build.VERSION.SDK_INT > 15) {
            val uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            decorView.systemUiVisibility = uiOptions
        }

        setContentView(R.layout.generic_camera_layout)

        previewContainer = findViewById(R.id.previewContainer) as AspectFrameLayout
        userContainer = findViewById(R.id.userContainer) as ViewGroup

        onProcessBundle(savedInstanceState)
        setUserContent()
    }

    protected open fun onProcessBundle(savedInstanceState: Bundle) {

    }

    override fun onResume() {
        super.onResume()

        cameraController!!.onResume()
        sensorManager!!.registerListener(this, sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()

        cameraController!!.onPause()
        sensorManager!!.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        cameraController!!.onDestroy()
    }

    abstract fun createCameraController(cameraView: CameraView, configurationProvider: ConfigurationProvider): CameraController<CameraId>

    private fun setUserContent() {
        userContainer.removeAllViews()
        userContainer.addView(getUserContentView(LayoutInflater.from(this), userContainer))
    }

    fun setCameraPreview(preview: View?, previewSize: Size) {
        onCameraControllerReady()

        if (previewContainer == null || preview == null) return
        previewContainer!!.removeAllViews()
        previewContainer!!.addView(preview)

        previewContainer!!.setAspectRatio(previewSize.height / previewSize.width.toDouble())
    }

    fun clearCameraPreview() {
        if (previewContainer != null)
            previewContainer!!.removeAllViews()
    }

    internal abstract fun getUserContentView(layoutInflater: LayoutInflater, parent: ViewGroup): View

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        synchronized(this) {
            if (sensorEvent.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                if (sensorEvent.values[0] < 4 && sensorEvent.values[0] > -4) {
                    if (sensorEvent.values[1] > 0) {
                        // UP
                        sensorPosition = CameraConfiguration.SENSOR_POSITION_UP
                        degrees = if (deviceDefaultOrientation == CameraConfiguration.ORIENTATION_PORTRAIT) 0 else 90
                    } else if (sensorEvent.values[1] < 0) {
                        // UP SIDE DOWN
                        sensorPosition = CameraConfiguration.SENSOR_POSITION_UP_SIDE_DOWN
                        degrees = if (deviceDefaultOrientation == CameraConfiguration.ORIENTATION_PORTRAIT) 180 else 270
                    }
                } else if (sensorEvent.values[1] < 4 && sensorEvent.values[1] > -4) {
                    if (sensorEvent.values[0] > 0) {
                        // LEFT
                        sensorPosition = CameraConfiguration.SENSOR_POSITION_LEFT
                        degrees = if (deviceDefaultOrientation == CameraConfiguration.ORIENTATION_PORTRAIT) 90 else 180
                    } else if (sensorEvent.values[0] < 0) {
                        // RIGHT
                        sensorPosition = CameraConfiguration.SENSOR_POSITION_RIGHT
                        degrees = if (deviceDefaultOrientation == CameraConfiguration.ORIENTATION_PORTRAIT) 270 else 0
                    }
                }
                onScreenRotation(degrees)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }

    protected abstract fun onScreenRotation(degrees: Int)

    protected open fun onCameraControllerReady() {}
}
