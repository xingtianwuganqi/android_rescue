package com.rescue.flutter_720yun.adoption.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.rescue.flutter_720yun.adoption.models.*
import com.rescue.flutter_720yun.adoption.repository.AdoptionError
import com.rescue.flutter_720yun.adoption.ui.AdoptionNotificationChanges
import kotlinx.coroutines.*

open class AdoptionApplicationsViewModel(private val saved: SavedStateHandle) : AdoptionViewModel() {
    val previewImages=MutableLiveData<Map<Int,String>>(emptyMap())
    private val previewRequested=mutableSetOf<Int>()
    private val previewJobs=mutableListOf<Job>()
    fun loadPreview(topicId: Int) {
        if(topicId<=0 || !previewRequested.add(topicId)) return
        val session=com.rescue.flutter_720yun.util.UserManager.sessionRevision.value
        previewJobs+=viewModelScope.launch {
            try {
                val image=repository.preview(topicId)
                if(session==com.rescue.flutter_720yun.util.UserManager.sessionRevision.value && image!=null)
                    previewImages.value=previewImages.value.orEmpty()+(topicId to image)
            } catch(e: CancellationException) { throw e } catch(e: Exception) { /* keep placeholder */ }
        }
    }
    val items = MutableLiveData<List<AdoptionApplication>>(emptyList())
    var topic: Int? = null
    protected var mine = false
    val role get() = if(mine) "mine" else "received"
    var status: String
        get() = saved["filter"] ?: if(mine) "" else "applying"
        set(value) { saved["filter"] = value }
    var page = 0
    var hasMore = true
    var initialized = false
    private var applicationLastRefresh=true
    private var applicationScope: Triple<String, Int?, String>? = null
    fun retryApplications() = load(applicationLastRefresh)
    fun load(refresh: Boolean = true) {
        val scope = Triple(role, topic, status)
        val scopeChanged = applicationScope != scope
        val reload = refresh || scopeChanged || !initialized
        if(reload) cancelWork()
        if(scopeChanged) {
            applicationScope = scope
            items.value = emptyList(); page = 0; hasMore = true; initialized = false
        }
        if(!reload && !hasMore) return
        applicationLastRefresh=reload
        val filter=status; val requestedTopic=topic; val next=if(reload) 1 else page+1
        run {
            val data=if(mine) repository.mine(filter,next) else repository.received(requestedTopic,filter,next)
            if(scope != Triple(role, topic, status)) return@run
            items.value=(if(reload) data.items.orEmpty() else items.value.orEmpty()+data.items.orEmpty()).distinctBy { it.application_id }
            page=next; hasMore=data.has_more; initialized=true
        }
    }

    val reminders=MutableLiveData<List<AdoptionNotification>>(emptyList())
    val reminderBusy=MutableLiveData(false)
    val reminderError=MutableLiveData<String?>()
    val reminderCounts=MutableLiveData<UnreadCount?>()
    val readingId=MutableLiveData<Int?>()
    val readMessage=MutableLiveData<String?>()
    var reminderHasMore=false
    private var reminderPage=0
    private var reminderFailedAppend=false
    fun retryReminders() = loadReminders(!reminderFailedAppend)
    private var reminderJob: Job?=null
    private var readJob: Job?=null
    private var reminderOperation=0L
    private var readOperation=0L
    fun loadReminders(refresh: Boolean = true) {
        if(readingId.value != null) return
        if(!refresh && (reminderBusy.value == true || !reminderHasMore)) return
        reminderJob?.cancel()
        val operation=++reminderOperation
        val requestedRole=role; val requestedTopic=topic
        val next=if(refresh) 1 else reminderPage+1
        reminderBusy.value=true; reminderError.value=null
        reminderJob=viewModelScope.launch {
            try {
                val data=repository.reminderPage(requestedRole,requestedTopic,next)
                if(operation != reminderOperation || requestedRole != role || requestedTopic != topic) return@launch
                reminders.value=(if(refresh) data.items else reminders.value.orEmpty()+data.items).distinctBy { it.id }
                reminderCounts.value=data.counts; reminderPage=next; reminderHasMore=data.hasMore
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { if(operation == reminderOperation) {
                reminderFailedAppend=!refresh
                reminderError.value=(e as? AdoptionError)?.message ?: "提醒加载失败，请点击重试"
            } }
            finally { if(operation == reminderOperation) reminderBusy.value=false }
        }
    }
    fun readReminder(item: AdoptionNotification) {
        if(readingId.value != null || item.id !in reminders.value.orEmpty().map { it.id }) return
        reminderJob?.cancel(); reminderOperation++; reminderBusy.value=false
        readingId.value=item.id; reminderError.value=null; reminderFailedAppend=false
        val operation=++readOperation
        readJob=viewModelScope.launch {
            try {
                repository.read(item.id)
                if(operation != readOperation) return@launch
                reminders.value=reminders.value.orEmpty().filter { it.id != item.id }
                reminderCounts.value=reminderCounts.value?.let { count ->
                    count.copy(unread_count=(count.unread_count-1).coerceAtLeast(0),
                        mine_unread_count=count.mine_unread_count?.let { if(role=="mine") (it-1).coerceAtLeast(0) else it },
                        received_unread_count=count.received_unread_count?.let { if(role=="received") (it-1).coerceAtLeast(0) else it })
                }
                readMessage.value=item.message.orEmpty()
                AdoptionNotificationChanges.changed(role)
                readingId.value=null
                loadReminders()
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { if(operation==readOperation) reminderError.value=(e as? AdoptionError)?.message ?: "标记已读失败，请重试" }
            finally { if(operation==readOperation) readingId.value=null }
        }
    }
    fun stopReminders() {
        reminderOperation++; readOperation++; reminderJob?.cancel(); readJob?.cancel()
        reminderBusy.value=false; readingId.value=null
    }
    override fun clearUnavailableState() { applicationScope=null; items.value=emptyList(); initialized=false; page=0; hasMore=true }
    override fun clearPrivateState() {
        previewJobs.forEach { it.cancel() };previewJobs.clear();previewRequested.clear();previewImages.value=emptyMap()
        stopReminders(); applicationScope=null; items.value=emptyList(); page=0; hasMore=true; initialized=false
        reminders.value=emptyList(); reminderCounts.value=null; reminderPage=0; reminderHasMore=false
        reminderError.value=null; readMessage.value=null; reminderFailedAppend=false
    }
}
