package com.rescue.flutter_720yun.user.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rescue.flutter_720yun.home.models.UserInfoModel

class UserInfoEditViewModel: ViewModel() {

    private val _userInfo = MutableLiveData<UserInfoModel>()
    val userInfo: LiveData<UserInfoModel> get() = _userInfo

    fun uploadUserModel(userInfo: UserInfoModel) {
        _userInfo.value = userInfo
    }

}