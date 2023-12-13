//
//  ShowRobotService.swift
//  AgoraEntScenarios
//
//  Created by Jonathan on 2023/7/12.
//

import Foundation

let kCurrentUid: UInt = ShowRobotService.shared.currentUid
let kRobotUid: UInt = 2000000001
private let kRobotRoomStartId = 1001001
private let robotStreamURL = [
    "https://download.agora.io/demo/release/agora_test_video_20_music.mp4",
    "https://download.agora.io/demo/release/agora_test_video_21_music.mp4",
    "https://download.agora.io/demo/release/agora_test_video_22_music.mp4",
    "https://download.agora.io/sdk/release/agora_test_video_12.mp4",
    "https://download.agora.io/sdk/release/agora_test_video_11.mp4",
    "https://download.agora.io/sdk/release/agora_test_video_10.mp4"
]

class ShowRobotService {
    static let shared: ShowRobotService = ShowRobotService()
    fileprivate var currentUid: UInt {
        get {
            let uidKey = "agora_uid"
            var uid = UserDefaults.standard.value(forKey: uidKey) as? UInt ?? 0
            if uid == 0 {
                uid = UInt(arc4random_uniform(100000000))
                UserDefaults.standard.setValue(uid, forKey: uidKey)
            }
            return uid
        }
    }
    private var timer: Timer?
    func startCloudPlayers(count: Int) {
        assert(count > 0)
        for i in 0...count-1 {
            let roomId = ShowRobotService.robotRoomId(i)
            let idx = i % robotStreamURL.count
            let streamUrl = robotStreamURL[idx]
            print("startCloudPlayer[\(roomId)]: \(streamUrl)")
            NetworkManager.shared.startCloudPlayer(channelName: "\(roomId)",
                                                   uid: "\(kCurrentUid)",
                                                   robotUid: UInt(kRobotUid),
                                                   streamUrl: streamUrl) { msg in
                if let msg = msg {
                    print("startCloudPlayer fail[\(roomId)]: \(msg)")
                    return
                }
                if i == 0 {
                    self.timer = Timer.scheduledTimer(withTimeInterval: 10, repeats: true) { timer in
                        self.playerHeartBeat(count: count)
                    }
                    self.timer?.fire()
                }
            }
        }
    }
    
    private func playerHeartBeat(count: Int) {
        for i in 0...count-1 {
            let roomId = ShowRobotService.robotRoomId(i)
            NetworkManager.shared.cloudPlayerHeartbeat(channelName: "\(roomId)", uid: "\(kCurrentUid)") { _ in
            }
        }
    }
    
    static func robotRoomId(_ index: Int) -> Int {
        return index % robotStreamURL.count + kRobotRoomStartId
    }
}
