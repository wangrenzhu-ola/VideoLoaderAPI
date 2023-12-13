package io.agora.videoloaderapi

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.SparseArray
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import io.agora.rtc2.*
import io.agora.rtc2.internal.Logging
import java.util.*
import kotlin.collections.ArrayList

/**
 * 直播间 item 触摸事件
 * @param mRtcEngine RtcEngineEx 对象
 * @param localUid 观众 uid
 * @param needPreJoin 房间是否需要 preJoin
 * @param videoScrollMode 视频出图模式
 */
abstract class OnPageScrollEventHandler2 constructor(
    private val layoutManager: LinearLayoutManager,
    private val mRtcEngine: RtcEngineEx,
    private val localUid: Int,
    private val needPreJoin: Boolean,
    private val videoScrollMode: AGSlicingType
) : RecyclerView.OnScrollListener() {
    private val tag = "OnPageScrollHandler2"
    private val videoSwitcher by lazy { VideoLoader.getImplInstance(mRtcEngine) }
    private val roomList = SparseArray<VideoLoader.RoomInfo>()

    fun cleanCache() {
        mainHandler.removeCallbacksAndMessages(null)
        roomsForPreloading.clear()
        roomsJoined.clear()
    }

    // ViewPager2.OnPageChangeCallback()
    private val POSITION_NONE = -1
    private var currLoadPosition = POSITION_NONE
    private var preLoadPosition = POSITION_NONE
    private var lastVisibleItemCount = 0

    private val roomsForPreloading = Collections.synchronizedList(mutableListOf<VideoLoader.RoomInfo>())
    private val roomsJoined = Collections.synchronizedList(mutableListOf<VideoLoader.RoomInfo>())

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private var isFirst = true

    fun onRoomCreated(position: Int, info: VideoLoader.RoomInfo) {
        roomList.put(position, info)
        if (isFirst) {
            isFirst = false
            info.anchorList.forEach {
                mRtcEngine.adjustUserPlaybackSignalVolumeEx(it.anchorUid, 100, RtcConnection(it.channelId, localUid))
            }
            mainHandler.postDelayed({
                roomsJoined.add(info)
                pageLoaded(position, info)
                preJoinChannels()
                preLoadPosition = POSITION_NONE
                currLoadPosition = layoutManager.findFirstVisibleItemPosition()
                lastVisibleItemCount = layoutManager.childCount
            }, 200)
        }
    }

    fun updateRoomList(list: ArrayList<VideoLoader.RoomInfo>) {
        roomsForPreloading.addAll(list)
    }

    fun updateRoomInfo(position: Int, info: VideoLoader.RoomInfo) {
        if (info.roomId != roomList[position].roomId) return
        val oldAnchorList = roomList[position].anchorList
        val newAnchorList = info.anchorList
        newAnchorList.forEach { newInfo ->
            videoSwitcher.switchAnchorState(AnchorState.JOINED, newInfo, localUid)
            onRequireRenderVideo(position, newInfo)?.let { canvas ->
                videoSwitcher.renderVideo(
                    newInfo,
                    localUid,
                    canvas
                )
            }
        }

        oldAnchorList.forEach { oldInfo ->
            if (newAnchorList.none { new -> new.channelId == oldInfo.channelId }) {
                videoSwitcher.switchAnchorState(AnchorState.IDLE, oldInfo, localUid)
            }
        }

        val roomInfo = roomsForPreloading.filter { it.roomId == info.roomId }.getOrNull(0) ?: return
        val index = roomsForPreloading.indexOf(roomInfo)
        roomsForPreloading[index] = info
        roomList[position] = info
    }

    fun getCurrentRoomPosition(): Int {
        return currLoadPosition
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)

        if (newState == RecyclerView.SCROLL_STATE_IDLE) { // 滚动停止时
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
            val visibleItemCount = layoutManager.childCount

            // 检查哪些item离开了视图
            for (i in currLoadPosition until currLoadPosition + lastVisibleItemCount) {
                if (i < firstVisibleItemPosition || i >= firstVisibleItemPosition + visibleItemCount) {
                    hideChannel(roomList[i] ?: return)
                    onPageLeft(i)
                }
            }

            // 当前停留的页面
            if (currLoadPosition != firstVisibleItemPosition) {
                startAudio(roomList[firstVisibleItemPosition] ?: return)
                roomsJoined.add(roomList[firstVisibleItemPosition] ?: return)
                preJoinChannels()
                pageLoaded(firstVisibleItemPosition, roomList[firstVisibleItemPosition])
            }

            currLoadPosition = firstVisibleItemPosition
            lastVisibleItemCount = visibleItemCount
            preLoadPosition = POSITION_NONE
        }
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        // 检查新的页面是否开始出现
        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
        //Log.d("hugo", "onScrolled, currLoadPosition：currLoadPosition firstVisibleItemPosition: $firstVisibleItemPosition, lastVisibleItemPosition: $lastVisibleItemPosition")
        // 和上次第一个可见的item不同，认为是新的页面开始加载
        if (firstVisibleItemPosition != currLoadPosition && preLoadPosition != firstVisibleItemPosition) {
            // 下滑
            preLoadPosition = firstVisibleItemPosition
        } else if (lastVisibleItemPosition != currLoadPosition && preLoadPosition != lastVisibleItemPosition) {
            // 上滑
            preLoadPosition = lastVisibleItemPosition
        } else {
            return
        }

        joinChannel(preLoadPosition, roomList[preLoadPosition] ?: return, localUid, false)
        onPageStartLoading(preLoadPosition)
    }

    // OnPageStateEventHandler
    abstract fun onPageStartLoading(position: Int)

    abstract fun onPageLoaded(position: Int)

    abstract fun onPageLeft(position: Int)

    abstract fun onRequireRenderVideo(position: Int, info: VideoLoader.AnchorInfo): VideoLoader.VideoCanvasContainer?

    // ------------------------ inner ---------------------------
    private fun joinChannel(position: Int, roomInfo: VideoLoader.RoomInfo, uid: Int, isCurrentItem: Boolean) {
        Logging.d(tag, "joinChannel roomInfo=$roomInfo")

        roomInfo.anchorList.forEach { anchorInfo ->
            videoSwitcher.switchAnchorState(AnchorState.JOINED_WITHOUT_AUDIO, anchorInfo, uid)
            if (videoScrollMode == AGSlicingType.VISIBLE || isCurrentItem) {
                onRequireRenderVideo(position, anchorInfo)?.let {
                    videoSwitcher.renderVideo(
                        anchorInfo,
                        localUid,
                        it
                    )
                }
            }

            // 打点
            mRtcEngine.startMediaRenderingTracingEx(RtcConnection(anchorInfo.channelId, localUid))
        }
    }

    private fun hideChannel(roomInfo: VideoLoader.RoomInfo) {
        Logging.d(tag, "switchRoomState, hideChannel: $roomInfo")
        roomsJoined.removeIf { it.roomId == roomInfo.roomId }
        val currentRoom = roomsJoined.firstOrNull() ?: return
        roomInfo.anchorList.forEach {
            if (needPreJoin && currentRoom.anchorList.none { joined -> joined.channelId == it.channelId }) {
                videoSwitcher.switchAnchorState(AnchorState.PRE_JOINED, it, localUid)
            } else if (currentRoom.anchorList.none { joined -> joined.channelId == it.channelId }) {
                videoSwitcher.switchAnchorState(AnchorState.IDLE, it, localUid)
            }
        }
    }

    private fun startAudio(roomInfo: VideoLoader.RoomInfo) {
        roomInfo.anchorList.forEach {
            videoSwitcher.switchAnchorState(AnchorState.JOINED, it, localUid)
        }
    }

    private fun preJoinChannels() {
        val size = roomsForPreloading.size
        val currentRoom = roomsJoined.firstOrNull() ?: return
        val index =
            roomsForPreloading.indexOfFirst { it.roomId == currentRoom.roomId }
        Logging.d(tag, "switchRoomState, index: $index, connectionsJoined:$roomsJoined")
        Logging.d(tag, "switchRoomState, roomsForPreloading: $roomsForPreloading")

        // joined房间的上下两个房间
        val connPreLoaded = mutableListOf<VideoLoader.RoomInfo>()
        for (i in (index - 1)..(index + 3 / 2)) {
            if (i == index) {
                continue
            }
            // workaround
            if (size == 0) {
                return
            }
            val realIndex = (if (i < 0) size + i else i) % size
            if (realIndex < 0 || realIndex >= size) {
                continue
            }
            val conn = roomsForPreloading[realIndex]
            if (roomsJoined.any { it.roomId == conn.roomId }) {
                continue
            }
            if (videoSwitcher.getRoomState(conn.roomId, localUid) != AnchorState.PRE_JOINED) {
                Logging.d(tag, "switchRoomState, getRoomState: $roomsForPreloading")
                videoSwitcher.preloadAnchor(conn.anchorList, localUid)
                conn.anchorList.forEach {
                    if (needPreJoin && currentRoom.anchorList.none { joined -> joined.channelId == it.channelId }) {
                        videoSwitcher.switchAnchorState(AnchorState.PRE_JOINED, it, localUid)
                    }
                }
            }
            connPreLoaded.add(conn)
        }

        Logging.d(tag, "switchRoomState, connPreLoaded: $connPreLoaded ")
        // 非preJoin房间需要退出频道
        roomsForPreloading.forEach { room ->
            if (needPreJoin && videoSwitcher.getRoomState(room.roomId, localUid) == AnchorState.PRE_JOINED && connPreLoaded.none {room.roomId == it.roomId}) {
                Logging.d(tag, "switchRoomState, remove: $room ")
                room.anchorList.forEach {
                    if (currentRoom.anchorList.none { joined -> joined.channelId == it.channelId }) {
                        videoSwitcher.switchAnchorState(AnchorState.IDLE, it, localUid)
                    }
                }
            } else if (!needPreJoin && videoSwitcher.getRoomState(room.roomId, localUid) != AnchorState.IDLE && roomsJoined.none {room.roomId == it.roomId}) {
                Logging.d(tag, "switchRoomState, remove: $room ")
                room.anchorList.forEach {
                    if (currentRoom.anchorList.none { joined -> joined.channelId == it.channelId }) {
                        videoSwitcher.switchAnchorState(AnchorState.IDLE, it, localUid)
                    }
                }
            }
        }
    }

    private fun pageLoaded(position: Int, roomInfo: VideoLoader.RoomInfo) {
        onPageLoaded(position)
        if (videoScrollMode == AGSlicingType.END_SCROLL) {
            roomInfo.anchorList.forEach { anchorInfo ->
                onRequireRenderVideo(position, anchorInfo)?.let {
                    videoSwitcher.renderVideo(
                        anchorInfo,
                        localUid,
                        it
                    )
                }
            }
        }
    }
}