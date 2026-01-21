package com.rescue.flutter_720yun.user.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.BaseResponse
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.UserService
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.util.paramDic
import com.rescue.flutter_720yun.util.toMD5
import kotlinx.coroutines.launch

class UserChangePasswordViewModel: ViewModel() {

    private var appService = ServiceCreator.create<UserService>()
    private val _changeSuccess = MutableLiveData<Boolean>()
    private val _statusMessage = MutableLiveData<String>()
    val changeSuccess: LiveData<Boolean> get() = _changeSuccess
    val statusMessage: LiveData<String> get() = _statusMessage

    fun changePasswordNetworking(originPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            try {
                val dic = paramDic
                dic["origin_pswd"] = originPassword.toMD5()
                dic["password"] = newPassword.toMD5()
                dic["confirm_pswd"] = confirmPassword.toMD5()
                val response = appService.changePasswordNetworking(dic).awaitResp()
                if (response.code == 200) {
                    _changeSuccess.value = true
                    _statusMessage.value = BaseApplication.context.getString(R.string.change_password_success)
                }else{
                    _changeSuccess.value = false
                    _statusMessage.value = response.message
                }
            }catch (e: Exception) {
                _changeSuccess.value = false
                _statusMessage.value = BaseApplication.context.getString(R.string.network_request_error)
            }
        }

    }

}