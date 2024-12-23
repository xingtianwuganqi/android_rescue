package com.rescue.flutter_720yun.message.models

import android.os.Parcelable
import com.rescue.flutter_720yun.home.models.FindPetModel
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.home.models.UserInfo
import com.rescue.flutter_720yun.show.models.CommentListModel
import com.rescue.flutter_720yun.show.models.ReplyListModel
import com.rescue.flutter_720yun.show.models.ShowPageModel
import kotlinx.parcelize.Parcelize

@Parcelize
class MessageSingleListModel(
    val id: Int?,
    val create_time: String?,
    val msg_type: Int?,
    val msg_id: Int?,
    val from_info: UserInfo?,
    val to_info: UserInfo?,
    var is_read: Boolean?,
    val topicInfo: HomeListModel?,
    val showInfo: ShowPageModel?,
    val replyInfo: ReplyListModel?,
    val commentInfo: CommentListModel?,
    val findInfo: FindPetModel?,
    val reply_type: Int?,
    val reply_id: Int?
): Parcelable

@Parcelize
class MessageSystemListModel(
    val id: Int?,
    val create_time: String?,
    val content: String?,
    val msg_type: Int?,
    val user_id: Int?,
    val timeStr: String?
): Parcelable

/*
struct SystemMsgModel: HandyJSON {
    var create_time: String?
    var content: String?
    var msg_type: Int?
    var user_id: Int?
    var timeStr: String?
    var id: Int?

    mutating func didFinishMapping() {
        timeStr = Tool.shared.timeTDate(time: create_time ?? "")
    }
}
 */