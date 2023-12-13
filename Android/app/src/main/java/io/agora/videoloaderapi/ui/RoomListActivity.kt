package io.agora.videoloaderapi.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import io.agora.videoloaderapi.*
import io.agora.videoloaderapi.databinding.ShowRoomItemBinding
import io.agora.videoloaderapi.databinding.ShowRoomListActivityBinding
import io.agora.videoloaderapi.rtc.RtcEngineInstance
import io.agora.videoloaderapi.service.ShowRoomDetailModel
import io.agora.videoloaderapi.service.ShowServiceProtocol
import io.agora.videoloaderapi.utils.TokenGenerator
import io.agora.videoloaderapi.widget.BindingSingleAdapter
import io.agora.videoloaderapi.widget.BindingViewHolder

class RoomListActivity : AppCompatActivity() {

    private val mBinding by lazy { ShowRoomListActivityBinding.inflate(LayoutInflater.from(this)) }
    private lateinit var mRoomAdapter: BindingSingleAdapter<ShowRoomDetailModel, ShowRoomItemBinding>
    private val mService by lazy { ShowServiceProtocol.getImplInstance() }
    private val mRtcVideoSwitcher by lazy { VideoLoader.getImplInstance(mRtcEngine) }
    private val mRtcEngine by lazy { RtcEngineInstance.rtcEngine }

    private val roomDetailModelList = mutableListOf<ShowRoomDetailModel>()

    private var isFirstLoad = true
    private var onRoomListScrollEventHandler: OnRoomListScrollEventHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        //启动机器人
        mService.startCloudPlayer()
        //获取万能token
        fetchUniversalToken ({
            val roomList = arrayListOf<VideoLoader.RoomInfo>( )
            roomDetailModelList.forEach { room ->
                roomList.add(
                    VideoLoader.RoomInfo(
                        room.roomId,
                        arrayListOf(
                            VideoLoader.AnchorInfo(
                                room.roomId,
                                room.ownerId.toInt(),
                                RtcEngineInstance.generalToken()
                            )
                        )
                    )
                )
            }
            onRoomListScrollEventHandler?.updateRoomList(roomList)
        })
        initView()
    }

    private fun initView() {

        onRoomListScrollEventHandler = object: OnRoomListScrollEventHandler(mRtcEngine, RtcEngineInstance.localUid()) {}

        mRoomAdapter = object : BindingSingleAdapter<ShowRoomDetailModel, ShowRoomItemBinding>() {
            override fun onBindViewHolder(
                holder: BindingViewHolder<ShowRoomItemBinding>,
                position: Int
            ) {
                updateRoomItem(mDataList, position, holder.binding, getItem(position) ?: return)
            }
        }
        mBinding.rvRooms.adapter = mRoomAdapter
        mBinding.rvRooms.addOnScrollListener(onRoomListScrollEventHandler as OnRoomListScrollEventHandler)

        mBinding.smartRefreshLayout.setEnableLoadMore(false)
        mBinding.smartRefreshLayout.setEnableRefresh(true)
        mBinding.smartRefreshLayout.setOnRefreshListener {
            mService.getRoomList(
                success = {
                    roomDetailModelList.clear()
                    roomDetailModelList.addAll(it)
                    if (isFirstLoad) {
                        val roomList = arrayListOf<VideoLoader.RoomInfo>( )
                        it.forEach { room ->
                            roomList.add(
                                VideoLoader.RoomInfo(
                                    room.roomId,
                                    arrayListOf(
                                        VideoLoader.AnchorInfo(
                                            room.roomId,
                                            room.ownerId.toInt(),
                                            RtcEngineInstance.generalToken()
                                        )
                                    )
                                )
                            )
                        }
                        onRoomListScrollEventHandler?.updateRoomList(roomList)

                        isFirstLoad = false
                    }
                    updateList(it)
                },
                error = {
                    updateList(emptyList())
                }
            )
        }
        mBinding.smartRefreshLayout.autoRefresh()
    }

    private fun updateList(data: List<ShowRoomDetailModel>) {
        mBinding.rvRooms.isVisible = data.isNotEmpty()
        mRoomAdapter.resetAll(data)

        mBinding.smartRefreshLayout.finishRefresh()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun updateRoomItem(
        list: List<ShowRoomDetailModel>,
        position: Int,
        binding: ShowRoomItemBinding,
        roomInfo: ShowRoomDetailModel
    ) {
        binding.tvRoomName.text = roomInfo.roomName
        binding.tvRoomId.text = getString(R.string.show_room_id, roomInfo.roomId)
        binding.ivCover.setImageResource(roomInfo.getThumbnailIcon())

        val onTouchEventHandler = object : OnLiveRoomItemTouchEventHandler(
            mRtcEngine,
            VideoLoader.RoomInfo(
                roomInfo.roomId,
                arrayListOf(
                    VideoLoader.AnchorInfo(
                        roomInfo.roomId,
                        roomInfo.ownerId.toInt(),
                        RtcEngineInstance.generalToken()
                    )
                )
            ),
            RtcEngineInstance.localUid()) {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                val isRoomOwner = roomInfo.ownerId == RtcEngineInstance.localUid().toString()
                if (isRoomOwner) {
                    if (event!!.action == MotionEvent.ACTION_UP) {
                        //ToastUtils.showToast("检测到上次直播异常退出，请重新开播")
                    }
                } else {
                    when (event!!.action) {
                        MotionEvent.ACTION_DOWN -> {
                            if (RtcEngineInstance.generalToken() == "") {
                                fetchUniversalToken({
                                }, {
                                    //ToastUtils.showToast("Fetch Token Failed")
                                })
                            } else {
                                super.onTouch(v, event)
                                mService.startCloudPlayer()
                            }
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            super.onTouch(v, event)
                        }
                        MotionEvent.ACTION_UP -> {
                            if (RtcEngineInstance.generalToken() != "") {
                                super.onTouch(v, event)
                                goLiveDetailActivity(list, position, roomInfo)
                            }
                        }
                    }
                }
                return true
            }

            override fun onRequireRenderVideo(info: VideoLoader.AnchorInfo): VideoLoader.VideoCanvasContainer? {
                Log.d("RoomListActivity", "onRequireRenderVideo")
                return null
            }
        }
        binding.root.setOnTouchListener(onTouchEventHandler)
    }

    private fun goLiveDetailActivity(list: List<ShowRoomDetailModel>, position: Int, roomInfo: ShowRoomDetailModel) {
        // 进房前设置一些必要的设置
        when (AgoraApplication.the()?.uiMode) {
            AGUIType.ViewPager -> {
                LiveViewPagerActivity.launch(
                    this,
                    ArrayList(list),
                    position,
                    roomInfo.ownerId != RtcEngineInstance.localUid().toString()
                )
            }
            AGUIType.RecycleView -> {
                LiveRecycleViewActivity.launch(
                    this,
                    ArrayList(list),
                    position,
                    roomInfo.ownerId != RtcEngineInstance.localUid().toString()
                )
            }
            else -> {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mService.destroy()
        mRtcVideoSwitcher.cleanCache()
        RtcEngineInstance.destroy()
        VideoLoader.release()
        RtcEngineInstance.setupGeneralToken("")
    }

    // 获取万能token
    private fun fetchUniversalToken(
        success: () -> Unit,
        error: ((Exception?) -> Unit)? = null
    ) {
        val localUId = RtcEngineInstance.localUid()
        TokenGenerator.generateToken("", localUId.toString(),
            TokenGenerator.TokenGeneratorType.token007,
            TokenGenerator.AgoraTokenType.rtc,
            success = {
                Log.d("RoomListActivity", "generateToken success：$it， uid：$localUId")
                RtcEngineInstance.setupGeneralToken(it)
                success.invoke()
            },
            failure = {
                Log.e("RoomListActivity", "generateToken failure：$it")
                error?.invoke(it)
            })
    }
}