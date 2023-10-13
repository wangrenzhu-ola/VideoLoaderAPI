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
    var origRoomList: [RoomListModel]?
    var roomList: [RoomListModel]? {
        didSet {
            if origRoomList == nil {
                origRoomList = roomList
            }
            delegateHandler.roomList = AGRoomArray(roomList: roomList)
        }
    }
    var focusIndex: Int = -1
    
    private lazy var delegateHandler: AGCollectionSlicingDelegateHandler = {
        let needPrejoin = settingInfoList[0].selectedValue() == 0 ? true : false
        let videoType = AGSlicingType(rawValue: settingInfoList[1].selectedValue()) ?? .visible
        let audioType = AGSlicingType(rawValue: settingInfoList[2].selectedValue()) ?? .endScroll
        let handler = AGCollectionSlicingDelegateHandler(localUid: kCurrentUid, needPrejoin: needPrejoin)
        handler.videoSlicingType = videoType
        handler.audioSlicingType = audioType
        handler.onRequireRenderVideo = { [weak self] (info, cell, indexPath) in
            guard let cell = cell as? TestRoomCollectionViewCell else {return nil }
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
        
        let button = UIButton(type: .custom)
        button.setTitle("close", for: .normal)
        button.setTitleColor(.white, for: .normal)
        view.addSubview(button)
        button.backgroundColor = .blue
        button.frame = CGRect(x: 10, y: 80, width: 100, height: 40)
        button.addTarget(self, action: #selector(closeAction), for: .touchUpInside)
        
        
        let button1 = UIButton(type: .custom)
        button1.setTitle("refresh", for: .normal)
        button1.setTitleColor(.white, for: .normal)
        view.addSubview(button1)
        button1.backgroundColor = .blue
        button1.frame = CGRect(x: 10, y: 130, width: 100, height: 40)
        button1.addTarget(self, action: #selector(refreshAction), for: .touchUpInside)
        
        let button2 = UIButton(type: .custom)
        button2.setTitle("load more", for: .normal)
        button2.setTitleColor(.white, for: .normal)
        view.addSubview(button2)
        button2.backgroundColor = .blue
        button2.frame = CGRect(x: 10, y: 180, width: 100, height: 40)
        button2.addTarget(self, action: #selector(reloadAction), for: .touchUpInside)
        
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
    
    @objc func refreshAction() {
        self.roomList = origRoomList!
        listView.reloadData()
    }
    
    @objc func reloadAction() {
        var appendIdxPath = [IndexPath]()
        let roomList = origRoomList!
        let totalCount = delegateHandler.roomList!.count()
        for (i, _) in roomList.enumerated() {
            appendIdxPath.append(IndexPath(row: i + totalCount, section: 0))
        }
        self.roomList = self.roomList! + roomList
        listView.insertItems(at: appendIdxPath)
    }
}

extension RoomCollectionViewController: UICollectionViewDataSource {
    open func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: kUIListViewCellIdentifier, for: indexPath) as! TestRoomCollectionViewCell
        let room = self.roomList![indexPath.row]
        cell.titleLabel.text = "roomId:\(room.channelName())\n index:\(indexPath.row)"
        return cell
    }
    
    open func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return roomList!.count
    }
}

extension RoomCollectionViewController: IVideoLoaderApiListener {
    func onFirstFrameRecv(channelName: String, uid: UInt, elapsed: Int64) {
        guard let room = roomList?.first(where: { $0.channelName() == channelName}),
                room.userId() == "\(uid)" else {
            return
        }
        label.frame.size = CGSize(width: view.bounds.width, height: 0)
        label.text = "\(formatter.string(from: Date()))\nroom[\(channelName)] show cost: \(elapsed) ms"
        label.sizeToFit()
        label.frame = CGRect(origin: CGPoint(x: 10, y: 40), size: label.frame.size)
    }
}
