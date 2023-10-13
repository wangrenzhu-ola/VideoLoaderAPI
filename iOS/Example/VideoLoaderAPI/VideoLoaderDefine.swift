//
//  VideoLoaderDefine.swift
//  TestVideoLoader
//
//  Created by wushengtao on 2023/9/4.
//

import VideoLoaderAPI

class RoomListModel: NSObject, IVideoLoaderRoomInfo {
    func userId() -> String {
        return "\(anchorInfoList.first?.uid ?? 0)"
    }
    
    var anchorInfoList: [VideoLoaderAPI.AnchorInfo] = []
    
    func channelName() -> String {
        return anchorInfoList.first?.channelName ?? ""
    }
    
}
