package com.rescue.flutter_720yun.user.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import androidx.lifecycle.ViewModel
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.MessageListModel
import com.rescue.flutter_720yun.util.UserManager

class UserSettingViewModel : ViewModel() {
    private val entries = MutableLiveData<List<MessageListModel>>()
    val messageList: LiveData<List<MessageListModel>> = entries

    private var needsEmailBinding=false
    private var statusJob: Job?=null
    fun refreshAccountStatus() {
        statusJob?.cancel();needsEmailBinding=false;refreshEntries()
        if(!UserManager.isLogin) return
        val revision=UserManager.sessionRevision.value
        statusJob=viewModelScope.launch {
            try {
                val response=com.rescue.flutter_720yun.network.ServiceCreator.create<com.rescue.flutter_720yun.network.UserService>().emailBindingStatus(mapOf("token" to UserManager.token))
                if(revision!=UserManager.sessionRevision.value) return@launch
                val code=response.body()?.get("code")?.asInt
                if(response.isSuccessful && code in listOf(200,213)) { needsEmailBinding=code==213;refreshEntries() }
            } catch(e: CancellationException) { throw e } catch(e: Exception) { /* Leave optional entry hidden when status is unknown. */ }
        }
    }
    fun refreshEntries() {
        val context = BaseApplication.context
        entries.value = buildList {
            add(MessageListModel("ic_setting_music", "背景音乐管理", category = "share_video_music"))
            add(MessageListModel("icon_mi_zeliao", context.getString(R.string.adoption_profile_title), category = "adoption_profile"))
            add(MessageListModel("icon_message_find_pet", context.getString(R.string.find_pet_profile_title), category = "find_pet_profile"))
            add(MessageListModel("icon_setting_pswd", context.getString(R.string.user_change_password), category = "password"))
            add(MessageListModel("icon_setting_fk", context.getString(R.string.user_suggestion), category = "feedback"))
            add(MessageListModel("icon_acc_sec", context.getString(R.string.user_account), category = "account_security"))
            add(MessageListModel("icon_me_black", "黑名单", category = "blacklist"))
            if (UserManager.isLogin && needsEmailBinding) add(MessageListModel("icon_check_email", "绑定邮箱", category="bind_email"))
            if (UserManager.isLogin) add(MessageListModel("icon_mi_inter", context.getString(R.string.logout), category = "logout"))
        }
    }
}
