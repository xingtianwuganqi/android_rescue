package com.rescue.flutter_720yun.user.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.MessageListModel
import com.rescue.flutter_720yun.util.UserManager

class UserSettingViewModel: ViewModel() {

    private val _messageList = MutableLiveData<MutableList<MessageListModel>>().apply {
        value = mutableListOf(
            MessageListModel("icon_setting_pswd",
                BaseApplication.context.resources.getString(R.string.user_change_password)),
            MessageListModel("icon_setting_fk", BaseApplication.context.resources.getString(R.string.user_suggestion)),
//            MessageListModel("icon_me_black",
//                BaseApplication.context.resources.getString(R.string.user_black)),
            MessageListModel("icon_acc_sec",
                BaseApplication.context.resources.getString(R.string.user_account)),
        )
    }
    val messageList: LiveData<MutableList<MessageListModel>> = _messageList

    init {
        if (UserManager.isLogin) {
            _messageList.value?.add(
                MessageListModel(
                    "icon_mi_inter",
                    BaseApplication.context.getString(R.string.logout)
                )
            )
        }
    }
}