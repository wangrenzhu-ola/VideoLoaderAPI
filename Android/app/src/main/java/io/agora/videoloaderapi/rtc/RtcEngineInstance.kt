package io.agora.videoloaderapi.rtc

import android.util.Log
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.RtcEngineEx
import io.agora.videoloaderapi.AgoraApplication
import io.agora.videoloaderapi.VideoLoader
import java.util.concurrent.Executors

object RtcEngineInstance {
    private val workingExecutor = Executors.newSingleThreadExecutor()

    // 万能通用 token ,进入房间列表默认获取万能 token
    private var generalToken: String = ""

    fun setupGeneralToken(generalToken: String) {
        RtcEngineInstance.generalToken = generalToken
    }

    fun generalToken(): String = generalToken

    private var innerLocalUid: Int = 0
    private val localUid: Int
        get() {
            if (innerLocalUid == 0) {
                innerLocalUid = (100..10000000).random()
            }
            return innerLocalUid
        }

    fun localUid(): Int = localUid


    private var innerRtcEngine: RtcEngineEx? = null
    val rtcEngine: RtcEngineEx
        get() {
            if (innerRtcEngine == null) {
                val config = RtcEngineConfig()
                config.mContext = AgoraApplication.the()
                config.mAppId = io.agora.videoloaderapi.BuildConfig.AGORA_APP_ID
                config.mEventHandler = object : IRtcEngineEventHandler() {
                    override fun onError(err: Int) {
                        super.onError(err)
                        Log.d(
                            "RtcEngineInstance",
                            "Rtc Error code:$err, msg:" + RtcEngine.getErrorDescription(err)
                        )
                    }
                }
                innerRtcEngine = (RtcEngine.create(config) as RtcEngineEx).apply {
                    enableVideo()
                }
            }
            return innerRtcEngine!!
        }

    fun cleanCache() {
        VideoLoader.getImplInstance(rtcEngine).cleanCache()
    }

    fun destroy() {
        innerRtcEngine?.let {
            innerLocalUid = 0
            workingExecutor.execute { RtcEngine.destroy() }
            innerRtcEngine = null
        }
    }
}