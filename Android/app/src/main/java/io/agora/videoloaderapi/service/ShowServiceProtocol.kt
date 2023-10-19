package io.agora.videoloaderapi.service

interface ShowServiceProtocol {

    companion object {
        private val instance by lazy {
            ShowSyncManagerServiceImpl()
        }

        fun getImplInstance(): ShowServiceProtocol = instance
    }

    // 释放资源
    fun destroy()

    // 获取房间列表
    fun getRoomList(
        success: (List<ShowRoomDetailModel>) -> Unit,
        error: ((Exception) -> Unit)? = null
    )

    // 启动机器人
    fun startCloudPlayer()
}