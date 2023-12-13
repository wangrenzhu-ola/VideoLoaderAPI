//
//  RoomCollectionViewController.swift
//  TestVideoLoader
//
//  Created by wushengtao on 2023/8/16.
//

import Foundation
import AgoraRtcKit
import VideoLoaderAPI

class RoomCollectionViewController: UIViewController {
    var randomPKClosure: ((RoomListModel)->())?
    var loadMoreClosure: (()->([RoomListModel]))?
    var origRoomList: [RoomListModel]?
    var roomList: [RoomListModel] = [] {
        didSet {
            if origRoomList == nil {
                origRoomList = roomList
            }
            delegateHandler.roomList = AGRoomArray(roomList: roomList)
        }
    }
    var focusIndex: Int = -1
    
    private lazy var delegateHandler: AGCollectionSlicingDelegateHandler = {
        let needPrejoin = settingInfoList[DebugIndexType.prejoin.rawValue].selectedValue() == 0 ? true : false
        let videoType = AGVideoSlicingType(rawValue: settingInfoList[DebugIndexType.videoLoadPolicy.rawValue].selectedValue()) ?? .visible
        let audioType = AGAudioSlicingType(rawValue: settingInfoList[DebugIndexType.audioLoadPolicy.rawValue].selectedValue()) ?? .endScroll
        let handler = AGCollectionSlicingDelegateHandler(localUid: kCurrentUid, needPrejoin: needPrejoin)
        handler.videoSlicingType = videoType
        handler.audioSlicingType = audioType
        handler.onRequireRenderVideo = { [weak self] (info, cell, indexPath) in
            guard let cell = cell as? TestRoomCollectionViewCell else {return nil }
            let roomInfo = self?.roomList[indexPath.row]
            if info.channelName != roomInfo?.channelName() {
                return cell.otherBroadcasterView
            }
            return cell.mainBroadcasterView
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
        collectionView.register(TestRoomCollectionViewCell.self, forCellWithReuseIdentifier: kUIListViewCellIdentifier)
        collectionView.scrollsToTop = false
        collectionView.delegate = self.delegateHandler
        collectionView.dataSource = self
        collectionView.isPagingEnabled = true
        collectionView.contentInsetAdjustmentBehavior = .never
        collectionView.bounces = false
        collectionView.showsVerticalScrollIndicator = false
        return collectionView
    }()
    
    private lazy var label: UILabel = {
       let label = UILabel()
        label.textColor = .blue
        label.numberOfLines = 0
        label.font = UIFont.systemFont(ofSize: 16)
        return label
    }()
    
    deinit {
        VideoLoaderApiImpl.shared.removeListener(listener: self)
        print("deinit-- RoomViewController")
    }
    override func viewDidLoad() {
        super.viewDidLoad()
        
        VideoLoaderApiImpl.shared.addListener(listener: self)
        
        // Do any additional setup after loading the view.
        view.addSubview(listView)
        listView.scrollToItem(at: IndexPath(row: focusIndex, section: 0), at: .centeredVertically, animated: false)
        
        let button1 = UIButton(type: .custom)
        button1.setTitle("close", for: .normal)
        button1.setTitleColor(.white, for: .normal)
        view.addSubview(button1)
        button1.backgroundColor = .blue
        button1.frame = CGRect(x: 10, y: 80, width: 100, height: 40)
        button1.addTarget(self, action: #selector(closeAction), for: .touchUpInside)
        
        var buttonBottom: CGFloat = 130
        if settingInfoList[DebugIndexType.pkEnable.rawValue].selectedValue() == 1 {
            let button2 = UIButton(type: .custom)
            button2.setTitle("random pk", for: .normal)
            button2.setTitleColor(.white, for: .normal)
            view.addSubview(button2)
            button2.backgroundColor = .blue
            button2.frame = CGRect(x: 10, y: buttonBottom, width: 100, height: 40)
            button2.addTarget(self, action: #selector(randomPKAction), for: .touchUpInside)
            buttonBottom = 180
        }
        
        let button3 = UIButton(type: .custom)
        button3.setTitle("load more", for: .normal)
        button3.setTitleColor(.white, for: .normal)
        view.addSubview(button3)
        button3.backgroundColor = .blue
        button3.frame = CGRect(x: 10, y: buttonBottom, width: 100, height: 40)
        button3.addTarget(self, action: #selector(loadMoreAction), for: .touchUpInside)
        
        view.addSubview(label)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: false)
    }
    
    @objc func closeAction() {
        VideoLoaderApiImpl.shared.cleanCache()
        navigationController?.popViewController(animated: false)
    }
    
    @objc func randomPKAction() {
        guard let indexPath = listView.indexPathsForVisibleItems.first,
                  let cell = listView.cellForItem(at: indexPath) as? TestRoomCollectionViewCell else {return}
        let room = roomList[indexPath.row]
        randomPKClosure?(room)
        let list = self.roomList
        self.roomList = list
        cell.titleLabel.text = "roomId:\(room.channelName())\n index:\(indexPath.row)"
        cell.broadcasterCount = room.anchorInfoList.count
        print("randomPKAction[\(room.channelName())] pk channelName: \(room.anchorInfoList.count > 1 ? room.anchorInfoList.last!.channelName : "none")")
    }
    
    @objc func loadMoreAction() {
        guard let moreList = self.loadMoreClosure?(), moreList.count > 0 else {return}
        var indexPaths: [IndexPath] = []
        let start = self.roomList.count
        let end = start + moreList.count - 1
        for i in start...end {
            indexPaths.append(IndexPath(row: i, section: 0))
        }
        self.roomList = self.roomList + moreList
        self.listView.insertItems(at: indexPaths)
    }
}

extension RoomCollectionViewController: UICollectionViewDataSource {
    open func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: kUIListViewCellIdentifier, for: indexPath) as! TestRoomCollectionViewCell
        let room = self.roomList[indexPath.row]
        cell.titleLabel.text = "roomId:\(room.channelName())\n index:\(indexPath.row)"
        cell.broadcasterCount = room.anchorInfoList.count
        return cell
    }
    
    open func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return roomList.count
    }
}

extension RoomCollectionViewController: IVideoLoaderApiListener {
    func onFirstFrameRecv(channelName: String, uid: UInt, elapsed: Int64) {
        guard let room = roomList.first(where: { $0.channelName() == channelName}),
                room.userId() == "\(uid)" else {
            return
        }
        label.frame.size = CGSize(width: view.bounds.width, height: 0)
        label.text = "\(formatter.string(from: Date()))\nroom[\(channelName)] rtc cost: \(elapsed)ms"
        #if DEBUG
        if let date = delegateHandler.cellVisibleDate[room.channelName()] {
            
            label.text = "\(formatter.string(from: Date()))\nroom[\(channelName)] rtc cost: \(elapsed)ms display: \(Int64(-date.timeIntervalSinceNow * 1000))ms"
        }
        #endif
        label.sizeToFit()
        label.frame = CGRect(origin: CGPoint(x: 10, y: 40), size: label.frame.size)
    }
}
