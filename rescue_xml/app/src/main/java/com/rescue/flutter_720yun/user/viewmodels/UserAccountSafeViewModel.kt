package com.rescue.flutter_720yun.user.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.UserService
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.util.paramDic
import kotlinx.coroutines.launch

class UserAccountSafeViewModel: ViewModel() {

    private val appService = ServiceCreator.create<UserService>()
    private val _deleteComplete = MutableLiveData<Boolean?>()
    val deleteComplete: LiveData<Boolean?> get() = _deleteComplete

    fun deleteAccountNetworking() {
        viewModelScope.launch {
            try {
                val dic = paramDic
                val response = appService.deleteAccount(dic).awaitResp()
                if (response.code == 200) {
                    _deleteComplete.value = true
                }else{
                    _deleteComplete.value = false
                }
            }catch (e: Exception) {
                _deleteComplete.value = false
            }
        }
    }

}