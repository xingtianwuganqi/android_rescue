package com.rescue.flutter_720yun.show.models

import android.os.Parcelable
import com.rescue.flutter_720yun.home.models.UserInfo
import com.rescue.flutter_720yun.home.models.UserInfoModel
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShowPageModel(
    val show_id: Int?,
    val user: UserInfo?,
    val imgs: List<String>?,
    val view_num: Int?,
    val likes_num: Int?,
    var collection_num: Int?,
    val comments_num: Int?,
    val instruction: String?,
    val gambit_type: GambitListModel?,
    var liked: Boolean?,
    var collectioned: Boolean?,
    val create_time: String?,
    val commentInfo: CommentListModel?,
    val commNum: Int?
): Parcelable

@Parcelize
data class GambitListModel(
    var descript: String?,
    var id: Int?,
    @Transient
    var selected: Boolean = false
): Parcelable


@Parcelize
data class CommentListModel(
    var comment_id: Int? = null,
    var create_time : String? = null,
    var topic_id : Int? = null,
    var topic_type : Int? = null, // 秀宠1，后面加领养的回复
    var content : String? = null,
    var from_uid : Int? = null,
    var to_uid: Int? = null,
    var replys: List<ReplyListModel>? = null,
    var isOpend: Boolean? = null, // 回复是否折叠，true 是全部展示,false 时展示折叠cell
    var showReply: List<ReplyListModel>? = null,
    var userInfo: UserInfoModel? = null,
    var reply_count: Int? = null,
    var next_page: Int = 2
): Parcelable

@Parcelize
data class ReplyListModel(
    var id: Int?,
    var comment_id : Int?,
    var reply_id : Int?, // #表示回复目标的 id，如果 reply_type 是 comment 的话，那么 reply_id ＝ commit_id，如果 reply_type 是 reply 的话，这表示这条回复的父回复。
    var reply_type : Int?, // #表示回复的类型，因为回复可以是针对评论的回复（comment），也可以是针对回复的回复（reply）， 通过这个字段来区分两种情景。
    var content : String?,
    var from_uid : Int?,
    var to_uid : Int?,
    var fromInfo: UserInfo?,
    var toInfo: UserInfo?,
    var create_time: String?
): Parcelable

@Parcelize
data class ShowReleaseModel(
    var instruction: String?,
    val images: String?,
    var gambit_id: Int?
): Parcelable



@Parcelize
data class CommentItemModel(
    var type: Int?, // 1.评论 2.回复 3.加载更多评论 4.暂无更多评论
    var commentItem: CommentListModel?,
    var replyItem: ReplyListModel?
): Parcelable


@Parcelize
data class UserShowCollectionModel(
    val showcollect_id: Int?,
    val topic_id: Int?,
    val showInfo: ShowPageModel?
): Parcelable