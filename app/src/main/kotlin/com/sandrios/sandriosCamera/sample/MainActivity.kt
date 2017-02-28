package com.sandrios.sandriosCamera.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast

import com.sandrios.sandriosCamera.internal.SandriosCamera
import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration

/**
 * SampleApp MainActivity
 * Created by Arpit Gandhi
 */

class MainActivity : AppCompatActivity() {

    private var activity: Activity? = null
    private val onClickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.withPicker -> SandriosCamera(activity, CAPTURE_MEDIA)
                    .setShowPicker(true)
                    .setVideoFileSize(20)
                    .setMediaAction(CameraConfiguration.MEDIA_ACTION_BOTH)
                    .enableImageCropping(true)
                    .launchCamera()
            R.id.withoutPicker -> SandriosCamera(activity, CAPTURE_MEDIA)
                    .setShowPicker(false)
                    .setMediaAction(CameraConfiguration.MEDIA_ACTION_PHOTO)
                    .enableImageCropping(false)
                    .launchCamera()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_layout)
        activity = this

        findViewById(R.id.withPicker).setOnClickListener(onClickListener)
        findViewById(R.id.withoutPicker).setOnClickListener(onClickListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAPTURE_MEDIA && resultCode == Activity.RESULT_OK) {
            Log.e("File", "" + data.getStringExtra(CameraConfiguration.Arguments.FILE_PATH))
            Toast.makeText(this, "Media captured.", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private val CAPTURE_MEDIA = 368
    }
}
