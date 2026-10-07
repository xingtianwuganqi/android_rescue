package com.rescue.flutter_720yun.adoption.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.rescue.flutter_720yun.adoption.models.*
import com.rescue.flutter_720yun.adoption.repository.AdoptionError
import kotlinx.coroutines.CancellationException

sealed class AdoptionDetailEvent {
    object EditProfile : AdoptionDetailEvent()
    object Applied : AdoptionDetailEvent()
    data class Notice(val message: String) : AdoptionDetailEvent()
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
        if(current.workflow_status != "open" || !current.can_apply) throw AdoptionError(409, message = "当前帖子不能申请，请查看最新状态")
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
            if(latest.workflow_status != "open" || !latest.can_apply) throw AdoptionError(409, message = "当前帖子不能申请")
            val application = repository.apply(topic, saved.get<String>("idempotency_key")!!, body)
            if(application.application_id <= 0 || application.version <= 0 || application.topic?.topic_id != topic ||
                application.applicant?.user_id != com.rescue.flutter_720yun.util.UserManager.userId ||
                application.status !in listOf("applying", "communicating", "ended")) {
                state.value = null
                throw AdoptionError(502, message = "申请结果暂不可用，请刷新状态后重试")
            }
            // A successful write must replace the pre-submit can_apply state, even if GET fails.
            state.value = latest.copy(can_apply = false, application_id = application.application_id,
                application_status = application.status, application_result = application.result,
                application_version = application.version, contact_authorized = application.contact_authorized)
            com.rescue.flutter_720yun.adoption.ui.AdoptionNotificationChanges.changed()
            resetAttempt()
            var refreshFailed = false
            try { state.value = repository.state(topic) }
            catch(e: CancellationException) { throw e }
            catch(e: AdoptionError) {
                if(e.status in listOf(401, 403, 404)) throw e
                refreshFailed = true
            }
            catch(e: Exception) { refreshFailed = true }
            event.value = when {
                application.status == "ended" -> AdoptionDetailEvent.Notice("此前的申请已结束，本次没有创建新申请。请查看最新状态后重新申请。")
                state.value?.application_status == "ended" -> AdoptionDetailEvent.Notice("申请已结束，请查看最新状态。")
                refreshFailed -> AdoptionDetailEvent.Notice("申请已提交，状态刷新失败，请稍后刷新。")
                state.value?.application_status == "communicating" -> AdoptionDetailEvent.Notice("申请已进入沟通中，请查看联系方式。")
                else -> AdoptionDetailEvent.Applied
            }
        }
    }
    fun readContact() = run {
        val current = repository.state(topic); state.value = current
        val id = current.application_id ?: return@run
        event.value = AdoptionDetailEvent.Contact(repository.application(id))
    }
    fun end(note: String) = run {
        val current = repository.state(topic); state.value = current
        val app=repository.application(current.application_id ?: return@run)
        if(app.topic?.topic_id!=topic || app.applicant?.user_id!=com.rescue.flutter_720yun.util.UserManager.userId || app.status !in listOf("applying","communicating"))
            throw AdoptionError(409,message="申请状态已变化，请刷新")
        val fingerprint="${app.application_id}:end:${app.version}"
        repository.action(app.application_id,ApplicationAction("end",app.version),operationKey(fingerprint))
        operationSucceeded(fingerprint)
        com.rescue.flutter_720yun.adoption.ui.AdoptionNotificationChanges.changed()
        state.value = repository.state(topic)
    }
    override fun clearPrivateState() {
        val resumeLogin = pendingLogin && saved.get<Int>("pending_user") == 0 && com.rescue.flutter_720yun.util.UserManager.isLogin
        state.value = null; event.value = null; pendingLogin = resumeLogin
        resetAttempt()
    }
}
