package com.sandrios.sandriosCamera.internal.manager.listener

import com.sandrios.sandriosCamera.internal.utils.Size

/**
 * Created by Arpit Gandhi on 8/14/16.
 */
interface CameraOpenListener<CameraId, SurfaceListener> {
    fun onCameraOpened(openedCameraId: CameraId, previewSize: Size, surfaceListener: SurfaceListener)

    fun onCameraOpenError()
}
