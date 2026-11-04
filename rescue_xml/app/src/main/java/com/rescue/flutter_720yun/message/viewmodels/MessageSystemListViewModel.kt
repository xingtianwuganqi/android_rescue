package com.rescue.flutter_720yun.message.viewmodels

import androidx.lifecycle.*
import com.rescue.flutter_720yun.findpet.FindPetRepository
import com.rescue.flutter_720yun.message.models.MessageSystemListModel
import com.rescue.flutter_720yun.util.*
import kotlinx.coroutines.*

/** Original system notification query still marks its notifications read server-side. */
class MessageSystemListViewModel : ViewModel(), CommonViewModelInterface {
    private val source=FindPetRepository()
    private val loading=MutableLiveData(false)
    override val isLoading: LiveData<Boolean> = loading
    override val isFirstLoading: LiveData<Boolean> = MutableLiveData(true)
    override val isLastPage: LiveData<Boolean> = MutableLiveData(true)
    private val refresh=MutableLiveData(RefreshState.REFRESH)
    override val refreshState: LiveData<RefreshState> = refresh
    val uiState=MutableLiveData<UiState<List<MessageSystemListModel>>>()
    private var job: Job?=null
    private var generation=0L
    private var session=UserManager.sessionRevision.value
    private val observer=Observer<Long> { current ->
        if(current!=session) {
            session=current;generation++;job?.cancel();loading.value=false
            uiState.value=UiState.Success(emptyList())
            if(UserManager.isLogin) systemMessageListNetworking(RefreshState.REFRESH) else uiState.value=UiState.Error("请登录后查看")
        }
    }
    init { UserManager.sessionRevision.observeForever(observer) }
    fun systemMessageListNetworking(state: RefreshState) {
        if(!UserManager.isLogin || loading.value==true) return
        val request=++generation
        loading.value=true;refresh.value=RefreshState.REFRESH
        if(uiState.value !is UiState.Success) uiState.value=UiState.FirstLoading
        job=viewModelScope.launch {
            try {
                val items=source.notifications()
                if(request!=generation) return@launch
                uiState.value=UiState.Success(items)
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { if(request==generation) uiState.value=UiState.Error(e.message ?: "消息加载失败，请重试") }
            finally { if(request==generation) loading.value=false }
        }
    }
    override fun onCleared() { UserManager.sessionRevision.removeObserver(observer);super.onCleared() }
}
