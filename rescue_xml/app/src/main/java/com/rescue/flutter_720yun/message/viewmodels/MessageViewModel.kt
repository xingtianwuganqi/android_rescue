package com.rescue.flutter_720yun.message.viewmodels

import androidx.lifecycle.*
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adoption.repository.AdoptionRepository
import com.rescue.flutter_720yun.home.models.MessageListModel
import com.rescue.flutter_720yun.message.models.MessageUnreadModel
import com.rescue.flutter_720yun.network.*
import com.rescue.flutter_720yun.util.*
import kotlinx.coroutines.*

class MessageViewModel : ViewModel() {
    private val appService = ServiceCreator.create<MessageService>()
    private val adoption = AdoptionRepository()
    private fun entries() = listOf(
        MessageListModel("icon_message_sys", "系统消息", category="system"),
        MessageListModel("icon_message_sys", "我的申请", unread=null, category="my_applications"),
        MessageListModel("icon_message_sys", "收到的申请", unread=null, category="received_applications"),
        MessageListModel("icon_message_like", BaseApplication.context.getString(R.string.like_action), category="like"),
        MessageListModel("icon_message_collect", BaseApplication.context.getString(R.string.collection_action), category="collection"),
        MessageListModel("icon_message_com", BaseApplication.context.getString(R.string.comment_action), category="comment")
    )
    private val _messageList = MutableLiveData(entries())
    val messageList: LiveData<List<MessageListModel>> = _messageList
    val unreadModel = MutableLiveData<MessageUnreadModel?>()
    val badgeTotal = MutableLiveData(0)
    private var adoptionTotal: Int? = null
    private fun updateBadge() {
        badgeTotal.value = com.rescue.flutter_720yun.message.models.MessageBadge.total(_messageList.value.orEmpty(), adoptionTotal)
    }
    private var changesRevision = com.rescue.flutter_720yun.adoption.ui.AdoptionNotificationChanges.revision.value
    private val changesObserver = Observer<Long> {
        if(it != changesRevision) {
            changesRevision = it
            val role=com.rescue.flutter_720yun.adoption.ui.AdoptionNotificationChanges.readRole
            val category=if(role=="mine") "my_applications" else if(role=="received") "received_applications" else null
            if(category!=null) {
                _messageList.value=_messageList.value?.map { row -> if(row.category==category) row.copy(unread=row.unread?.let { count -> (count-1).coerceAtLeast(0) }) else row }
                adoptionTotal=adoptionTotal?.let { count -> (count-1).coerceAtLeast(0) }; updateBadge()
            }
            unreadMessageNumberNetworking()
        }
    }
    private var oldJob: Job? = null
    private var adoptionJob: Job? = null
    private val accountObserver = Observer<Long> {
        oldJob?.cancel(); adoptionJob?.cancel()
        unreadModel.value = null; adoptionTotal=null; _messageList.value = entries(); badgeTotal.value=0
        if(UserManager.isLogin) unreadMessageNumberNetworking()
    }
    init { UserManager.sessionRevision.observeForever(accountObserver); com.rescue.flutter_720yun.adoption.ui.AdoptionNotificationChanges.revision.observeForever(changesObserver) }
    fun unreadMessageNumberNetworking() {
        if(!UserManager.isLogin) return
        val session=UserManager.sessionRevision.value
        oldJob?.cancel()
        oldJob = viewModelScope.launch {
            try {
                val response=appService.unreadMessageNumber(paramDic).awaitResp()
                if(session != UserManager.sessionRevision.value) return@launch
                if(response.code==200) {
                    unreadModel.value=response.data
                    val values=mapOf("system" to response.data.sys_unread,"like" to response.data.like_unread,
                        "collection" to response.data.collec_unread,"comment" to response.data.com_unread)
                    _messageList.value=_messageList.value?.map { it.copy(unread=values[it.category] ?: it.unread) }; updateBadge()
                }
            } catch(e: CancellationException) { throw e } catch(e: Exception) { /* Existing counts retry on foreground. */ }
        }
        adoptionJob?.cancel()
        adoptionJob=viewModelScope.launch {
            try {
                val counts=adoption.classifiedUnread()
                adoptionTotal=counts.unread_count
                _messageList.value=_messageList.value?.map { when(it.category) {
                    "my_applications" -> it.copy(unread=counts.mine_unread_count)
                    "received_applications" -> it.copy(unread=counts.received_unread_count)
                    else -> it
                } }; updateBadge()
            } catch(e: CancellationException) { throw e } catch(e: Exception) { /* No payload logging; retry on foreground. */ }
        }
    }
    override fun onCleared() { com.rescue.flutter_720yun.adoption.ui.AdoptionNotificationChanges.revision.removeObserver(changesObserver); UserManager.sessionRevision.removeObserver(accountObserver); super.onCleared() }
}
