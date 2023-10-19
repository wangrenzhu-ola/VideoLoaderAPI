package io.agora.videoloaderapi

import android.app.Application

class AgoraApplication : Application() {

    companion object {
        private var sInstance: AgoraApplication? = null
        fun the(): AgoraApplication? {
            return sInstance
        }
    }

    var needPreJoin: Boolean = false
    var sliceMode: AGSlicingType = AGSlicingType.VISIABLE

    override fun onCreate() {
        super.onCreate()
        sInstance = this
    }
}