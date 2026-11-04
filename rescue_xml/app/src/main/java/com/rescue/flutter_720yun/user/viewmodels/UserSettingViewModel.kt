package com.rescue.flutter_720yun.user.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.MessageListModel
import com.rescue.flutter_720yun.util.UserManager

class UserSettingViewModel : ViewModel() {
    private val entries = MutableLiveData<List<MessageListModel>>()
    val messageList: LiveData<List<MessageListModel>> = entries

    fun refreshEntries() {
        val context = BaseApplication.context
        entries.value = buildList {
            add(MessageListModel("icon_mi_zeliao", context.getString(R.string.adoption_profile_title), category = "adoption_profile"))
            add(MessageListModel("icon_message_find_pet", "找宠资料", category = "find_pet_profile"))
            add(MessageListModel("icon_setting_pswd", context.getString(R.string.user_change_password), category = "password"))
            add(MessageListModel("icon_setting_fk", context.getString(R.string.user_suggestion), category = "feedback"))
            add(MessageListModel("icon_acc_sec", context.getString(R.string.user_account), category = "account_security"))
            if (UserManager.isLogin) add(MessageListModel("icon_mi_inter", context.getString(R.string.logout), category = "logout"))
        }
    }
}
