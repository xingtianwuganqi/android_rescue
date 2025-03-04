package com.rescue.flutter_720yun.user.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.MessageListModel

class AdoptAboutViewModel: ViewModel() {

    private val _messageList = MutableLiveData<List<MessageListModel>>().apply {
        value = listOf(
            MessageListModel("icon_mi_zhinan",
                BaseApplication.context.resources.getString(R.string.drawer_guideline)),
            MessageListModel("icon_mi_inter",
                BaseApplication.context.resources.getString(R.string.drawer_description)),
//            MessageListModel("icon_mi_ad_xy",
//                BaseApplication.context.resources.getString(R.string.drawer_protocol)),
        )
    }
    val messageList: LiveData<List<MessageListModel>> = _messageList

}