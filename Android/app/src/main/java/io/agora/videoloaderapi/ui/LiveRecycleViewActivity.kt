package io.agora.videoloaderapi.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.SnapHelper
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import io.agora.videoloaderapi.*
import io.agora.videoloaderapi.databinding.ShowLiveRecycleViewActivityBinding
import io.agora.videoloaderapi.databinding.ShowLiveRecycleViewItemBinding
import io.agora.videoloaderapi.rtc.RtcEngineInstance
import io.agora.videoloaderapi.service.ShowInteractionStatus
import io.agora.videoloaderapi.service.ShowRoomDetailModel
import io.agora.videoloaderapi.service.ShowRoomStatus
import io.agora.videoloaderapi.utils.RunnableWithDenied
import io.agora.videoloaderapi.widget.BaseViewBindingActivity
import io.agora.videoloaderapi.widget.BindingSingleAdapter
import io.agora.videoloaderapi.widget.BindingViewHolder


class LiveRecycleViewActivity : BaseViewBindingActivity<ShowLiveRecycleViewActivityBinding>() {
    private val tag = "LiveRecycleViewActivity"

    companion object {
        private const val EXTRA_ROOM_DETAIL_INFO_LIST = "roomDetailInfoList"
        private const val EXTRA_ROOM_DETAIL_INFO_LIST_SELECTED_INDEX =
            "roomDetailInfoListSelectedIndex"
        private const val EXTRA_ROOM_DETAIL_INFO_LIST_SCROLLABLE = "roomDetailInfoListScrollable"

        fun launch(
            context: Context,
            roomDetail: ArrayList<ShowRoomDetailModel>,
            selectedIndex: Int,
            scrollable: Boolean
        ) {
            context.startActivity(Intent(context, LiveRecycleViewActivity::class.java).apply {
                putExtra(EXTRA_ROOM_DETAIL_INFO_LIST, roomDetail)
                putExtra(EXTRA_ROOM_DETAIL_INFO_LIST_SELECTED_INDEX, selectedIndex)
                putExtra(EXTRA_ROOM_DETAIL_INFO_LIST_SCROLLABLE, scrollable)
            })
        }
    }

    private val mRoomInfoList by lazy {
        intent.getParcelableArrayListExtra<ShowRoomDetailModel>(
            EXTRA_ROOM_DETAIL_INFO_LIST
        )!!
    }

    private val POSITION_NONE = -1
    private var onPageScrollEventHandler: OnPageScrollEventHandler2? = null
    private var toggleVideoRun: RunnableWithDenied? = null
    private var toggleAudioRun: Runnable? = null
    private var viewArrayList = SparseArray<View>()

    override fun getPermissions() {
        if (toggleVideoRun != null) {
            toggleVideoRun?.run()
            toggleVideoRun = null
        }
        if (toggleAudioRun != null) {
            toggleAudioRun?.run()
            toggleAudioRun = null
        }
    }

    override fun onPermissionDined(permission: String?) {
        if (toggleVideoRun != null && permission == Manifest.permission.CAMERA) {
            toggleVideoRun?.onDenied()
        }
    }

    override fun getViewBinding(inflater: LayoutInflater): ShowLiveRecycleViewActivityBinding {
        return  ShowLiveRecycleViewActivityBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val selectedRoomIndex = intent.getIntExtra(EXTRA_ROOM_DETAIL_INFO_LIST_SELECTED_INDEX, 0)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = RecyclerView.VERTICAL // 设置滑动方向为垂直
        binding.recyclerView.layoutManager = layoutManager

        val snapHelper: SnapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.recyclerView)

        // 在此处添加直播间的数据
        val adapter = object :
            BindingSingleAdapter<ShowRoomDetailModel, ShowLiveRecycleViewItemBinding>() {

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): BindingViewHolder<ShowLiveRecycleViewItemBinding> {
                val viewHolder = super.onCreateViewHolder(parent, viewType)
                viewHolder.itemView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                return viewHolder
            }

            override fun onBindViewHolder(
                holder: BindingViewHolder<ShowLiveRecycleViewItemBinding>,
                position: Int
            ) {
                Log.d(tag, "onBindViewHolder, position: $position")
                viewArrayList[position] = holder.binding.root
                val actualPosition = position % mRoomInfoList.size
                updateRoomItem(position, holder.binding, getItem(actualPosition) ?: return)
            }

            override fun getItemCount(): Int {
                return Int.MAX_VALUE
            }
        }

        binding.recyclerView.adapter = adapter

        val needPreJoin = AgoraApplication.the()?.needPreJoin == true
        onPageScrollEventHandler = object : OnPageScrollEventHandler2(layoutManager, RtcEngineInstance.rtcEngine, RtcEngineInstance.localUid(), needPreJoin,
            AgoraApplication.the()?.sliceMode!!) {
            override fun onPageStartLoading(position: Int) {
                Log.d(tag, "onPageStartLoading, position: $position")
            }

            override fun onPageLoaded(position: Int) {
                Log.d(tag, "onPageLoaded, position: $position")
            }

            override fun onPageLeft(position: Int) {
                Log.d(tag, "onPageLeft, position: $position")
            }

            override fun onRequireRenderVideo(
                position: Int,
                info: VideoLoader.AnchorInfo
            ): VideoLoader.VideoCanvasContainer? {
                Log.d(tag, "onRequireRenderVideo, position: $position}")
                val mRoomInfo = mRoomInfoList[position % mRoomInfoList.size]
                if ((mRoomInfo.interactStatus == ShowInteractionStatus.pking.value)) {
                    if (info.channelId == mRoomInfo.roomId) {
                        return VideoLoader.VideoCanvasContainer(
                            this@LiveRecycleViewActivity,
                            viewArrayList[position].findViewById(R.id.iBroadcasterAView),
                            mRoomInfo.ownerId.toInt()
                        )
                    } else if (info.channelId == mRoomInfo.interactRoomName) {
                        return VideoLoader.VideoCanvasContainer(
                            this@LiveRecycleViewActivity,
                            viewArrayList[position].findViewById(R.id.iBroadcasterBView),
                            mRoomInfo.interactOwnerId.toInt()
                        )
                    }
                }

                return VideoLoader.VideoCanvasContainer(
                    this@LiveRecycleViewActivity,
                    viewArrayList[position].findViewById(R.id.videoLinkingLayout),
                    mRoomInfo.ownerId.toInt()
                )
            }
        }
        binding.recyclerView.addOnScrollListener(onPageScrollEventHandler as OnScrollListener)
        layoutManager.scrollToPositionWithOffset(selectedRoomIndex, 0)
        adapter.resetAll(mRoomInfoList)

        val list = ArrayList<VideoLoader.RoomInfo>()
        mRoomInfoList.forEach {
            val anchorList = arrayListOf(
                VideoLoader.AnchorInfo(
                    it.roomId,
                    it.ownerId.toInt(),
                    RtcEngineInstance.generalToken()
                )
            )
            if (it.interactStatus == ShowInteractionStatus.pking.value) {
                anchorList.add(VideoLoader.AnchorInfo(
                    it.interactRoomName,
                    it.interactOwnerId.toInt(),
                    RtcEngineInstance.generalToken()
                ))
            }
            list.add(
                VideoLoader.RoomInfo(it.roomId, anchorList)
            )
        }
        onPageScrollEventHandler?.updateRoomList(list)
    }

    override fun finish() {
        RtcEngineInstance.cleanCache()
        super.finish()
    }

    private fun updateRoomItem(
        position: Int,
        binding: ShowLiveRecycleViewItemBinding,
        roomInfo: ShowRoomDetailModel,
    ) {
        val topLayout = binding.topLayout
        Glide.with(this)
            .load(roomInfo.ownerAvatar)
            .error(R.mipmap.show_default_avatar)
            .into(topLayout.ivOwnerAvatar)
        topLayout.tvRoomName.text = roomInfo.roomName
        topLayout.tvRoomId.text = getString(R.string.show_room_id, roomInfo.roomId)
        topLayout.ivClose.setOnClickListener { onBackPressed() }

        when (roomInfo.interactStatus) {
            ShowInteractionStatus.idle.value -> {
                binding.videoPKLayout.root.isVisible = false
                binding.videoLinkingLayout.root.isVisible = true
            }
            ShowInteractionStatus.pking.value -> {
                binding.topLayout.root.bringToFront()
                binding.videoLinkingLayout.root.isVisible = false
                binding.videoPKLayout.root.isVisible = true
            }
        }

        val anchorList = arrayListOf(
            VideoLoader.AnchorInfo(
                roomInfo.roomId,
                roomInfo.ownerId.toInt(),
                RtcEngineInstance.generalToken()
            )
        )
        if (roomInfo.interactStatus == ShowInteractionStatus.pking.value) {
            anchorList.add(VideoLoader.AnchorInfo(
                roomInfo.interactRoomName,
                roomInfo.interactOwnerId.toInt(),
                RtcEngineInstance.generalToken()
            ))
        }
        onPageScrollEventHandler?.onRoomCreated(position,
            VideoLoader.RoomInfo(
                roomInfo.roomId,
                anchorList
            ))
    }
}