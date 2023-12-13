package io.agora.videoloaderapi

import android.app.Application

/**
 * 秒切UI模式
 */
enum class AGUIType(val value :Int) {
    ViewPager(0),
    RecycleView(1)
}

class AgoraApplication : Application() {

    companion object {
        private var sInstance: AgoraApplication? = null
        fun the(): AgoraApplication? {
            return sInstance
        }
    }

    var uiMode: AGUIType = AGUIType.ViewPager
    var needPreJoin: Boolean = false
    var sliceMode: AGSlicingType = AGSlicingType.VISIBLE

    override fun onCreate() {
        super.onCreate()
        sInstance = this
    }
}