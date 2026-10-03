package com.rescue.flutter_720yun.adoption.viewmodels

import androidx.lifecycle.*
import com.rescue.flutter_720yun.adoption.repository.*
import com.rescue.flutter_720yun.util.UserManager
import kotlinx.coroutines.*

open class AdoptionViewModel : ViewModel() {
    val repository = AdoptionRepository()
    val busy = MutableLiveData(false)
    val error = MutableLiveData<AdoptionError?>()
    val revision = MutableLiveData(0L)
    private var job: Job? = null
    private var operation = 0L
    private var accountRevision = UserManager.sessionRevision.value
    private val accountObserver = Observer<Long> {
        if(it == accountRevision) return@Observer
        accountRevision = it
        cancelWork(); error.value = null
        clearPrivateState(); revision.value = (revision.value ?: 0) + 1
    }
    init { UserManager.sessionRevision.observeForever(accountObserver) }
    protected open fun clearPrivateState() = Unit
    protected open fun clearUnavailableState() = clearPrivateState()
    fun cancelWork() { operation++; job?.cancel(); busy.value = false }
    fun run(action: suspend () -> Unit) {
        if(busy.value == true) return
        val currentOperation = ++operation
        busy.value = true; error.value = null
        job = viewModelScope.launch {
            try { action() }
            catch(cancel: CancellationException) { throw cancel }
            catch(e: AdoptionError) { if(operation == currentOperation) { busy.value = false; if(e.http==403) clearPrivateState() else if(e.http==404) clearUnavailableState(); error.value = e } }
            catch(e: Exception) { if(operation == currentOperation) { busy.value = false; error.value = AdoptionError(0, message = "网络结果未知，请刷新状态后重试") } }
            finally { if(operation == currentOperation) busy.value = false }
        }
    }
    override fun onCleared() { UserManager.sessionRevision.removeObserver(accountObserver); super.onCleared() }
}
