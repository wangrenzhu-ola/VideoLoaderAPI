# VideoLoaderAPI  
*English | [中文](README.zh.md)*        

This document provides a guide on how to quickly integrate the VideoLoaderAPI.

## 1. Environment Setup
- Xcode 13.0 or above
- Minimum supported system: iOS 12.0
- Make sure your project has a valid developer signature

## 2. Running the Example

- Clone or directly download the project source code
- Follow [The Account Document](https://docs.agora.io/en/video-calling/reference/manage-agora-account) to get the **App ID** and **App Certificate(if enable token)**.
- Follow [The Restfull Document](https://docs.agora.io/en/video-calling/reference/restful-authentication) to get the **Customer ID** and **Customer Secret**.
- Follow [The Media Pull Document](https://docs.agora.io/en/media-pull/get-started/enable-media-pull) to enable media pull for cloud player.
- Fill in Agora's App ID, Certificate, CloudPlayerKey, and CloudPlayerSecret in [KeyCenter.swift](Example/VideoLoaderAPI/KeyCenter.swift)

  ```
  static var AppId: String = <#Your AppId#>
  static var Certificate: String = <#Your Certificate#>
  static let CloudPlayerKey: String? = <#Your CloudPlayerKey#>
  static let CloudPlayerSecret: String? = <#Your CloudPlayerSecret#>
  ```
- Open the terminal and navigate to the directory of [Podfile](Example/Podfile). Execute the command pod install to generate the `VideoLoaderAPI.xcworkspace` file.
- Finally, open `VideoLoaderAPI.xcworkspace` and run the project to start your experience


## 3. Project Introduction

- <mark>1. Overview</mark>
> The VideoLoaderAPI is an API for achieving quick-join and quick-switch capabilities in video live streaming. This module aims to help video streaming developers integrate Agora's quick-join and quick-switch features more quickly.
>
- <mark>2. Function Introduction</mark>
> The VideoLoaderAPI Demo currently covers the following functions:
> - Selection of preload mode and video rendering mode 
>
>   Related code reference: [DebugSettingViewController.swift](Example/VideoLoaderAPI/DebugSettingViewController.swift)
>
> - Quick join
>
>   Related code reference: [RoomCollectionListViewController.swift](Example/VideoLoaderAPI/RoomCollectionListViewController.swift)
>
> - Quick switch
>   Related code reference: [RoomCollectionViewController.swift](Example/VideoLoaderAPI/Normal/CollectionView/CollectionRoomViewController.swift) 
>
- 3.File Introduction

Related code reference: [VideoLoaderAPI](VideoLoaderAPI/Classes/)

* [UIView+VideoLoader.swift](VideoLoaderAPI/Classes/UI/UIView+VideoLoader.swift): event handling module for quick join
* [AGCollectionLoadingDelegateHandler.swift](VideoLoaderAPI/Classes/UI/AGCollectionLoadingDelegateHandler.swift): event handling module for room list scrolling
* [AGCollectionSlicingDelegateHandler.swift](VideoLoaderAPI/Classes/UI/AGCollectionSlicingDelegateHandler.swift): event handling module for switching between live rooms
* [VideoLoaderApiImpl.swift](VideoLoaderAPI/Classes/VideoLoaderApiImpl.swift): the internal class used to handle channel management

## 4.Quick Integration
### Dependency Integration
- Copy the VideoLoaderAPI directory from the example code and add it to your project, at the same level as the Podfile

- Add the following code to your Podfile
  ```
  pod 'VideoLoaderAPI', :path => './VideoLoaderAPI'
  ```
- Open the terminal, navigate to the directory of the Podfile, and execute the `pod install` command to integrate the VideoLoader API into your project

### Initialization
  ```swift
  let api = VideoLoaderApiImpl.shared
  let config = VideoLoaderConfig()
  config.rtcEngine = _createRtcEngine()
  api.setup(config: config)
  ```
### List item object implementation IVideoLoaderRoomInfo
  ```swift
  class RoomListModel: NSObject, IVideoLoaderRoomInfo {
    //Externally set the interactive objects for the current room. If there are multiple, it indicates PK or connected microphone, and 1 represents a single anchor display
    var anchorInfoList: [VideoLoaderAPI.AnchorInfo] = []
    //room owner's uid
    func userId() -> String {
        return "\(anchorInfoList.first?.uid ?? 0)"
    }
    //room's id
    func channelName() -> String {
        return anchorInfoList.first?.channelName ?? ""
    }
  }
  ```
### Implementing Quick Join
#### Definition of Quick Join
The process in which the audience quickly sees the next live stream video after scrolling up or down in a live streaming room is defined as "quick Join".

#### Best Practices for Quick Join
  - Preloading channels on the room list page for channels in the field of view:
    - If the total number of rooms is less than 20, preloading channels for all rooms is recommended.
    - If the total number of rooms is more than 20, preloading channels for the first 20 rooms is recommended. When scrolling the room list, preloading channels for the rooms within the field of view after the scrolling ends is recommended.
  - Handling logic when clicking on a live room item:
    - When the user touches a live room item, join the channel and subscribe to the audio and video streams. (Mute audio if necessary)
    - When the user releases the touch, transition to the live room page with animation, and unmute audio.
    - If the user touches and then swipes away without completing a valid click, exit the channel.
  - When a live room item on the room list page is clicked, the live room page may not be created yet due to lazy loading. However, the RTC has already joined the channel and started pulling the stream, which may result in no rendered video view being set up for the SDK, possibly causing the first frame to be rendered slowly. The following solutions are recommended:
    - Use `setupRemoteVideo` to set up a view (View1) for the SDK immediately after joining the channel (joinChannelEx), and then add View1 to the video container area for display prior to handling other business logic (such as IM in the room).
    - Listen for the first frame rendered callback from the SDK, play a loading animation before the callback triggers, and set the live video as visible after the callback triggers.
  - Use universal tokens:
    - Universal tokens can save the time of fetching channel tokens before joining the channel.
    - Universal tokens have the risk of exposing the token, so whether to use them needs to be determined based on specific requirements.
#### How to Use VideoLoaderAPI for Quick join
  - **Create an AGCollectionLoadingDelegateHandler object and bind it to the corresponding collectionView**
      ```swift
      let collectionView = UICollectionView()
      ...

      //Create a handler instance and set the uid
      self.delegateHandler = AGCollectionLoadingDelegateHandler(localUid: kCurrentUid)
      //Set room list
      self.delegateHandler.roomList = roomList
      //Bind handler
      collectionView.delegate = self.delegateHandler
      ```

    > ⚠️ If you need to implement methods of UICollectionViewDelegate, you can inherit from AGCollectionLoadingDelegateHandler and implement them yourself. However, make sure to call super.{superMethod} in the overridden methods to ensure that quick join is still available.

  - **Set the tap operation for the corresponding cell**
      ```swift
      cell.ag_addPreloadTap(roomInfo: room,
                            localUid: kCurrentUid) { state in
          //Obtain the start and end of the click. If you need to intercept and not continue with the second click operation, you can return false, for example, if the token was not successfully obtained
          if token.count == 0 {
            if state == .began {
              //start click
            } else if state == .ended {
              //end click
            }
            return false
          }
          
          return true
      } completion: { [weak self] in
          guard let self = self else {return}
          //Got clicked and entered the room details page
      }
      ```
### Implementing Quick switch
##### Definition of Quick Switch
The process in which the audience quickly sees the next live stream video after scrolling up or down within a live streaming room is defined as "quick switch".

##### Best Practices for Quick Switch
- Preload Strategy:
  - Preload channels for up to 20 channels scrolled up or down in the list.
- Preload + PreJoin Strategy:
   - Preload channels as mentioned above.
   - PreJoin: Join the RTC channel of the above and below rooms without subscribing to the audio and video streams. Subscribe to the audio and video streams of the target room at the appropriate time during scrolling.
   - The PreJoin strategy may incur additional charges due to joining additional channels, so use it carefully.
- Use universal tokens:
   - Universal tokens can save the time of fetching channel tokens before joining the channel.
   - Universal tokens have the risk of exposing the token, so whether to use them needs to be determined based on specific requirements.
- Rendering video frames: After subscribing to the video stream, immediately call `setupRemoteVideo` to set up the view to avoid missing the first I-frame decoding and slow rendering of the first frame.
  
#### How to Implement Quick Switch Using the VideoLoaderAPI
  - **Create an AGCollectionSlicingDelegateHandler object and bind it to the corresponding collectionView**
      ```swift
      let collectionView = UICollectionView()
      ...

      //Create a handler instance
      let needPrejoin = true  //Set whether prejoin is required.
      let videoType = .visible //Set video on quick switch policy。
      let audioType = .endScroll  //Set audio on quick switch policy。
      self.delegateHandler = AGCollectionSlicingDelegateHandler(localUid: kCurrentUid, needPrejoin: needPrejoin)
      self.delegateHandler.videoSlicingType = videoType
      self.delegateHandler.audioSlicingType = audioType

      //Set canvas callback
      self.delegateHandler.onRequireRenderVideo = { [weak self] (info, cell, indexPath) in
          guard let cell = cell as? TestRoomCollectionViewCell else {return nil }
          return cell.canvasView
      }
      
      //Set room list
      self.delegateHandler.roomList = AGRoomArray(roomList: roomList)
      
      //Bind handler
      collectionView.delegate = self.delegateHandler
      ```
  - **Update the room list**
    ```swift
    //Update room list
    self.delegateHandler.roomList = AGRoomArray(roomList: roomList)

    //1. Fully refresh the room
    collectionView.reloadData()

    //2. Specify refreshing specific rooms
    collectionView.reloadItems(at: indexPaths)
    ```
    > ⚠️ If you need to implement methods of UICollectionViewDelegate, you can inherit from AGCollectionSlicingDelegateHandler and implement them yourself. 
    However, make sure to call super.{superMethod} in the overridden methods to ensure that quick switch is still available.


  - **Clean the cache after leaving the quick switch room**
    ```swift
    VideoLoaderApiImpl.shared.cleanCache()
    ```