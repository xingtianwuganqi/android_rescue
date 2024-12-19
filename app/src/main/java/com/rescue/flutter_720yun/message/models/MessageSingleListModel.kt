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
    var is_read: Int?,
    val topicInfo: HomeListModel?,
    val showInfo: ShowPageModel?,
    val replyInfo: ReplyListModel?,
    val commentInfo: CommentListModel?,
    val findInfo: FindPetModel?,
    val reply_type: Int?,
    val reply_id: Int?
): Parcelable

/*
struct MessageListModel: HandyJSON {
    var id: Int?
    var create_time: String?
    var msg_type: Int?
    var msg_id: Int?
    var from_info: UserInfoModel?
    var to_info: UserInfoModel?
    var is_read: Int?
    var topicInfo: HomePageModel?
    var showInfo: ShowPageModel?
    var replyInfo: ReplyListModel?
    var commentInfo: CommentListModel?
    var findInfo: FindPetListModel?
    var reply_type: Int?
    var reply_id: Int?
}
 */