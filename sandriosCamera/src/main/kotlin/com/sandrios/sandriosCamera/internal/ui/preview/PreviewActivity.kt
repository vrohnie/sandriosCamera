package com.sandrios.sandriosCamera.internal.ui.preview

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.MediaController

import com.sandrios.sandriosCamera.R
import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration
import com.sandrios.sandriosCamera.internal.ui.BaseSandriosActivity
import com.sandrios.sandriosCamera.internal.ui.view.AspectFrameLayout
import com.sandrios.sandriosCamera.internal.utils.Utils
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.view.UCropView

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel

/**
 * Created by Arpit Gandhi on 7/6/16.
 */
class PreviewActivity : AppCompatActivity(), View.OnClickListener {

    private var mediaAction: Int = 0
    private var previewFilePath: String? = null
    private var mContext: PreviewActivity? = null
    private var surfaceView: SurfaceView? = null
    private var photoPreviewContainer: FrameLayout? = null
    private var imagePreview: UCropView? = null
    private var buttonPanel: ViewGroup? = null
    private var videoPreviewContainer: AspectFrameLayout? = null

    private var mediaController: MediaController? = null
    private var mediaPlayer: MediaPlayer? = null

    private var currentPlaybackPosition = 0
    private var isVideoPlaying = true
    private var showCrop = false

    private val MediaPlayerControlImpl = object : MediaController.MediaPlayerControl {
        override fun start() {
            mediaPlayer!!.start()
        }

        override fun pause() {
            mediaPlayer!!.pause()
        }

        override fun getDuration(): Int {
            return mediaPlayer!!.duration
        }

        override fun getCurrentPosition(): Int {
            return mediaPlayer!!.currentPosition
        }

        override fun seekTo(pos: Int) {
            mediaPlayer!!.seekTo(pos)
        }

        override fun isPlaying(): Boolean {
            return mediaPlayer!!.isPlaying
        }

        override fun getBufferPercentage(): Int {
            return 0
        }

        override fun canPause(): Boolean {
            return true
        }

        override fun canSeekBackward(): Boolean {
            return true
        }

        override fun canSeekForward(): Boolean {
            return true
        }

        override fun getAudioSessionId(): Int {
            return mediaPlayer!!.audioSessionId
        }
    }
    private val surfaceCallbacks = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            showVideoPreview(holder)
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        mContext = this
        surfaceView = findViewById(R.id.video_preview) as SurfaceView
        surfaceView!!.setOnTouchListener(View.OnTouchListener { v, event ->
            if (mediaController == null) return@OnTouchListener false
            if (mediaController!!.isShowing) {
                mediaController!!.hide()
                showButtonPanel(true)
            } else {
                showButtonPanel(false)
                mediaController!!.show()
            }
            false
        })

        videoPreviewContainer = findViewById(R.id.previewAspectFrameLayout) as AspectFrameLayout
        photoPreviewContainer = findViewById(R.id.photo_preview_container) as FrameLayout
        imagePreview = findViewById(R.id.image_view) as UCropView
        buttonPanel = findViewById(R.id.preview_control_panel) as ViewGroup
        val confirmMediaResult = findViewById(R.id.confirm_media_result)
        val reTakeMedia = findViewById(R.id.re_take_media)
        val cancelMediaAction = findViewById(R.id.cancel_media_action)

        findViewById(R.id.crop_image).setOnClickListener {
            val options = UCrop.Options()
            options.setToolbarColor(ContextCompat.getColor(mContext!!, android.R.color.black))
            options.setStatusBarColor(ContextCompat.getColor(mContext!!, android.R.color.black))
            val uri = Uri.fromFile(File(previewFilePath!!))
            UCrop.of(uri, uri)
                    .withOptions(options)
                    .start(mContext!!)
        }

        confirmMediaResult?.setOnClickListener(this)

        reTakeMedia?.setOnClickListener(this)

        cancelMediaAction?.setOnClickListener(this)

        val args = intent.extras

        mediaAction = args.getInt(MEDIA_ACTION_ARG)
        previewFilePath = args.getString(FILE_PATH_ARG)
        showCrop = args.getBoolean(SHOW_CROP)

        if (mediaAction == CameraConfiguration.MEDIA_ACTION_VIDEO) {
            displayVideo(savedInstanceState)
        } else if (mediaAction == CameraConfiguration.MEDIA_ACTION_PHOTO) {
            displayImage()
        } else {
            val mimeType = Utils.getMimeType(previewFilePath)
            if (mimeType.contains(MIME_TYPE_VIDEO)) {
                displayVideo(savedInstanceState)
            } else if (mimeType.contains(MIME_TYPE_IMAGE)) {
                displayImage()
            } else
                finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveVideoParams(outState)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            showImagePreview()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
        if (mediaController != null) {
            mediaController!!.hide()
            mediaController = null
        }
    }

    private fun displayImage() {
        if (showCrop)
            findViewById(R.id.crop_image).visibility = View.VISIBLE
        else
            findViewById(R.id.crop_image).visibility = View.GONE

        videoPreviewContainer!!.visibility = View.GONE
        surfaceView!!.visibility = View.GONE
        showImagePreview()
    }

    private fun showImagePreview() {
        try {
            val uri = Uri.fromFile(File(previewFilePath!!))
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(File(uri.path).absolutePath, options)

            imagePreview!!.cropImageView.setImageUri(uri, null)
            imagePreview!!.overlayView.setShowCropFrame(false)
            imagePreview!!.overlayView.setShowCropGrid(false)
            imagePreview!!.cropImageView.isRotateEnabled = false
            imagePreview!!.overlayView.setDimmedColor(Color.TRANSPARENT)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun displayVideo(savedInstanceState: Bundle?) {
        findViewById(R.id.crop_image).visibility = View.GONE
        if (savedInstanceState != null) {
            loadVideoParams(savedInstanceState)
        }
        photoPreviewContainer!!.visibility = View.GONE
        surfaceView!!.holder.addCallback(surfaceCallbacks)
    }

    private fun showVideoPreview(holder: SurfaceHolder) {
        try {
            mediaPlayer = MediaPlayer()
            mediaPlayer!!.setDataSource(previewFilePath)
            mediaPlayer!!.setDisplay(holder)
            mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mediaPlayer!!.setOnPreparedListener { mp ->
                mediaController = MediaController(mContext)
                mediaController!!.setAnchorView(surfaceView)
                mediaController!!.setMediaPlayer(MediaPlayerControlImpl)

                val videoWidth = mp.videoWidth
                val videoHeight = mp.videoHeight

                videoPreviewContainer!!.setAspectRatio(videoWidth.toDouble() / videoHeight)

                mediaPlayer!!.start()
                mediaPlayer!!.seekTo(currentPlaybackPosition)

                if (!isVideoPlaying)
                    mediaPlayer!!.pause()
            }
            mediaPlayer!!.setOnErrorListener { mp, what, extra ->
                finish()
                true
            }
            mediaPlayer!!.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Error media player playing video.")
            finish()
        }

    }

    private fun saveCroppedImage(croppedFileUri: Uri) {
        try {
            val saveFile = File(previewFilePath!!)
            val inStream = FileInputStream(File(croppedFileUri.path))
            val outStream = FileOutputStream(saveFile)
            val inChannel = inStream.channel
            val outChannel = outStream.channel
            inChannel.transferTo(0, inChannel.size(), outChannel)
            inStream.close()
            outStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun saveVideoParams(outState: Bundle) {
        if (mediaPlayer != null) {
            outState.putInt(VIDEO_POSITION_ARG, mediaPlayer!!.currentPosition)
            outState.putBoolean(VIDEO_IS_PLAYED_ARG, mediaPlayer!!.isPlaying)
        }
    }

    private fun loadVideoParams(savedInstanceState: Bundle) {
        currentPlaybackPosition = savedInstanceState.getInt(VIDEO_POSITION_ARG, 0)
        isVideoPlaying = savedInstanceState.getBoolean(VIDEO_IS_PLAYED_ARG, true)
    }

    private fun showButtonPanel(show: Boolean) {
        if (show) {
            buttonPanel!!.visibility = View.VISIBLE
        } else {
            buttonPanel!!.visibility = View.GONE
        }
    }

    override fun onClick(view: View) {
        val resultIntent = Intent()
        if (view.id == R.id.confirm_media_result) {
            resultIntent.putExtra(RESPONSE_CODE_ARG, BaseSandriosActivity.ACTION_CONFIRM)
            resultIntent.putExtra(FILE_PATH_ARG, previewFilePath)
        } else if (view.id == R.id.re_take_media) {
            deleteMediaFile()
            resultIntent.putExtra(RESPONSE_CODE_ARG, BaseSandriosActivity.ACTION_RETAKE)
        } else if (view.id == R.id.cancel_media_action) {
            deleteMediaFile()
            resultIntent.putExtra(RESPONSE_CODE_ARG, BaseSandriosActivity.ACTION_CANCEL)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        deleteMediaFile()
    }

    private fun deleteMediaFile(): Boolean {
        val mediaFile = File(previewFilePath!!)
        return mediaFile.delete()
    }

    companion object {

        private val TAG = "PreviewActivity"

        private val SHOW_CROP = "show_crop"
        private val MEDIA_ACTION_ARG = "media_action_arg"
        private val FILE_PATH_ARG = "file_path_arg"
        private val RESPONSE_CODE_ARG = "response_code_arg"
        private val VIDEO_POSITION_ARG = "current_video_position"
        private val VIDEO_IS_PLAYED_ARG = "is_played"
        private val MIME_TYPE_VIDEO = "video"
        private val MIME_TYPE_IMAGE = "image"

        fun newIntent(context: Context,
                      @CameraConfiguration.MediaAction mediaAction: Int,
                      filePath: String, showImageCrop: Boolean): Intent {

            return Intent(context, PreviewActivity::class.java)
                    .putExtra(MEDIA_ACTION_ARG, mediaAction)
                    .putExtra(SHOW_CROP, showImageCrop)
                    .putExtra(FILE_PATH_ARG, filePath)
        }

        fun isResultConfirm(resultIntent: Intent): Boolean {
            return BaseSandriosActivity.ACTION_CONFIRM == resultIntent.getIntExtra(RESPONSE_CODE_ARG, -1)
        }

        fun getMediaFilePatch(resultIntent: Intent): String {
            return resultIntent.getStringExtra(FILE_PATH_ARG)
        }

        fun isResultRetake(resultIntent: Intent): Boolean {
            return BaseSandriosActivity.ACTION_RETAKE == resultIntent.getIntExtra(RESPONSE_CODE_ARG, -1)
        }

        fun isResultCancel(resultIntent: Intent): Boolean {
            return BaseSandriosActivity.ACTION_CANCEL == resultIntent.getIntExtra(RESPONSE_CODE_ARG, -1)
        }
    }
}
