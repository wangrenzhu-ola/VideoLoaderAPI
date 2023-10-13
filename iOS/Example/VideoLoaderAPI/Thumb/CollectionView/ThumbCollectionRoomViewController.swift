//
//  ThumbRoomCollectionViewController.swift
//  TestVideoLoader
//
//  Created by wushengtao on 2023/8/16.
//

import Foundation
import AgoraRtcKit
import VideoLoaderAPI

class ThumbRoomCollectionViewController: UIViewController {
    var roomList: [RoomListModel] = [] {
        didSet {
            delegateHandler.roomList = AGRoomArray(roomList: roomList)
            listView.reloadData()
        }
    }
    var focusIndex: Int = 1
    
    private lazy var delegateHandler: AGCollectionSlicingDelegateHandler = {
        let handler = AGCollectionSlicingDelegateHandler(localUid: kCurrentUid, needPrejoin: true)
        handler.onRequireRenderVideo = { [weak self] (info, cell, indexPath) in
            guard let cell = cell as? ThumbRoomCollectionViewCell else {return nil }
            return cell.canvasView
        }
        return handler
    }()
    private lazy var listView: UICollectionView = {
        let layout = UICollectionViewFlowLayout()
        layout.minimumLineSpacing = 0
        layout.minimumInteritemSpacing = 0
        layout.sectionInset = .zero
        layout.itemSize = self.view.bounds.size
        let collectionView = UICollectionView(frame: self.view.bounds, collectionViewLayout: layout)
        collectionView.register(ThumbRoomCollectionViewCell.self, forCellWithReuseIdentifier: kUIListViewCellIdentifier)
        collectionView.scrollsToTop = false
        collectionView.delegate = self.delegateHandler
        collectionView.dataSource = self
        collectionView.isPagingEnabled = true
        collectionView.contentInsetAdjustmentBehavior = .never
        collectionView.bounces = false
        collectionView.showsVerticalScrollIndicator = false
        return collectionView
    }()
    
    deinit {
        print("deinit-- RoomViewController")
    }
    override func viewDidLoad() {
        super.viewDidLoad()
        _setupAPI()
        
        // Do any additional setup after loading the view.
        view.addSubview(listView)
        
        _loadToken()
    }
    
    @objc func closeAction() {
        VideoLoaderApiImpl.shared.cleanCache()
        dismiss(animated: false)
    }
    
    private func _setupAPI() {
        let rtcConfig = AgoraRtcEngineConfig()
        rtcConfig.appId = KeyCenter.AppId
        rtcConfig.channelProfile = .liveBroadcasting
        rtcConfig.audioScenario = .gameStreaming
        rtcConfig.areaCode = .global
        let engine = AgoraRtcEngineKit.sharedEngine(with: rtcConfig, delegate: nil)
        engine.setClientRole(.broadcaster)
        
        let api = VideoLoaderApiImpl.shared
        let config = VideoLoaderConfig()
        config.rtcEngine = engine
        api.setup(config: config)
    }
    
    private func _loadToken() {
        NetworkManager.shared.generateToken(channelName: "",
                                            uid: "\(kCurrentUid)",
                                            tokenType: .token007,
                                            type: .rtc) { token in
            guard let token = token else {
                self._loadToken()
                return
            }
            var list: [RoomListModel] = []
            for i in 0...10 {
                let idx = i % 3
                let room = RoomListModel()
                let anchor = AnchorInfo()
                anchor.uid = kRobotUid
                anchor.channelName = "\(ShowRobotService.robotRoomId(i))"
                anchor.token = token
                room.anchorInfoList = [anchor]
                list.append(room)
            }
            self.roomList = list
            self.listView.scrollToItem(at: IndexPath(row: self.focusIndex, section: 0), at: .centeredVertically, animated: false)
        }
    }
}

extension ThumbRoomCollectionViewController: UICollectionViewDataSource {
    open func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: kUIListViewCellIdentifier, for: indexPath) as! ThumbRoomCollectionViewCell
        let room = self.roomList[indexPath.row]
        cell.titleLabel.text = "roomId:\(room.channelName())\nidx: \(indexPath.row)"
        debugLoaderPrint("[VC]cellForItemAt[\(room.channelName())]: \(indexPath.row)")
        return cell
    }
    
    open func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return roomList.count
    }
}
