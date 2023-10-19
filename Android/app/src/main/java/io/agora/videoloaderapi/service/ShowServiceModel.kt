package io.agora.videoloaderapi.service

import android.os.Parcel
import android.os.Parcelable
import io.agora.videoloaderapi.R

enum class ShowRoomStatus(val value: Int) {
    activity(0),//直播中
    end(1)//直播结束
}

enum class ShowRoomRequestStatus(val value: Int){
    idle(0),
    waitting(1),// 等待中
    accepted(2),//  已接受
    rejected(3),// 已拒绝
    ended(4)// 已结束
}

enum class ShowInteractionStatus(val value: Int) {
    idle(0), /// 空闲
    onSeat(1), /// 连麦中
    pking(2) /// pk中
}

// 房间详情信息
data class ShowRoomDetailModel constructor(
    val roomId: String,
    val roomName: String,
    val roomUserCount: Int,
    val thumbnailId: String, // 0, 1, 2, 3
    val ownerId: String,
    val ownerAvatar: String,// http url
    val ownerName: String,
    val roomStatus: Int = ShowRoomStatus.activity.value,
    val interactStatus: Int = ShowInteractionStatus.idle.value,
    val interactRoomName: String,
    val createdAt: Double,
    val updatedAt: Double
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!, // 房间号
        parcel.readString()?:"",
        parcel.readInt(),
        parcel.readString()?:"",
        parcel.readString()!!, // 房主id
        parcel.readString()?:"",
        parcel.readString()?:"",
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString()?:"",
        parcel.readDouble(),
        parcel.readDouble()
    ) {
    }

    fun toMap(): HashMap<String, Any>{
        return hashMapOf(
            Pair("roomId", roomId),
            Pair("roomName", roomName),
            Pair("roomUserCount", roomUserCount),
            Pair("thumbnailId", thumbnailId),
            Pair("ownerId", ownerId),
            Pair("ownerAvatar", ownerAvatar),
            Pair("ownerName", ownerName),
            Pair("roomStatus", roomStatus),
            Pair("interactStatus", interactStatus),
            Pair("createdAt", createdAt),
            Pair("updatedAt", updatedAt),
        )
    }

    fun getThumbnailIcon() = R.mipmap.show_room_cover_3

    fun isRobotRoom() = roomId.length > 6

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(roomId)
        parcel.writeString(roomName)
        parcel.writeInt(roomUserCount)
        parcel.writeString(thumbnailId)
        parcel.writeString(ownerId)
        parcel.writeString(ownerAvatar)
        parcel.writeString(ownerName)
        parcel.writeInt(roomStatus)
        parcel.writeInt(interactStatus)
        parcel.writeString(interactRoomName)
        parcel.writeDouble(createdAt)
        parcel.writeDouble(updatedAt)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ShowRoomDetailModel> {
        override fun createFromParcel(parcel: Parcel): ShowRoomDetailModel {
            return ShowRoomDetailModel(parcel)
        }

        override fun newArray(size: Int): Array<ShowRoomDetailModel?> {
            return arrayOfNulls(size)
        }
    }
}
