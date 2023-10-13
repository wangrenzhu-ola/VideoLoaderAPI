# 秒切/秒开场景化API

本文档主要介绍如何快速集成秒切/秒开场景化API

## 1.环境准备
- Xcode 13.0及以上版本
- 最低支持系统：iOS 12.0
- 请确保您的项目已设置有效的开发者签名

## 2.最佳实践策略
### 秒开
- 房间列表页面preloadChannel
- 触碰到房间Cell后加入频道，并订阅视频流
- 加入频道后尽早的设置远端渲染画面的view
- 松手订阅音视频
- 触碰到房间Cell但未完成点击，退出频道
### 秒切
- 加入房间后上下房间join rtc但是不订阅音视频流
- 加入频道后尽早的设置远端渲染画面的view
- 滑动后订阅可视区域房间的视频流,
- 停止滑动后订阅可视区域的房间音视频流，取消不在重用cell列表里的房间音视频流并leave rtc
- 使用万能Token，节省几百ms的耗时(可选: 取决于客户对于业务安全性的诉求)
  
## 3.运行示例
- 克隆或者直接下载项目源码
- 在项目的[KeyCenter.swift](Example/VideoLoaderAPI/KeyCenter.swift) 中填入声网的AppId、Certificate、CloudPlayerKey、CloudPlayerSecret，如何申请请查看[如何获取声网APPID](../README.md###如何获取声网APPID)
  
  ```
  static var AppId: String = <#Your AppId#>
  static var Certificate: String = <#Your Certificate#>
  static let CloudPlayerKey: String? = <#Your CloudPlayerKey#>
  static let CloudPlayerSecret: String? = <#Your CloudPlayerSecret#>
  ```
- 打开终端，进入到[Podfile](Example/Podfile)目录下，执行`pod install`命令
- 最后打开[VideoLoaderAPI.xcworkspace](Example/VideoLoaderAPI.xcworkspace)，运行即可开始您的体验

## 4.快速接入

- 把示例代码的目录VideoLoaderAPI拷贝至自己的工程里，例如与Podfile文件同级
- 在Podfile文件里加入
  ```
  pod 'VideoLoaderAPI', :path => './VideoLoaderAPI'
  ```
- 打开终端，执行`pod install`命令，秒切/秒开API即可集成进项目里
- 初始化设置
  ```swift
  let api = VideoLoaderApiImpl.shared
  let config = VideoLoaderConfig()
  config.rtcEngine = _createRtcEngine()
  api.setup(config: config)
  ```
- 列表item对象实现IVideoLoaderRoomInfo
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
- 秒开设置
    - 创建AGCollectionLoadingDelegateHandler对象绑定对应的collectionView
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

    - 为对应的cell设置点击操作
        ```swift
        cell.ag_addPreloadTap(roomInfo: room,
                              localUid: kCurrentUid) { state in
            //获取到点击的开始、结束，如果需要拦截不继续做秒开操作，可以返回false
            return true
        } completion: { [weak self] in
            guard let self = self else {return}
            //获取到被点击了，进入房间详情页面
        }
        ```
- 秒切设置
    - 创建AGCollectionSlicingDelegateHandler对象绑定对应的collectionView
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

      > ⚠️如果需要实现UICollectionViewDelegate的方法，请继承AGCollectionSlicingDelegateHandler自行实现，，但需要保证在重写过父类的方法里调用super.{superMethod}使用来保证秒切可用
- 离开秒切房间后清理缓存
    ```swift
    VideoLoaderApiImpl.shared.cleanCache()