# VideoLoaderAPI

*__其他语言版本：__  [__简体中文__](README.zh.md)*

The VideoLoaderAPI is an API for achieving quick-join and quick-switch capabilities in video live streaming. This module aims to help video streaming developers integrate Agora's quick-join and quick-switch features more quickly.

### 1. Quick Start

This section mainly describes how to quickly run the VideoLoaderAPI Demo

#### 1.1 Environment Preparation

- Minimum compatibility with Android 5.0 (SDK API Level 21)
- Android Studio 3.5 or above
- Android devices running Android 5.0 or above

#### 1.2 Running the Sample

1. Follow [The Account Document](https://docs.agora.io/en/video-calling/reference/manage-agora-account) to get the **App ID** and **App Certificate(if enable token)**.
2. Follow [The Restfull Document](https://docs.agora.io/en/video-calling/reference/restful-authentication) to get the **Customer ID** and **Customer Secret**.
3. Follow [The Media Pull Document](https://docs.agora.io/en/media-pull/get-started/enable-media-pull) to enable media pull for cloud player.
4. Open the `Android` project and fill in properties got above to the root [gradle.properties](../gradle.properties) file.

```
# RTM RTC SDK key Config
AGORA_APP_ID=<Your Agora App ID>
AGORA_APP_CERTIFICATE=<Your Agora App Certificate(if enable token)>

# Cloud Player Config
CLOUD_PLAYER_KEY=<Your Agora Customer ID>
CLOUD_PLAYER_SECRET=<Your Agora Customer Secret>
```

#### 3. Project Introduction

- <mark>1. Overview</mark>
> The VideoLoaderAPI is an API for achieving quick-join and quick-switch capabilities in video live streaming. This module aims to help video streaming developers integrate Agora's quick-join and quick-switch features more quickly.
>
>  <mark>2. Function Introduction</mark>

> The VideoLoaderAPI Demo currently covers the following functions:
>
> - Selection of preload mode and video rendering mode 
>
>   Related code reference: [MainActivity](http://app/src/main/java/io/agora/videoloaderapi/ui/MainActivity.kt)
>
> - Quick join
>
>   Related code reference: [RoomListActivity](app/src/main/java/io/agora/videoloaderapi/ui/RoomListActivity.kt) 
>
> - Quick switch
>
>   - ViewPager
>   
>     code reference: The implementation of `OnPageChangeCallback` in [LiveViewPagerActivity](app/src/main/java/io/agora/videoloaderapi/ui/LiveViewPagerActivity.kt)
>   
>   - RecycleView
>   
>     code reference: The implementation of `OnScrollListener` in [LiveRecycleViewActivity](app/src/main/java/io/agora/videoloaderapi/ui/LiveRecycleViewActivity.kt)



### 2. VideoLoaderAPI Usage Tutorial

#### 2.0 File Introduction

Related code reference：[VideoLoaderAPI](lib_videoloaderapi/src/main/java/io/agora/videoloaderapi/OnLiveRoomItemTouchEventHandler.kt) 

* OnLiveRoomItemTouchEventHandler: event handling module for quick join
* OnRoomListScrollEventHandler: event handling module for room list scrolling
* OnPageScrollEventHandler: event handling module for switching between live rooms
* VideoLoader: the internal class used to handle channel management

#### 2.1 Quick Join

##### 2.1.1 Definition

The process of an audience member clicking on a live room from the room list and quickly seeing the live video in the room is defined as "Quick Join".

##### 2.1.2 Best Practices for Quick Join

1. Preloading channels on the room list page for channels in the field of view:

   - If the total number of rooms is less than 20, preloading channels for all rooms is recommended.
- If the total number of rooms is more than 20, preloading channels for the first 20 rooms is recommended. When scrolling the room list, preloading channels for the rooms within the field of view after the scrolling ends is recommended.
2. Handling logic when clicking on a live room item:
   - When the user touches a live room item, join the channel and subscribe to the audio and video streams. (Mute audio if necessary)
   - When the user releases the touch, transition to the live room page with animation, and unmute audio.
   - If the user touches and then swipes away without completing a valid click, exit the channel.
3. When a live room item on the room list page is clicked, the live room page may not be created yet due to lazy loading. However, the RTC has already joined the channel and started pulling the stream, which may result in no rendered video view being set up for the SDK, possibly causing the first frame to be rendered slowly. The following solutions are recommended:
   - Use `setupRemoteVideo` to set up a view (View1) for the SDK immediately after joining the channel (joinChannelEx), and then add View1 to the video container area for display prior to handling other business logic (such as IM in the room).
   - Listen for the first frame rendered callback from the SDK, play a loading animation before the callback triggers, and set the live video as visible after the callback triggers.
4. Use universal tokens:
   - Universal tokens can save the time of fetching channel tokens before joining the channel.
   - Universal tokens have the risk of exposing the token, so whether to use them needs to be determined based on specific requirements.

##### 2.1.3 How to Use VideoLoaderAPI for Quick join

* **Bind the OnRoomListScrollEventHandler to the room list scroll events**

  ~~~
  private var onRoomListScrollEventHandler: OnRoomListScrollEventHandler? = null
  private fun initView() {
     onRoomListScrollEventHandler = object: OnRoomListScrollEventHandler(mRtcEngine, UserManager.getInstance().user.id.toInt()) {}
     // mBinding.rvRooms is the RecyclerView for the room list
     mBinding.rvRooms.addOnScrollListener(onRoomListScrollEventHandler as OnRoomListScrollEventHandler)
  }
  ~~~

* **Pass the room list information to the OnRoomListScrollEventHandler object in real time**

  The OnRoomListScrollEventHandler will preLoadChannel for the required rooms after intercepting the room list scroll events.

  ~~~
  onRoomListScrollEventHandler?.updateRoomList(roomList)
  ~~~

* **Bind the OnLiveRoomItemTouchEventHandler to the individual room item**

  After intercepting the touch events of the room item, the onTouchEventHandler will perform the corresponding join channel operation and throw the onRequireRenderVideo callback. You need to return the video container that needs to render the anchor's video in the onRequireRenderVideo callback. The VideoLoaderAPI will display the video view in this container.

  ~~~
  val onTouchEventHandler = object : OnLiveRoomItemTouchEventHandler(
              this,
              mRtcEngine,
              VideoLoader.RoomInfo(
                  roomInfo.roomId,
                  arrayListOf(VideoLoader.AnchorInfo(roomInfo.roomId, roomInfo.ownerId.toInt(), RtcEngineInstance.generalToken()))
              ),
              UserManager.getInstance().user.id.toInt()) {
                  override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                      when (event!!.action) {
                          MotionEvent.ACTION_UP -> {
                              // Jump to the room page here
                          }
                      }
                      return true
                  }
  
                  override fun onRequireRenderVideo(info: VideoLoader.AnchorInfo): VideoLoader.VideoCanvasContainer? {
                      // Set the best timing for the video view here, and return the video container that needs to render the anchor's view
                  }
              }
          // The binding.root is the view of the individual room item
          binding.root.setOnTouchListener(onTouchEventHandler)
      }
  ~~~

  

#### 2.2 Quick switch

##### 2.2.1 Definition

The process of an audience member starting to scroll up or down in a live room and quickly seeing the next live room picture is defined as "Quick switch".

##### 2.2.2 Best Practices for Quick switch

1. Preload Strategy:
   - Preload channels for up to 20 channels scrolled up or down in the list.
2. Preload + PreJoin Strategy:
   - Preload channels as mentioned above.
   - PreJoin: Join the RTC channel of the above and below rooms without subscribing to the audio and video streams. Subscribe to the audio and video streams of the target room at the appropriate time during scrolling.
   - The PreJoin strategy may incur additional charges due to joining additional channels, so use it carefully.
3. Use universal tokens:
   - Universal tokens can save the time of fetching channel tokens before joining the channel.
   - Universal tokens have the risk of exposing the token, so whether to use them needs to be determined based on specific requirements.
4. Rendering video frames: After subscribing to the video stream, immediately call `setupRemoteVideo` to set up the view to avoid missing the first I-frame decoding and slow rendering of the first frame.

##### 2.2.3 How to Use VideoLoaderAPI for Quick switch

The current scene-based transitioning module is designed for scrolling live rooms implemented with ViewPager2 + Fragment.

* **Bind the OnPageScrollEventHandler to the room list scroll events**

  * needPrejoin: Whether to enable the prejoin mode.
  * mode: The mode for video rendering. Currently, there are two modes: immediate rendering and rendering after the scrolling stops.

  ~~~
  val onPageScrollEventHandler = object : OnPageScrollEventHandler(
  				this, 
  				RtcEngineInstance.rtcEngine, 
  				UserManager.getInstance().user.id.toInt(), 
  				needPrejoin, 
  				mode
  		 ) {
              override fun onPageScrollStateChanged(state: Int) {
                  when(state){
                      ViewPager2.SCROLL_STATE_SETTLING -> binding.viewPager2.isUserInputEnabled = false
                      ViewPager2.SCROLL_STATE_IDLE -> binding.viewPager2.isUserInputEnabled = true
                  }
                  super.onPageScrollStateChanged(state)
              }
  
              override fun onPageStartLoading(position: Int) {
                  // The page starts to display
              }
  
              override fun onPageLoaded(position: Int) {
                  // The page has finished displaying
              }
  
              override fun onPageLeft(position: Int) {
                  // The page is hidden or left
              }
  
              override fun onRequireRenderVideo(
                  position: Int,
                  info: VideoLoader.AnchorInfo
              ): VideoLoader.VideoCanvasContainer? {
                  // Set the best timing for the video view, and return the video container that needs to render the anchor's view
              }
          }
  ~~~

* **Pass the created room information to the OnPageScrollEventHandler object in the FragmentStateAdapter**

  ~~~
  binding.viewPager2.offscreenPageLimit = 1
  val fragmentAdapter = object : FragmentStateAdapter(this) {
      override fun createFragment(position: Int): Fragment {
          return LiveDetailFragment.newInstance(roomInfo,
              onPageScrollEventHandler as OnPageScrollEventHandler, position
          ).apply {
              onPageScrollEventHandler?.onRoomCreated(position,
                  VideoLoader.RoomInfo(
                      roomInfo.roomId,
                      arrayListOf(VideoLoader.AnchorInfo(
                          roomInfo.roomId,
                          roomInfo.ownerId.toInt(),
                          RtcEngineInstance.generalToken()
                      )
                  )),position == binding.viewPager2.currentItem)
          }
      }
  }
  binding.viewPager2.adapter = fragmentAdapter
  ~~~
  
* **Pass the room list information to the OnPageScrollEventHandler object in real time**

  ~~~
  onPageScrollEventHandler?.updateRoomList(list)
  ~~~

* **Update the room information corresponding to a specific position**

  When you need to update the information of the room corresponding to the position:

  ~~~
  onPageScrollEventHandler?.updateRoomInfo(
      mPosition,
      VideoLoader.RoomInfo(
          mRoomInfo.roomId, arrayListOf(
              VideoLoader.AnchorInfo(
                  mRoomInfo.roomId,
                  mRoomInfo.ownerId.toInt(),
                  RtcEngineInstance.generalToken()
              ),
              VideoLoader.AnchorInfo(
                  interactionInfo!!.roomId,
                  interactionInfo!!.userId.toInt(),
                  RtcEngineInstance.generalToken()
              )
          )
      )
  )
  ~~~

