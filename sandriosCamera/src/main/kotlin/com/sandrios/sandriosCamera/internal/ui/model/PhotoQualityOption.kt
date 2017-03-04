package com.sandrios.sandriosCamera.internal.ui.model

import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration
import com.sandrios.sandriosCamera.internal.utils.Size

/**
 * Created by Arpit Gandhi on 12/1/16.
 */

class PhotoQualityOption(@CameraConfiguration.MediaQuality

                         @CameraConfiguration.MediaQuality
                         val mediaQuality: Int, size: Size) : CharSequence {
    private val title: String

    init {

        title = size.width.toString() + " x " + size.height.toString()
    }

    override fun length(): Int {
        return title.length
    }

    override fun charAt(index: Int): Char {
        return title[index]
    }

    override fun subSequence(start: Int, end: Int): CharSequence {
        return title.subSequence(start, end)
    }

    override fun toString(): String {
        return title
    }
}
