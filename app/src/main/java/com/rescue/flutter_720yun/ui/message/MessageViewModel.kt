package com.rescue.flutter_720yun.ui.message

import androidx.compose.ui.res.stringResource
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.MessageListModel

class MessageViewModel : ViewModel() {

    private val _messageList = MutableLiveData<List<MessageListModel>>().apply {
        value = listOf(
            MessageListModel("icon_message_sys",BaseApplication.context.resources.getString(R.string.message_system)),
            MessageListModel("icon_message_like", BaseApplication.context.resources.getString(R.string.like_action)),
            MessageListModel("icon_message_collect",BaseApplication.context.resources.getString(R.string.collection_action)),
            MessageListModel("icon_message_com",BaseApplication.context.resources.getString(R.string.comment_action)),
            )
    }
    val messageList: LiveData<List<MessageListModel>> = _messageList
}