package com.rescue.flutter_720yun.user.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.UserService
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.util.paramDic
import kotlinx.coroutines.launch

class UserFeedbackViewModel: ViewModel() {

    private val appService = ServiceCreator.create<UserService>()
    private val _completion = MutableLiveData<Boolean>()
    val completion get() = _completion

    fun feedBackNetworking(content: String, contact: String) {
        viewModelScope.launch {
            try {
                val dic = paramDic
                dic["content"] = content
                dic["contact"] = contact
                dic["phone_type"] = "android"
                val response = appService.suggestionNetworking(dic).awaitResp()
                if (response.code == 200) {
                    _completion.value = true
                }else{
                    _completion.value = false
                }
            }catch (e: Exception) {
                _completion.value = false
            }

        }
    }
}