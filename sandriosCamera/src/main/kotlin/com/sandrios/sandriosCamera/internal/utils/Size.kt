package com.sandrios.sandriosCamera.internal.utils

import android.annotation.TargetApi
import android.hardware.Camera
import android.os.Build

import java.util.ArrayList

/**
 * Created by Arpit Gandhi on 12/1/16.
 */

class Size {

    var width: Int = 0
    var height: Int = 0

    constructor() {
        width = 0
        height = 0
    }

    constructor(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(size: android.util.Size) {
        this.width = size.width
        this.height = size.height
    }

    constructor(size: Camera.Size) {
        this.width = size.width
        this.height = size.height
    }

    companion object {

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        fun fromList2(sizes: List<android.util.Size>?): List<Size>? {
            if (sizes == null) return null
            val result = ArrayList<Size>(sizes.size)

            for (size in sizes) {
                result.add(Size(size))
            }

            return result
        }

        fun fromList(sizes: List<Camera.Size>?): List<Size>? {
            if (sizes == null) return null
            val result = ArrayList<Size>(sizes.size)

            for (size in sizes) {
                result.add(Size(size))
            }

            return result
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        fun fromArray2(sizes: Array<android.util.Size>?): Array<Size>? {
            if (sizes == null) return null
            val result = arrayOfNulls<Size>(sizes.size)

            for (i in sizes.indices) {
                result[i] = Size(sizes[i])
            }

            return result
        }

        fun fromArray(sizes: Array<Camera.Size>?): Array<Size>? {
            if (sizes == null) return null
            val result = arrayOfNulls<Size>(sizes.size)

            for (i in sizes.indices) {
                result[i] = Size(sizes[i])
            }

            return result
        }
    }
}
