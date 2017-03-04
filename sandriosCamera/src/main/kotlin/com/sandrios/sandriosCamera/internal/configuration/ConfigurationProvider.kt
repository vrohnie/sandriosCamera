package com.sandrios.sandriosCamera.internal.configuration

/**
 * Created by Arpit Gandhi on 7/6/16.
 */
interface ConfigurationProvider {

    val requestCode: Int

    val mediaAction: Int

    val mediaQuality: Int

    val videoDuration: Int

    val videoFileSize: Long

    val sensorPosition: Int

    val degrees: Int

    val minimumVideoDuration: Int

    val flashMode: Int

}
