package com.rescue.flutter_720yun.adoption.ui

import android.content.Intent
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.activity.HomeDetailActivity
import com.rescue.flutter_720yun.home.activity.LoginActivity
import com.rescue.flutter_720yun.adoption.activity.*
import com.rescue.flutter_720yun.adoption.viewmodels.*
import com.rescue.flutter_720yun.adoption.models.*
import com.rescue.flutter_720yun.adoption.repository.AdoptionError
import com.rescue.flutter_720yun.util.UserManager

/** Private dialogs and contact responses live only on the foreground detail page. */
class AdoptionDetailFlow(private val host: HomeDetailActivity, private val button: TextView,
    private val complete: () -> Boolean) {
    private val vm = ViewModelProvider(host)[AdoptionDetailViewModel::class.java]
    private var dialog: AlertDialog? = null
    private val login = host.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (!UserManager.isLogin) vm.pendingLogin = false
        continueLogin()
        if (!vm.pendingLogin) refresh()
    }
    private val profile = host.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh() }
    init {
        vm.state.observe(host) { render(it) }
        vm.busy.observe(host) { render(vm.state.value); continueLogin() }
        vm.error.observe(host) { error ->
            render(vm.state.value)
            if (error != null && !vm.suppressErrorDialog && host.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) handleError(error,false)
        }
        vm.event.observe(host) {
            if (it != null && !host.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) vm.event.value = null
            else consumeEvent()
        }
        host.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) { continueLogin(); consumeEvent() }
        })
        UserManager.sessionRevision.observe(host) { clearDialog(); render(null) }
        host.supportFragmentManager.setFragmentResultListener("adoption_changed",host) { _,_ -> host.reloadAdoptionTopic() }
        button.setOnClickListener { click() }
    }
    private fun continueLogin() {
        if (vm.pendingLogin && UserManager.isLogin && vm.busy.value != true && host.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            vm.pendingLogin = false; vm.open()
        }
    }
    private fun consumeEvent() {
        if (!host.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        val event = vm.event.value ?: return
        vm.event.value = null
        when(event) {
            AdoptionDetailEvent.EditProfile -> requireProfile()
            AdoptionDetailEvent.Applied -> host.notice(host.getString(R.string.adoption_apply_success_wait))
            AdoptionDetailEvent.Manage -> manage()
            is AdoptionDetailEvent.Failure -> handleError(event.error,event.submission)
            is AdoptionDetailEvent.Notice -> message(event.title,event.message)
            is AdoptionDetailEvent.Confirm -> confirm(event.profile)
            is AdoptionDetailEvent.Contact -> showApplication(event.value,event.submitted)
        }
    }
    fun updateButton() { render(vm.state.value) }
    fun setTopic(topic: Int) { vm.topic = topic }
    fun refresh() { if (UserManager.isLogin && vm.topic > 0) vm.refresh() else render(null) }
    private fun render(state: ApplicationState?) {
        button.text = when {
            !UserManager.isLogin -> if (complete()) host.getString(R.string.adoption_workflow_ended) else host.getString(R.string.adoption_apply)
            state != null -> AdoptionDetailPolicy.title(state)
            vm.error.value != null -> if (vm.error.value?.status == 404) host.getString(R.string.adoption_topic_unavailable) else host.getString(R.string.adoption_status_load_failed_retry)
            else -> host.getString(R.string.adoption_querying_status)
        }
        button.isEnabled = vm.busy.value != true
    }
    private fun click() {
        if (vm.busy.value == true) return
        if (!UserManager.isLogin) {
            com.rescue.flutter_720yun.analytics.AnalyticsManager.loginClick()
            vm.pendingLogin = true; login.launch(Intent(host,LoginActivity::class.java)); return
        }
        // Never branch on a state cached before this tap.
        vm.open()
    }
    private fun requireProfile() {
        clearDialog()
        dialog = AlertDialog.Builder(host).setTitle(host.getString(R.string.adoption_profile_title)).setMessage(host.getString(R.string.adoption_no_profile_to_apply))
            .setNegativeButton(host.getString(R.string.cancel),null).setPositiveButton(host.getString(R.string.adoption_go_apply)) { d,_ ->
                d.dismiss(); profile.launch(Intent(host,AdoptionProfileActivity::class.java))
            }.show()
    }
    private fun message(title: String?, text: String) {
        clearDialog()
        dialog = AlertDialog.Builder(host).setTitle(title).setMessage(text).setPositiveButton(host.getString(R.string.adoption_got_it),null).show()
    }
    private fun confirm(profileData: ProfileData) {
        clearDialog()
        val instruction = host.getString(R.string.adoption_guide_title)
        val warning = host.getString(R.string.adoption_guide_warning)
        val text = host.getString(R.string.adoption_guide_full,instruction,warning)
        val styled = android.text.SpannableString(text).apply {
            val start = text.indexOf(instruction)
            setSpan(object : android.text.style.ClickableSpan() {
                override fun onClick(widget: android.view.View) {
                    clearDialog()
                    host.startActivity(Intent(host,com.rescue.flutter_720yun.user.activity.WebPageActivity::class.java)
                        .putExtra("webUrl",com.rescue.flutter_720yun.util.AppBuildConfig.BASEURL.trimEnd('/') + com.rescue.flutter_720yun.util.AppBuildConfig.INSTRUCTION))
                }
                override fun updateDrawState(ds: android.text.TextPaint) { ds.color = ContextCompat.getColor(host,R.color.color_url); ds.isUnderlineText = false }
            },start,start+instruction.length,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            val noticeStart = text.indexOf(warning)
            setSpan(android.text.style.ForegroundColorSpan(ContextCompat.getColor(host,R.color.color_remind)),noticeStart,noticeStart+warning.length,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val body = TextView(host).apply {
            this.text = styled; textSize = 16f; setTextColor(ContextCompat.getColor(host,R.color.color_content))
            val padding = (20*resources.displayMetrics.density).toInt(); setPadding(padding,padding,padding,padding)
            movementMethod = android.text.method.LinkMovementMethod.getInstance(); setLineSpacing(2*resources.displayMetrics.density,1f)
        }
        // Wrap short content; cap long content so the bottom button panel always stays visible.
        val maxHeight = (host.resources.displayMetrics.heightPixels * 0.55f).toInt()
        val scroll = com.rescue.flutter_720yun.views.MaxHeightScrollView(host).apply {
            this.maxHeight = maxHeight
            addView(body)
        }
        (host.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager)
            ?.hideSoftInputFromWindow(host.window.decorView.windowToken,0)
        val builder = AlertDialog.Builder(host).setTitle(host.getString(R.string.adoption_notice_title)).setView(scroll)
            .setNegativeButton(host.getString(R.string.cancel),null)
            .setPositiveButton(host.getString(R.string.adoption_continue_apply)) { d,_ -> d.dismiss(); vm.submit(profileData,"") }
        val noticeDialog = builder.create()
        dialog = noticeDialog
        noticeDialog.show()
    }
    private fun showApplication(app: AdoptionApplication, submitted: Boolean) {
        clearDialog()
        val contact = AdoptionContact.visible(app.topic)
        if (submitted && app.status == "applying") { host.notice(host.getString(R.string.adoption_apply_success_wait)); return }
        if (submitted && app.status !in listOf("applying","communicating")) {
            message(host.getString(R.string.adoption_status_changed),host.getString(R.string.adoption_no_new_application)); return
        }
        val body = if (submitted) contact?.let { host.getString(R.string.adoption_contact_available_format,it) }
            ?: host.getString(R.string.adoption_apply_success_contact_later)
        else (when (app.status) {
            "applying" -> host.getString(R.string.adoption_applied_wait_owner)
            "communicating" -> host.getString(R.string.adoption_contact_available)
            else -> AdoptionDetailPolicy.title(app)
        }) + (contact?.let { host.getString(R.string.adoption_contact_line,it) } ?: "")
        val builder = AlertDialog.Builder(host).setTitle(if (submitted) host.getString(R.string.adoption_apply_success) else AdoptionDetailPolicy.title(app))
            .setMessage(body).setNegativeButton(host.getString(R.string.common_close),null)
        if (contact != null) builder.setPositiveButton(host.getString(R.string.common_copy_contact)) { _,_ ->
            // Re-read the acquisition fields before writing any private value to the clipboard.
            AdoptionContactCopy.copy(host,vm,app.application_id)
        }
        if (!submitted && AdoptionDetailPolicy.canReapply(vm.state.value,app)) builder.setNeutralButton(host.getString(R.string.adoption_reapply)) { _,_ -> vm.resetAttempt(); vm.prepare() }
        dialog = builder.show()
    }
    private fun handleError(error: AdoptionError, submission: Boolean) {
        clearDialog()
        if (submission && error.status != 401 && error.code != 401) refresh()
        when {
            error.status == 401 || error.code == 401 -> { vm.pendingLogin = false; login.launch(Intent(host,LoginActivity::class.java)) }
            error.errorCode == "PROFILE_REQUIRED" -> requireProfile()
            error.errorCode == "PROFILE_CHANGED" -> {
                vm.resetAttempt(); refresh(); message(host.getString(R.string.adoption_profile_updated),host.getString(R.string.adoption_profile_changed_reapply))
            }
            submission && error.code in listOf(209,210) -> {
                val unbound = error.code == 209
                dialog = AlertDialog.Builder(host).setTitle(host.getString(R.string.adoption_apply_failed)).setMessage(host.getString(if (unbound) R.string.adoption_phone_not_bound else R.string.adoption_phone_not_verified))
                    .setNegativeButton(host.getString(R.string.cancel),null).setPositiveButton(host.getString(if (unbound) R.string.adoption_go_bind else R.string.adoption_go_verify)) { _,_ ->
                        login.launch(Intent(host,LoginActivity::class.java).putExtra("type",if (unbound) "bindPhone" else "checkPhone"))
                    }.show()
            }
            else -> {
                if (error.errorCode == "IDEMPOTENCY_CONFLICT") vm.resetAttempt()
                val generic = error.message in listOf(host.getString(R.string.adoption_operation_failed_refresh),host.getString(R.string.adoption_access_denied),"操作失败，请刷新重试","拒绝访问")
                val text = if (generic && (error.errorCode == "BLACKLISTED" || error.code == 202)) host.getString(R.string.adoption_account_restricted_apply)
                    else if (generic && submission) host.getString(R.string.adoption_apply_failed_later) else error.message
                if (error.status == 409) refresh()
                message(if (submission) host.getString(R.string.adoption_apply_failed) else null,text)
            }
        }
    }
    fun disappeared() { clearDialog(); vm.event.value = null; vm.cancelWork() }
    fun manage() { if(vm.state.value?.can_manage == true) host.startActivity(Intent(host, AdoptionApplicationsActivity::class.java).putExtra("topic_id",vm.topic)) }
    fun ownerMore() {
        val state = vm.state.value ?: run { refresh(); return }
        if(!state.can_manage) { host.notice(host.getString(R.string.adoption_no_manage_permission)); return }
        dialog = AlertDialog.Builder(host).setTitle(host.getString(R.string.adoption_manage_workflow)).setItems(arrayOf(host.getString(R.string.adoption_manage_applications_desc), host.getString(R.string.adoption_close_workflow), host.getString(R.string.adoption_reopen_workflow), host.getString(R.string.adoption_delete_post))) { _,i -> when(i) {
            0 -> manage()
            1 -> closeTopic()
            2 -> vm.run {
                val current = vm.repository.state(vm.topic); vm.state.value = current
                if(current.can_manage && current.application_count == 0 && current.workflow_status == "adopted") host.reopenLegacyTopic()
                else host.notice(host.getString(R.string.adoption_reopen_not_allowed))
            }
            3 -> host.confirmDeleteTopic()
        } }.show()
    }
    private fun closeTopic() {
        dialog = AlertDialog.Builder(host).setTitle(host.getString(R.string.adoption_confirm_close_workflow_title)).setMessage(host.getString(R.string.adoption_confirm_close_workflow_desc))
            .setPositiveButton(host.getString(R.string.adoption_confirm_close)) { _,_ -> vm.run {
                val state=vm.repository.state(vm.topic)
                if(!state.can_manage || state.workflow_status!="open") throw com.rescue.flutter_720yun.adoption.repository.AdoptionError(409,message=host.getString(R.string.adoption_topic_status_changed))
                val version=state.workflow_version ?: throw com.rescue.flutter_720yun.adoption.repository.AdoptionError(409,message=host.getString(R.string.adoption_flow_version_unavailable))
                val fingerprint="${vm.topic}:close:$version"
                vm.repository.topicAction(vm.topic,TopicAction("close",true,version),vm.operationKey(fingerprint))
                vm.operationSucceeded(fingerprint)
                vm.state.value=vm.repository.state(vm.topic); host.reloadAdoptionTopic()
            } }.setNegativeButton(host.getString(R.string.cancel),null).show()
    }
    fun clearDialog() { dialog?.setMessage(null); dialog?.dismiss(); dialog=null }
}
