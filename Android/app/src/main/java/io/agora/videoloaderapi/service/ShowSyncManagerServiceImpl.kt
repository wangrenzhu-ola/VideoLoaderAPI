package io.agora.videoloaderapi.service

import io.agora.videoloaderapi.rtc.RtcEngineInstance
import java.util.*

class ShowSyncManagerServiceImpl: ShowServiceProtocol {
    private val tag = "service"

    private val kRobotAvatars = listOf("https://download.agora.io/demo/release/bot1.png")
    private val kRobotUid = 2000000001
    private val kRobotVideoRoomIds = arrayListOf(1001001, 1001002, 1001003, 1001004, 1001005, 1001006)
    private val kRobotVideoStreamUrls = arrayListOf(
        "https://download.agora.io/demo/release/agora_test_video_20_music.mp4",
        "https://download.agora.io/demo/release/agora_test_video_21_music.mp4",
        "https://download.agora.io/demo/release/agora_test_video_22_music.mp4",
        "https://download.agora.io/sdk/release/agora_test_video_12.mp4",
        "https://download.agora.io/sdk/release/agora_test_video_11.mp4",
        "https://download.agora.io/sdk/release/agora_test_video_10.mp4"
    )

    private val cloudPlayerService by lazy { CloudPlayerService() }

    // global cache data
    private val roomMap = mutableMapOf<String, ShowRoomDetailModel>()

    override fun destroy() {
        roomMap.clear()
    }

    override fun getRoomList(
        success: (List<ShowRoomDetailModel>) -> Unit,
        error: ((Exception) -> Unit)?
    ) {
        success.invoke(mockAppendRobotRooms())
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

    private fun mockAppendRobotRooms(): List<ShowRoomDetailModel> {
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
            val robotRoomId = robotRoomIds[i % 6]
            val robotId = robotRoomId % 10
            var interactionRoomName = ""
            val interactionStatus = if ((0..1).random() % 2 == 0) ShowInteractionStatus.pking.value else ShowInteractionStatus.idle.value
            if (interactionStatus == ShowInteractionStatus.pking.value) {
                val robotRooms = ArrayList(kRobotVideoRoomIds)
                robotRooms.remove(robotRoomId)
                interactionRoomName = robotRooms[(0..4).random()].toString()
            }
            val roomInfo = ShowRoomDetailModel(
                robotRoomId.toString(), // roomId
                "Smooth $i", // roomName
                1,
                "1",
                kRobotUid.toString(),
                kRobotAvatars[(robotId - 1) % kRobotAvatars.size],
                "Robot $robotId",
                ShowRoomStatus.activity.value,
                interactionStatus,
                interactionRoomName,
                kRobotUid.toString(),
                0.0,
                0.0
            )
            roomMap[roomInfo.roomId] = roomInfo
            retRoomList.add(roomInfo)
        }
        return retRoomList
    }
}