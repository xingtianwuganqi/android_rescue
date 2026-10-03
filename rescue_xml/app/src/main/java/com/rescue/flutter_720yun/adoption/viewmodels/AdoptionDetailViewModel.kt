package com.rescue.flutter_720yun.adoption.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.rescue.flutter_720yun.adoption.models.*
import com.rescue.flutter_720yun.adoption.repository.AdoptionError

sealed class AdoptionDetailEvent {
    object EditProfile : AdoptionDetailEvent()
    data class Confirm(val profile: ProfileData) : AdoptionDetailEvent()
    data class Contact(val value: AdoptionApplication) : AdoptionDetailEvent()
}
class AdoptionDetailViewModel(private val saved: SavedStateHandle) : AdoptionViewModel() {
    val state = MutableLiveData<ApplicationState?>()
    val event = MutableLiveData<AdoptionDetailEvent?>()
    var pendingLogin: Boolean
        get() = saved["pending_login"] ?: false
        set(value) { saved["pending_login"] = value; if(value) saved["pending_user"] = com.rescue.flutter_720yun.util.UserManager.userId ?: 0 }
    var topic: Int
        get() = saved["topic_id"] ?: 0
        set(value) { saved["topic_id"] = value }
    var statementDraft = ""
        private set
    fun resetAttempt() {
        statementDraft = ""; saved.remove<String>("idempotency_key"); saved.remove<String>("request_fingerprint")
    }
    fun refresh() = run {
        state.value = null
        val current = repository.state(topic); state.value = current
        if(current.application_status in listOf("applying", "communicating")) resetAttempt()
    }
    fun prepare() = run {
        state.value = null
        val current = repository.state(topic); state.value = current
        if(!current.can_apply) throw AdoptionError(409, message = "当前帖子不能申请，请查看最新状态")
        val profile = repository.profile()
        if(!profile.is_complete || profile.profile == null || profile.version == null) event.value = AdoptionDetailEvent.EditProfile
        else event.value = AdoptionDetailEvent.Confirm(profile)
    }
    fun submit(profile: ProfileData, statement: String) {
        if(busy.value == true) return
        val body = ApplicationWrite(profile.version ?: return, statement.trim())
        if(body.statement.length > 500) { error.value = AdoptionError(400, message = "申请说明最多500字"); return }
        statementDraft = body.statement
        val fingerprint = com.rescue.flutter_720yun.adoption.models.ApplicationFingerprint.of(com.rescue.flutter_720yun.util.UserManager.userId ?: 0, topic, body)
        saved["idempotency_key"] = ApplicationAttempt.key(saved["idempotency_key"], saved["request_fingerprint"], fingerprint)
        saved["request_fingerprint"] = fingerprint
        run {
            val latest = repository.state(topic); state.value = latest
            // An unknown prior POST is resolved by state before another POST with the same key.
            if(latest.application_status in listOf("applying", "communicating")) { resetAttempt(); return@run }
            if(!latest.can_apply) throw AdoptionError(409, message = "当前帖子不能申请")
            repository.apply(topic, saved.get<String>("idempotency_key")!!, body)
            resetAttempt()
            state.value = repository.state(topic)
        }
    }
    fun readContact() = run {
        val current = repository.state(topic); state.value = current
        val id = current.application_id ?: return@run
        event.value = AdoptionDetailEvent.Contact(repository.application(id))
    }
    fun end(note: String) = run {
        val current = repository.state(topic); state.value = current
        repository.action(current.application_id ?: return@run,
            ApplicationAction("end", current.application_version ?: return@run, note.trim()))
        state.value = repository.state(topic)
    }
    override fun clearPrivateState() {
        val resumeLogin = pendingLogin && saved.get<Int>("pending_user") == 0 && com.rescue.flutter_720yun.util.UserManager.isLogin
        state.value = null; event.value = null; pendingLogin = resumeLogin
        resetAttempt()
    }
}
