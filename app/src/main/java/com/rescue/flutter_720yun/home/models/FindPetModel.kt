package com.rescue.flutter_720yun.home.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FindPetModel(
    var findId: Int?,
    var create_time: String?,
    var update_time: String?,
    var pet_type: Int?,
    var address: String?,
    var address_info: String?,
    var user_id: Int?,
    var is_delete: Int?,
    var effective: Int?,
    var desc: String?,
    var contact: String?,
    var userInfo: UserInfo?,
    var liked: Boolean?,
    var collection: Boolean?,

    var likeNum: Int?,
    var collectionNum: Int?,
    var commNum: Int?,
    var contact_info: String?,
    var getedcontact: Boolean?
): Parcelable