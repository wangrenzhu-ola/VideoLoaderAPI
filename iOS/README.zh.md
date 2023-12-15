# 秒切/秒开场景化API          

*[English](README.md) | 中文*

本文档主要介绍如何快速集成秒切/秒开场景化API

## 1.环境准备
- Xcode 13.0及以上版本
- 最低支持系统：iOS 12.0
- 请确保您的项目已设置有效的开发者签名
  
## 2.运行示例
- 克隆或者直接下载项目源码
- 获取声网App ID -------- [声网Agora - 文档中心 - 如何获取 App ID](https://docs.agora.io/cn/Agora%20Platform/get_appid_token?platform=All%20Platforms#%E8%8E%B7%E5%8F%96-app-id)
  
  > - 点击创建应用
  >   
  >   ![](https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/github_readme/create_app_1.jpg)
  > 
  > - 选择你要创建的应用类型
  >   
  >   ![](https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/github_readme/create_app_2.jpg)
  > 

- 获取App 证书 ----- [声网Agora - 文档中心 - 获取 App 证书](https://docs.agora.io/cn/Agora%20Platform/get_appid_token?platform=All%20Platforms#%E8%8E%B7%E5%8F%96-app-%E8%AF%81%E4%B9%A6)
  
  > 在声网控制台的项目管理页面，找到你的项目，点击配置。
  > ![](https://fullapp.oss-cn-beijing.aliyuncs.com/scenario_api/callapi/config/1641871111769.png)
  > 点击主要证书下面的复制图标，即可获取项目的 App 证书。
  > ![](https://fullapp.oss-cn-beijing.aliyuncs.com/scenario_api/callapi/config/1637637672988.png)
- 获取秒切机器人服务配置（CloudPlayerKey、CloudPlayerSecret）
  > 
  >   ![xxx](https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/github_readme/show/CloudPlayer.png)
  > 
- 在项目的[KeyCenter.swift](Example/VideoLoaderAPI/KeyCenter.swift) 中填入声网的AppId、Certificate、机器人推流配置(CloudPlayerKey、CloudPlayerSecret)
  
  ```
  static var AppId: String = <#Your AppId#>
  static var Certificate: String = <#Your Certificate#>
  static let CloudPlayerKey: String? = <#Your CloudPlayerKey#>
  static let CloudPlayerSecret: String? = <#Your CloudPlayerSecret#>
  ```
- 打开终端，进入到[Podfile](Example/Podfile)目录下，执行`pod install`命令，生成`VideoLoaderAPI.xcworkspace`文件
- 最后打开`VideoLoaderAPI.xcworkspace`，运行即可开始您的体验

## 3. 项目介绍

- <mark>1. 概述</mark>
> VideoLoaderAPI 即秒开秒切场景化api, 该模块旨在帮助视频直播开发者更快集成声网秒切、秒开相关能力的最佳实践
>
- <mark>2. 功能介绍</mark>
> VideoLoaderAPI Demo 目前已涵盖以下功能
> - 选择预加载模式和视频出图模式
>
>   相关代码请参考：[DebugSettingViewController.swift](Example/VideoLoaderAPI/DebugSettingViewController.swift)
>
> - 秒开
>
>   相关代码请参考：[RoomCollectionListViewController.swift](Example/VideoLoaderAPI/RoomCollectionListViewController.swift)
>
> - 秒切
>     相关代码请参考：[RoomCollectionViewController.swift](Example/VideoLoaderAPI/Normal/CollectionView/CollectionRoomViewController.swift) 
>
- 3.文件简介

相关核心代码请参考：[VideoLoaderAPI](VideoLoaderAPI/Classes/)

* [UIView+VideoLoader.swift](VideoLoaderAPI/Classes/UI/UIView+VideoLoader.swift): 秒开事件处理模块
* [AGCollectionLoadingDelegateHandler.swift](VideoLoaderAPI/Classes/UI/AGCollectionLoadingDelegateHandler.swift): 房间列表滑动事件处理模块
* [AGCollectionSlicingDelegateHandler.swift](VideoLoaderAPI/Classes/UI/AGCollectionSlicingDelegateHandler.swift): 直播间切换事件处理模块
* [VideoLoaderApiImpl.swift](VideoLoaderAPI/Classes/VideoLoaderApiImpl.swift): 内部使用处理频道管理类

## 4.快速接入
### 集成依赖
- 把示例代码的目录VideoLoaderAPI拷贝至自己的工程里，例如与Podfile文件同级
- 在Podfile文件里加入
  ```
  pod 'VideoLoaderAPI', :path => './VideoLoaderAPI'
  ```
- 打开终端，执行`pod install`命令，秒切/秒开API即可集成进项目里
### 初始化设置
  ```swift
  let api = VideoLoaderApiImpl.shared
  let config = VideoLoaderConfig()
  config.rtcEngine = _createRtcEngine()
  api.setup(config: config)
  ```
### 列表item对象实现IVideoLoaderRoomInfo
  ```swift
  class RoomListModel: NSObject, IVideoLoaderRoomInfo {
    //外部设置当前房间的互动对象，如果有多个表示是pk或连麦，1个表示单主播展示
    var anchorInfoList: [VideoLoaderAPI.AnchorInfo] = []
    //房主uid
    func userId() -> String {
        return "\(anchorInfoList.first?.uid ?? 0)"
    }
    //房间id
    func channelName() -> String {
        return anchorInfoList.first?.channelName ?? ""
    }
  }
  ```
### 实现秒开
#### 秒开定义
观众在直播间内开始上下滑动直播间, 迅速看到下一个直播间画面的过程定义为 “秒切”
#### 秒开的最佳实践
  - 房间列表页面对视野范围内的频道进行 `preloadChannel`（channelList, token）
    - 如果房间总数小于20, 可以对所有房间进行 `preloadChannel`
    - 如果大于20, 对前20个房间进行 `preloadChannel`, 在滑动房间列表时, 当滑动结束, 对视野范围内的房间进行 preloadChannel
 
  - 点击直播间 item 处理逻辑：
    - 手指触碰到直播间 Item： 加入频道并订阅音视频流。（音频静音处理）
    - 手指触碰后抬起：列表页面切换到直播间内，增加切换动画, 解除音频静音
    - 手指触碰后滑走：未完成有效点击，退出频道

  - 房间列表页面内直播间 item 被点击时，直播间内页面因为懒加载可能未被创建，但此时 RTC 已加入频道并且拉流，导致无渲染视频的 View setup 给 SDK，导致可能错过首个 I 帧解码，导致可能首帧渲染慢, 解决方案如下:

    - 通过先创建一个视图 View1 设置给 SDK( `joinChannelEx` 后立刻 `setupRemoteVideoEx`)，等直播间内页面创建完优先将 View1 添加到视频准备显示的区域容器上，确保第一时间渲染。 其次再处理其他业务逻辑（例如 房间内的 IM 等其他 UI 渲染)
    - 可以监听 SDK 首帧出图回调, 在回调触发前播放加载动画, 在回调触发后将直播画面设置为 visiable

  - 使用万能 token。
    - 万能 token 可以节省加入频道前拉取频道 token 的耗时
    - 出于对业务安全性的考虑, 万能 token 有炸房风险, 需要根据具体需求决定是否使用
#### 如何使用 VideoLoaderAPI 快速实现秒开
  - **创建AGCollectionLoadingDelegateHandler对象绑定对应的collectionView**
      ```swift
      let collectionView = UICollectionView()
      ...

      //创建handler实例，并设置uid
      self.delegateHandler = AGCollectionLoadingDelegateHandler(localUid: kCurrentUid)
      //设置房间列表
      self.delegateHandler.roomList = roomList
      //绑定handler
      collectionView.delegate = self.delegateHandler
      ```

    > ⚠️如果需要实现UICollectionViewDelegate的方法，请继承AGCollectionLoadingDelegateHandler自行实现，但需要保证在重写过父类的方法里调用super.{superMethod}使用来保证秒开可用

  - **为对应的cell设置点击操作**
      ```swift
      cell.ag_addPreloadTap(roomInfo: room,
                            localUid: kCurrentUid) { state in
          //获取到点击的开始、结束，如果需要拦截不继续做秒开操作，可以返回false，例如token没有获取成功
          if token.count == 0 {
            if state == .began {
              //开始点击
            } else if state == .ended {
              //结束点击
            }
            return false
          }
          
          return true
      } completion: { [weak self] in
          guard let self = self else {return}
          //获取到被点击了，进入房间详情页面
      }
      ```
### 实现秒切
##### 秒切定义
观众在直播间内开始上下滑动直播间, 迅速看到下一个直播间画面的过程定义为 “秒切”

##### 秒切的最佳实践
  - preload 策略:
    - 在某个直播间内, 对滑动列表的上下小于等于20个频道进行 `preloadChannel`
  - preload + preJoin 策略:
    - preload 同上
    - preJoin: 加入上、下两个房间 rtcChannel 但不订阅音视频流, 滑动时根据业务对应的时机订阅目标房间的音视频流
    - preJoin 策略会加入额外的频道, 可能会带来额外费用, 在选择策略时需要特别注意
  - 使用万能 token。
    - 万能 token 可以节省加入频道前拉取频道 token 的耗时
    - 出于对业务安全性的考虑, 万能 token 有炸房风险, 需要根据具体需求决定是否使用
  - 渲染视频画面: 订阅视频流后立刻 `setupRemoteVideo` 设置视图, 避免错过首个 I 帧解码导致可能的首帧渲染慢
  
#### 如何使用 VideoLoaderAPI 快速实现秒切
  - **创建AGCollectionSlicingDelegateHandler对象绑定对应的collectionView**
      ```swift
      let collectionView = UICollectionView()
      ...

      //创建handler实例
      let needPrejoin = true  //设置是否需要秒切
      let videoType = .visible //设置视频秒开策略
      let audioType = .endScroll  //设置音频秒开策略
      self.delegateHandler = AGCollectionSlicingDelegateHandler(localUid: kCurrentUid, needPrejoin: needPrejoin)
      self.delegateHandler.videoSlicingType = videoType
      self.delegateHandler.audioSlicingType = audioType

      //设置画布回调
      self.delegateHandler.onRequireRenderVideo = { [weak self] (info, cell, indexPath) in
          guard let cell = cell as? TestRoomCollectionViewCell else {return nil }
          return cell.canvasView
      }
      
      //设置房间列表
      self.delegateHandler.roomList = AGRoomArray(roomList: roomList)
      
      //绑定handler
      collectionView.delegate = self.delegateHandler
      ```
  - **更新房间列表**
    ```swift
    //更新房间列表
    self.delegateHandler.roomList = AGRoomArray(roomList: roomList)

    //1.全量刷新房间
    collectionView.reloadData()

    //2.指定刷新特定房间
    collectionView.reloadItems(at: indexPaths)
    ```
    > ⚠️如果需要实现UICollectionViewDelegate的方法，请继承AGCollectionSlicingDelegateHandler自行实现，但需要保证在重写过父类的方法里调用super.{superMethod}使用来保证秒切可用
  - **离开秒切房间后清理缓存**
    ```swift
    VideoLoaderApiImpl.shared.cleanCache()
    ```