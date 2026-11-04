package com.rescue.flutter_720yun.adoption.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.rescue.flutter_720yun.adoption.models.*
import com.rescue.flutter_720yun.adoption.repository.AdoptionError
import kotlinx.coroutines.CancellationException

sealed class AdoptionDetailEvent {
    object EditProfile : AdoptionDetailEvent()
    object Applied : AdoptionDetailEvent()
    object Manage : AdoptionDetailEvent()
    data class Failure(val error: AdoptionError, val submission: Boolean) : AdoptionDetailEvent()
    data class Notice(val message: String, val title: String? = null) : AdoptionDetailEvent()
    data class Confirm(val profile: ProfileData) : AdoptionDetailEvent()
    data class Contact(val value: AdoptionApplication, val submitted: Boolean = false) : AdoptionDetailEvent()
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
    var suppressErrorDialog = false
        private set
    private fun publishError(value: AdoptionError) {
        suppressErrorDialog = true
        try { error.value = value } finally { suppressErrorDialog = false }
    }
    private fun request(submission: Boolean = false, action: suspend () -> Unit) = run {
        try { action() }
        catch(e: CancellationException) { throw e }
        catch(e: AdoptionError) {
            if (!submission) state.value = null
            publishError(e); busy.value = false
            event.value = AdoptionDetailEvent.Failure(e,submission)
        }
    }
    fun refresh() = request {
        try {
            val current = repository.state(topic); state.value = current
            if(current.application_status in listOf("applying", "communicating")) resetAttempt()
        } catch(e: CancellationException) { throw e }
        catch(e: AdoptionError) { state.value = null; publishError(e) }
        catch(e: Exception) { state.value = null; publishError(AdoptionError(0,message="网络结果未知，请刷新状态后重试")) }
    }
    fun open() = request {
        val current = repository.state(topic); state.value = current
        when (AdoptionDetailPolicy.route(current)) {
            AdoptionDetailPolicy.Route.MANAGE -> event.value = AdoptionDetailEvent.Manage
            AdoptionDetailPolicy.Route.ENDED -> event.value = AdoptionDetailEvent.Notice("送养已结束，无法提交新申请")
            AdoptionDetailPolicy.Route.UNAVAILABLE -> event.value = AdoptionDetailEvent.Notice("帖子状态暂不可用，请稍后重试")
            AdoptionDetailPolicy.Route.BLOCKED -> event.value = AdoptionDetailEvent.Notice("当前暂不可提交申请，请稍后重试")
            AdoptionDetailPolicy.Route.APPLICATION -> {
                val id = current.application_id?.takeIf { it > 0 } ?: throw AdoptionError(502,message="申请状态暂不可用，请刷新重试")
                event.value = AdoptionDetailEvent.Contact(checkedApplication(repository.application(id)))
            }
            AdoptionDetailPolicy.Route.APPLY -> loadProfile()
        }
    }
    private fun checkedApplication(app: AdoptionApplication): AdoptionApplication {
        if(app.application_id <= 0 || app.topic?.topic_id != topic || app.applicant?.user_id != com.rescue.flutter_720yun.util.UserManager.userId)
            throw AdoptionError(502,message="申请结果暂不可用，请刷新状态后重试")
        return app
    }
    private suspend fun loadProfile() {
        val profile = repository.profile()
        if(!AdoptionDetailPolicy.profileReady(profile)) event.value = AdoptionDetailEvent.EditProfile
        else event.value = AdoptionDetailEvent.Confirm(profile.copy(version=profile.version ?: profile.profile?.version))
    }

    fun prepare() = request {
        val current = repository.state(topic); state.value = current
        if(current.workflow_status != "open" || !current.can_apply) throw AdoptionError(409, message = "当前帖子不能申请，请查看最新状态")
        loadProfile()
    }
    fun submit(profile: ProfileData, statement: String) {
        if(busy.value == true) return
        val body = ApplicationWrite(profile.version ?: return, statement.trim())
        if(body.statement.length > 500) { error.value = AdoptionError(400, message = "申请说明最多500字"); return }
        statementDraft = body.statement
        val fingerprint = com.rescue.flutter_720yun.adoption.models.ApplicationFingerprint.of(com.rescue.flutter_720yun.util.UserManager.userId ?: 0, topic, body)
        saved["idempotency_key"] = ApplicationAttempt.key(saved["idempotency_key"], saved["request_fingerprint"], fingerprint)
        saved["request_fingerprint"] = fingerprint
        request(submission = true) {
            val latest = repository.state(topic); state.value = latest
            // An unknown prior POST is resolved by state before another POST with the same key.
            if(latest.application_status in listOf("applying", "communicating")) {
                resetAttempt()
                latest.application_id?.let { event.value = AdoptionDetailEvent.Contact(checkedApplication(repository.application(it)),submitted=true) }
                return@request
            }
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
            try { state.value = repository.state(topic) }
            catch(e: CancellationException) { throw e }
            catch(e: AdoptionError) {
                if(e.status in listOf(401, 403, 404)) throw e
            }
            catch(e: Exception) { /* Keep the successful POST result if the refresh is unavailable. */ }
            event.value = when {
                application.status == "ended" -> AdoptionDetailEvent.Notice("未创建新的有效申请，请刷新后重新申请。", "申请状态已变化")
                state.value?.application_status == "ended" || state.value?.workflow_status in listOf("adopted","closed") -> AdoptionDetailEvent.Notice("申请已结束，请查看最新状态。")
                application.status == "communicating" -> AdoptionDetailEvent.Contact(application,submitted=true)
                else -> AdoptionDetailEvent.Applied
            }
        }
    }
    fun readContact() = request {
        val current = repository.state(topic); state.value = current
        val id = current.application_id ?: return@request
        event.value = AdoptionDetailEvent.Contact(checkedApplication(repository.application(id)))
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
