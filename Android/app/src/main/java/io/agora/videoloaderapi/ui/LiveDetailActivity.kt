package io.agora.videoloaderapi.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import io.agora.videoloaderapi.AgoraApplication
import io.agora.videoloaderapi.OnPageScrollEventHandler
import io.agora.videoloaderapi.VideoLoader
import io.agora.videoloaderapi.databinding.ShowLiveDetailActivityBinding
import io.agora.videoloaderapi.rtc.RtcEngineInstance
import io.agora.videoloaderapi.service.ShowInteractionStatus
import io.agora.videoloaderapi.service.ShowRoomDetailModel
import io.agora.videoloaderapi.utils.RunnableWithDenied
import io.agora.videoloaderapi.utils.TokenGenerator
import io.agora.videoloaderapi.widget.BaseViewBindingActivity


class LiveDetailActivity : BaseViewBindingActivity<ShowLiveDetailActivityBinding>(),
    LiveDetailFragment.OnMeLinkingListener {
    private val tag = "LiveDetailActivity"

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
            context.startActivity(Intent(context, LiveDetailActivity::class.java).apply {
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
    private val mScrollable by lazy {
        intent.getBooleanExtra(
            EXTRA_ROOM_DETAIL_INFO_LIST_SCROLLABLE,
            true
        )
    }

    private val POSITION_NONE = -1
    private val vpFragments = SparseArray<LiveDetailFragment>()
    private var currLoadPosition = POSITION_NONE

    private var onPageScrollEventHandler: OnPageScrollEventHandler? = null

    private var toggleVideoRun: RunnableWithDenied? = null
    private var toggleAudioRun: Runnable? = null

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

    override fun onMeLinking(isLinking: Boolean) {
        // 连麦观众禁止切换房间
        binding.viewPager2.isUserInputEnabled = !isLinking
    }

    override fun getViewBinding(inflater: LayoutInflater): ShowLiveDetailActivityBinding {
        return  ShowLiveDetailActivityBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.viewPager2) { _: View?, insets: WindowInsetsCompat ->
            val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.viewPager2.setPaddingRelative(inset.left, 0, inset.right, inset.bottom)
            WindowInsetsCompat.CONSUMED
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val selectedRoomIndex = intent.getIntExtra(EXTRA_ROOM_DETAIL_INFO_LIST_SELECTED_INDEX, 0)

        val needPreJoin = AgoraApplication.the()?.needPreJoin == true
        onPageScrollEventHandler = object : OnPageScrollEventHandler(this, RtcEngineInstance.rtcEngine, RtcEngineInstance.localUid(), needPreJoin,
            AgoraApplication.the()?.sliceMode!!
        ) {
            override fun onPageScrollStateChanged(state: Int) {
                when (state) {
                    ViewPager2.SCROLL_STATE_SETTLING -> binding.viewPager2.isUserInputEnabled = false
                    ViewPager2.SCROLL_STATE_IDLE -> binding.viewPager2.isUserInputEnabled = true
                    ViewPager2.SCROLL_STATE_DRAGGING -> {
                        // TODO 暂不支持
                    }
                }
                super.onPageScrollStateChanged(state)
            }

            override fun onPageStartLoading(position: Int) {
                Log.d(tag, "onPageLoad, position:$position")
                vpFragments[position]?.startLoadPageSafely()
            }

            override fun onPageLoaded(position: Int) {
                Log.d(tag, "onPageReLoad, position:$position")
                vpFragments[position]?.onPageLoaded()
            }

            override fun onPageLeft(position: Int) {
                Log.d(tag, "onPageHide, position:$position")
                vpFragments[position]?.stopLoadPage(true)
            }

            override fun onRequireRenderVideo(
                position: Int,
                info: VideoLoader.AnchorInfo
            ): VideoLoader.VideoCanvasContainer? {
                Log.d(tag, "onRequireRenderVideo, position:$position")
                return vpFragments[position]?.initAnchorVideoView(info)
            }
        }

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
                    it.ownerId.toInt(),
                    RtcEngineInstance.generalToken()
                ))
            }
            list.add(
                VideoLoader.RoomInfo(it.roomId, anchorList)
            )
        }
        onPageScrollEventHandler?.updateRoomList(list)

        // 设置vp当前页面外的页面数
        binding.viewPager2.offscreenPageLimit = 1
        val fragmentAdapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = if (mScrollable) Int.MAX_VALUE else 1

            override fun createFragment(position: Int): Fragment {
                val roomInfo = if (mScrollable) {
                    mRoomInfoList[position % mRoomInfoList.size]
                } else {
                    mRoomInfoList[selectedRoomIndex]
                }
                return LiveDetailFragment.newInstance(
                    roomInfo,
                    onPageScrollEventHandler as OnPageScrollEventHandler, position
                ).apply {
                    Log.d(tag, "position：$position, room:${roomInfo.roomId}")
                    vpFragments.put(position, this)
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
                            roomInfo.ownerId.toInt(),
                            RtcEngineInstance.generalToken()
                        ))
                    }
                    onPageScrollEventHandler?.onRoomCreated(position,
                        VideoLoader.RoomInfo(
                            roomInfo.roomId,
                            anchorList
                        ),position == binding.viewPager2.currentItem)
                }
            }
        }
        binding.viewPager2.adapter = fragmentAdapter
        binding.viewPager2.isUserInputEnabled = mScrollable

        if (mScrollable) {
            binding.viewPager2.registerOnPageChangeCallback(onPageScrollEventHandler as OnPageChangeCallback)
            binding.viewPager2.setCurrentItem(
                Int.MAX_VALUE / 2 - Int.MAX_VALUE / 2 % mRoomInfoList.size + selectedRoomIndex,
                false
            )
        } else {
            currLoadPosition = 0
        }
    }

    override fun finish() {
        if (!mScrollable) {
            vpFragments[currLoadPosition]?.stopLoadPage(false)
        } else if (onPageScrollEventHandler != null) {
            vpFragments[onPageScrollEventHandler!!.getCurrentRoomPosition()]?.stopLoadPage(false)
        }

        TokenGenerator.expireSecond = -1
        RtcEngineInstance.cleanCache()
        super.finish()
    }
}