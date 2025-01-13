package com.rescue.flutter_720yun.user.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.home.models.UserInfo
import com.rescue.flutter_720yun.home.models.UserInfoModel
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.UserService
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.util.paramDic
import kotlinx.coroutines.launch

class UserViewModel: ViewModel() {
    val appService = ServiceCreator.create<UserService>()
    private val _userInfo = MutableLiveData<UserInfoModel>()
    val userInfo: LiveData<UserInfoModel> get() = _userInfo


    var userId: Int? = null

    fun userIdGetUserInfoNetworking() {
        viewModelScope.launch {
            try {
                val dic = paramDic
                dic["userId"] = userId
                Log.d("TAG", "$dic")
                val response = appService.userIdGetUserInfo(dic).awaitResp()
                if (response.code == 200) {
                    _userInfo.value = response.data
                }
            }catch (e: Exception) {
                Log.d("TAG", "$e")
            }
        }
    }
}