package io.agora.videoloaderapi.service

import io.agora.videoloaderapi.rtc.RtcEngineInstance
import java.util.*

class ShowSyncManagerServiceImpl: ShowServiceProtocol {
    private val tag = "service"

    private val kRobotAvatars = listOf("https://download.agora.io/demo/release/bot1.png")
    private val kRobotUid = 2000000001
    private val kRobotVideoRoomIds = arrayListOf(2023001, 2023002, 2023003)
    private val kRobotVideoStreamUrls = arrayListOf(
        "https://download.agora.io/sdk/release/agora_test_video_10.mp4",
        "https://download.agora.io/sdk/release/agora_test_video_11.mp4",
        "https://download.agora.io/sdk/release/agora_test_video_12.mp4"
    )

    private val cloudPlayerService by lazy { CloudPlayerService() }

    // global cache data
    private val roomMap = mutableMapOf<String, ShowRoomDetailModel>()

    // current room cache data
    data class RoomInfoController constructor(
        val roomId: String
    )

    private val roomInfoControllers = Collections.synchronizedList(mutableListOf<RoomInfoController>())

    override fun destroy() {

    }

    override fun getRoomList(
        success: (List<ShowRoomDetailModel>) -> Unit,
        error: ((Exception) -> Unit)?
    ) {
        success.invoke(appendRobotRooms())
    }

    private fun appendRobotRooms(): List<ShowRoomDetailModel> {
        val retRoomList = mutableListOf<ShowRoomDetailModel>()

        val robotRoomIds = ArrayList(kRobotVideoRoomIds)
        val kRobotRoomStartId = kRobotVideoRoomIds[0]
        retRoomList.forEach { roomDetail ->
            val differValue = roomDetail.roomId.toInt() - kRobotRoomStartId
            if (differValue >= 0) {
                robotRoomIds.firstOrNull { robotRoomId -> robotRoomId == roomDetail.roomId.toInt() }?.let { id ->
                    robotRoomIds.remove(id)
                }
            }

        }
        for (i in 0 until robotRoomIds.size) {
            val robotRoomId = robotRoomIds[i]
            val robotId = robotRoomId % 10
            val interactionRoomName = if (i == 0) robotRoomIds[1].toString() else ""
            val interactionStatus = if (i == 0) ShowInteractionStatus.pking.value else ShowInteractionStatus.idle.value
            val roomInfo = ShowRoomDetailModel(
                robotRoomId.toString(), // roomId
                "Smooth $robotId", // roomName
                1,
                "1",
                kRobotUid.toString(),
                kRobotAvatars[(robotId - 1) % kRobotAvatars.size],
                "Robot $robotId",
                ShowRoomStatus.activity.value,
                interactionStatus,
                interactionRoomName,
                0.0,
                0.0
            )
            roomMap[roomInfo.roomId] = roomInfo
            retRoomList.add(roomInfo)
        }
        return retRoomList
    }

    override fun startCloudPlayer() {
        for (i in 0 until kRobotVideoRoomIds.size) {
            val roomId = kRobotVideoRoomIds[i]
            cloudPlayerService.startCloudPlayer(
                roomId.toString(),
                RtcEngineInstance.localUid().toString(),
                kRobotUid,
                kRobotVideoStreamUrls[i],
                "cn",
                success = {
                    cloudPlayerService.startHeartBeat(
                        roomId.toString(),
                        RtcEngineInstance.localUid().toString()
                    )
                },
                failure = { })
        }
    }
}