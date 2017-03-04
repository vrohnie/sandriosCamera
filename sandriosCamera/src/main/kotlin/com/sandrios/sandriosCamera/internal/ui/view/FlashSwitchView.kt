package com.sandrios.sandriosCamera.internal.ui.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.IntDef
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton

import com.sandrios.sandriosCamera.R

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy


/**
 * Created by Arpit Gandhi on 7/6/16.
 */
class FlashSwitchView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ImageButton(context, attrs) {
    @FlashMode
    var currentFlashMode = FLASH_AUTO
        private set
    private var switchListener: FlashModeSwitchListener? = null
    private val flashOnDrawable: Drawable
    private val flashOffDrawable: Drawable
    private val flashAutoDrawable: Drawable

    init {
        flashOnDrawable = ContextCompat.getDrawable(context, R.drawable.ic_flash_on_white_24dp)
        flashOffDrawable = ContextCompat.getDrawable(context, R.drawable.ic_flash_off_white_24dp)
        flashAutoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_flash_auto_white_24dp)
        init()
    }

    private fun init() {
        setBackgroundColor(Color.TRANSPARENT)
        setOnClickListener(FlashButtonClickListener())
        setIcon()
    }

    private fun setIcon() {
        if (FLASH_OFF == currentFlashMode) {
            setImageDrawable(flashOffDrawable)
        } else if (FLASH_ON == currentFlashMode) {
            setImageDrawable(flashOnDrawable)
        } else
            setImageDrawable(flashAutoDrawable)

    }

    fun setFlashMode(@FlashMode mode: Int) {
        this.currentFlashMode = mode
        setIcon()
    }

    fun setFlashSwitchListener(switchListener: FlashModeSwitchListener) {
        this.switchListener = switchListener
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (Build.VERSION.SDK_INT > 10) {
            if (enabled) {
                alpha = 1f
            } else {
                alpha = 0.5f
            }
        }
    }

    @IntDef(FLASH_ON.toLong(), FLASH_OFF.toLong(), FLASH_AUTO.toLong())
    @Retention(RetentionPolicy.SOURCE)
    annotation class FlashMode

    interface FlashModeSwitchListener {
        fun onFlashModeChanged(@FlashMode mode: Int)
    }

    private inner class FlashButtonClickListener : View.OnClickListener {

        override fun onClick(v: View) {
            if (FLASH_AUTO == currentFlashMode) {
                currentFlashMode = FLASH_OFF
            } else if (FLASH_OFF == currentFlashMode) {
                currentFlashMode = FLASH_ON
            } else if (FLASH_ON == currentFlashMode) {
                currentFlashMode = FLASH_AUTO
            }
            setIcon()
            if (switchListener != null) {
                switchListener!!.onFlashModeChanged(currentFlashMode)
            }
        }
    }

    companion object {

        val FLASH_ON = 0
        val FLASH_OFF = 1
        val FLASH_AUTO = 2
    }
}
