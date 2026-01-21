package com.rescue.flutter_720yun.message.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.MessageListModel
import com.rescue.flutter_720yun.message.models.MessageUnreadModel
import com.rescue.flutter_720yun.network.MessageService
import com.rescue.flutter_720yun.network.ServiceCreator
import com.rescue.flutter_720yun.network.awaitResp
import com.rescue.flutter_720yun.util.paramDic
import kotlinx.coroutines.launch

class MessageViewModel : ViewModel() {

    private val appService = ServiceCreator.create<MessageService>()

    private val _messageList = MutableLiveData<List<MessageListModel>>().apply {
        value = listOf(
            MessageListModel("icon_message_sys",BaseApplication.context.resources.getString(R.string.message_system)),
            MessageListModel("icon_message_like", BaseApplication.context.resources.getString(R.string.like_action)),
            MessageListModel("icon_message_collect",BaseApplication.context.resources.getString(R.string.collection_action)),
            MessageListModel("icon_message_com",BaseApplication.context.resources.getString(R.string.comment_action)),
            )
    }
    val messageList: LiveData<List<MessageListModel>> = _messageList

    private val _unreadModel = MutableLiveData<MessageUnreadModel>()
    val unreadModel get() = _unreadModel

    fun unreadMessageNumberNetworking() {
        viewModelScope.launch {
            try {
                val dic = paramDic
                val response = appService.unreadMessageNumber(dic).awaitResp()
                if (response.code == 200) {
                    _unreadModel.value = response.data
                    uploadMessageList(response.data)
                }
            }catch (e: Exception) {

            }
        }
    }

    private fun uploadMessageList(model: MessageUnreadModel) {
        val data = _messageList.value
        data?.get(0)?.unread = model.sys_unread
        data?.get(1)?.unread = model.like_unread
        data?.get(2)?.unread = model.collec_unread
        data?.get(3)?.unread = model.com_unread
        _messageList.value = data
        Log.d("TAG", "unread Message ${model.sys_unread}, ${model.com_unread}")

    }
}