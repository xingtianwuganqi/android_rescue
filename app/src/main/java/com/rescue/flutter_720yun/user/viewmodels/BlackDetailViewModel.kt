package com.rescue.flutter_720yun.user.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rescue.flutter_720yun.home.models.CoachReleasePhoto
import com.rescue.flutter_720yun.user.models.BlackDetailModel

class BlackDetailViewModel: ViewModel() {

    private var _dataModels = MutableLiveData<List<BlackDetailModel>>()
    val dataModels get() = _dataModels
    private var data: MutableList<BlackDetailModel> = mutableListOf(
        BlackDetailModel(
            "手机号",
            "请输入失信人手机号",
            null,
            null,
            true
        ),
        BlackDetailModel(
            "微信号",
            "请输入失信人微信号",
            null,
            null,
            false
        ),
        BlackDetailModel(
            "微信昵称",
            "请输入失信人微信昵称",
            null,
            null,
            false
        ),
        BlackDetailModel(
            "身份",
            null,
            null,
            null,
            true
        ),
        BlackDetailModel(
            "举报理由",
            "请输入举报理由",
            null,
            null,
            true
        ),
        BlackDetailModel(
            "举报证据",
            "请列举证据，聊天截图等",
            null,
            mutableListOf(
                CoachReleasePhoto(
                true,
                null,
                null,
                null
            )),
            true
        ),
    )
    var photos = mutableListOf(
        CoachReleasePhoto(
            true,
            null,
            null,
            null
        )
    )


    init {
        _dataModels.value = data
    }


    fun uploadImageData() {
        val item = data.last()
        item.photos = photos

        data[5] = item
        _dataModels.value = data

    }
}