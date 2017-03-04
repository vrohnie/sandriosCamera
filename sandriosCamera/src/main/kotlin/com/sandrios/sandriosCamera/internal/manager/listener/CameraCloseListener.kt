package com.sandrios.sandriosCamera.internal.manager.listener

/**
 * Created by Arpit Gandhi on 8/14/16.
 */
interface CameraCloseListener<CameraId> {
    fun onCameraClosed(closedCameraId: CameraId)
}
