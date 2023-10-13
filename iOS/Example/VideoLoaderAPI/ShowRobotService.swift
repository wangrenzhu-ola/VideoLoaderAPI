//
//  ShowRobotService.swift
//  AgoraEntScenarios
//
//  Created by Jonathan on 2023/7/12.
//

import Foundation

let kCurrentUid: UInt = 20001234
let kRobotUid: UInt = 1024
private let kRobotRoomStartId = 100
private let robotStreamURL = [
    "https://download.agora.io/demo/test/agora_test_video_10.mp4",
    "https://download.agora.io/demo/test/agora_test_video_11.mp4",
    "https://download.agora.io/demo/test/agora_test_video_12.mp4",
    "https://download.agora.io/demo/release/agora_show_video_1.mp4",
    "https://download.agora.io/demo/release/agora_show_video_2.mp4",
    "https://download.agora.io/demo/release/agora_show_video_3.mp4"
]
private let robotRoomIds = ["1", "2", "3"]
private let robotRoomOwnerHeaders = [
    "https://download.agora.io/demo/release/bot1.png"
]

class ShowRobotService {
    static let shared: ShowRobotService = ShowRobotService()
    private var timer: Timer?
    func startCloudPlayers(count: Int) {
        assert(count > 0)
        for i in 0...count-1 {
            let roomId = ShowRobotService.robotRoomId(i)
            let idx = i % robotStreamURL.count
            NetworkManager.shared.startCloudPlayer(channelName: "\(roomId)",
                                                   uid: "\(kCurrentUid)",
                                                   robotUid: UInt(kRobotUid),
                                                   streamUrl: robotStreamURL[idx]) { msg in
                if let _ = msg {return}
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
