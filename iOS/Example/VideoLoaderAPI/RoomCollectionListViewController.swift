//
//  RoomCollectionListViewController.swift
//  TestVideoLoader
//
//  Created by wushengtao on 2023/9/3.
//

import AgoraRtcKit
import VideoLoaderAPI

class RoomCollectionListViewController: UIViewController {
    private var roomList: [RoomListModel] = [] {
        didSet {
            delegateHandler.roomList = AGRoomArray(roomList: roomList)
            listView.reloadData()
        }
    }
    
    private func _initAPI() {
        let api = VideoLoaderApiImpl.shared
        let config = VideoLoaderConfig()
        config.rtcEngine = _createRtcEngine()
        api.setup(config: config)
    }
    
    private func _createRtcEngine() ->AgoraRtcEngineKit {
        let config = AgoraRtcEngineConfig()
        config.appId = KeyCenter.AppId
        config.channelProfile = .liveBroadcasting
        config.audioScenario = .gameStreaming
        config.areaCode = .global
        let engine = AgoraRtcEngineKit.sharedEngine(with: config,
                                                    delegate: nil)
        
        engine.setClientRole(.broadcaster)
        return engine
    }
    
    private lazy var delegateHandler: TestCollectionLoadingDelegateHandler = {
        let handler = TestCollectionLoadingDelegateHandler(localUid: kCurrentUid)
        handler.selectClosure = { [weak self] indexPath in
            guard let self = self else {return}
//            let vc = RoomTableViewController()
//            let vc = RoomCollectionViewController()
//            vc.modalPresentationStyle = .fullScreen
//            vc.roomList = self.roomList
//            vc.focusIndex = indexPath.row
//            present(vc, animated: false)

        }
        return handler
    }()
    private lazy var listView: UICollectionView = {
        let layout = UICollectionViewFlowLayout()
        layout.minimumLineSpacing = 5
        layout.minimumInteritemSpacing = 5
        layout.sectionInset = .zero
        let w = view.bounds.width / 2 - 5
        layout.itemSize = CGSize(width: w, height: w * 1.5)
        let collectionView = UICollectionView(frame: self.view.bounds, collectionViewLayout: layout)
        collectionView.register(RoomListViewCell.self, forCellWithReuseIdentifier: kUIListViewCellIdentifier)
        collectionView.scrollsToTop = false
        collectionView.delegate = self.delegateHandler
        collectionView.dataSource = self
        collectionView.contentInsetAdjustmentBehavior = .never
        collectionView.bounces = false
        collectionView.showsVerticalScrollIndicator = false
        return collectionView
    }()

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
        _initAPI()
        view.addSubview(listView)
        _loadToken()
        ShowRobotService.shared.startCloudPlayers(count: 10)
        
        self.navigationItem.rightBarButtonItem = UIBarButtonItem(title: "设置", style: .done, target: self, action: #selector(onSettingAction))
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(false, animated: false)
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
            for i in 0...12 {
                let room = RoomListModel()
                let anchor = AnchorInfo()
                anchor.uid = kRobotUid
                anchor.channelName = "\(ShowRobotService.robotRoomId(i))"
                anchor.token = token
                room.anchorInfoList = [anchor]
                let pkChannelIdx = Int(arc4random_uniform(24))
                if pkChannelIdx < 12 {
                    let channelName = "\(ShowRobotService.robotRoomId(pkChannelIdx))"
                    if channelName == anchor.channelName {
                        continue
                    }
                    let pkAnchor = AnchorInfo()
                    pkAnchor.uid = kRobotUid
                    pkAnchor.channelName = channelName
                    pkAnchor.token = token
                    room.anchorInfoList.append(pkAnchor)
                }
                list.append(room)
            }
            self.roomList = list
        }
    }
    
    @objc func onSettingAction() {
        let vc = DebugSettingViewController()
        navigationController?.pushViewController(vc, animated: true)
    }
}

extension RoomCollectionListViewController: UICollectionViewDataSource {
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return roomList.count
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: kUIListViewCellIdentifier, for: indexPath) as! RoomListViewCell
        let idx = indexPath.row
        let room = self.roomList[idx]
        cell.titleLabel.text = "roomId:\(room.channelName())"
        cell.ag_addPreloadTap(roomInfo: room,
                              localUid: kCurrentUid) { state in
            return true
        } completion: { [weak self] in
            guard let self = self else {return}
            let vc = RoomCollectionViewController()
            vc.roomList = self.roomList
            vc.focusIndex = indexPath.row
            self.navigationController?.pushViewController(vc, animated: true)
        }

        return cell
    }
}
