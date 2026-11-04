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
            AdoptionDetailEvent.Applied -> host.notice("申请成功，等待送养人反馈。")
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
            !UserManager.isLogin -> if (complete()) "送养已结束" else "申请领养"
            state != null -> AdoptionDetailPolicy.title(state)
            vm.error.value != null -> if (vm.error.value?.status == 404) "帖子不可用" else "状态加载失败 · 重试"
            else -> "正在查询状态"
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
        dialog = AlertDialog.Builder(host).setTitle("领养资料").setMessage("没有领养资料，需要填写后再申请。")
            .setNegativeButton("取消",null).setPositiveButton("去申请") { d,_ ->
                d.dismiss(); profile.launch(Intent(host,AdoptionProfileActivity::class.java))
            }.show()
    }
    private fun message(title: String?, text: String) {
        clearDialog()
        dialog = AlertDialog.Builder(host).setTitle(title).setMessage(text).setPositiveButton("知道了",null).show()
    }
    private fun confirm(profileData: ProfileData) {
        clearDialog()
        val instruction = "《领养说明》"
        val warning = "不要相信任何理由的提前转账要求，如定金、运费等。若是红包领养，请当面给送养人。"
        val text = "请仔细阅读${instruction}，${warning}领养更多是一种爱心行为，一些必要的程序，如领养协议、互换身份证复印件等必不可少。宠物是生命不是物品或工具，一切领养活动都应在为生命负责的态度下进行。"
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
        val scroll = android.widget.ScrollView(host).apply { addView(body) }
        dialog = AlertDialog.Builder(host).setTitle("领养须知").setView(scroll)
            .setNegativeButton("取消",null).setPositiveButton("继续申请") { d,_ -> d.dismiss(); vm.submit(profileData,"") }.show()
    }
    private fun showApplication(app: AdoptionApplication, submitted: Boolean) {
        clearDialog()
        val contact = AdoptionContact.visible(app.topic)
        if (submitted && app.status == "applying") { host.notice("申请成功，等待送养人反馈。"); return }
        if (submitted && app.status !in listOf("applying","communicating")) {
            message("申请状态已变化","未创建新的有效申请，请刷新后重新申请。"); return
        }
        val body = if (submitted) contact?.let { "可联系送养人\n送养人联系方式：$it" }
            ?: "申请成功，联系方式暂不可用，请稍后在「我的申请」中查看。"
        else (when (app.status) {
            "applying" -> "已申请，等待送养人同意"
            "communicating" -> "可联系送养人"
            else -> AdoptionDetailPolicy.title(app)
        }) + (contact?.let { "\n送养人联系方式：$it" } ?: "")
        val builder = AlertDialog.Builder(host).setTitle(if (submitted) "申请成功" else AdoptionDetailPolicy.title(app))
            .setMessage(body).setNegativeButton("关闭",null)
        if (contact != null) builder.setPositiveButton("复制联系方式") { _,_ ->
            // Re-read the acquisition fields before writing any private value to the clipboard.
            AdoptionContactCopy.copy(host,vm,app.application_id)
        }
        if (!submitted && AdoptionDetailPolicy.canReapply(vm.state.value,app)) builder.setNeutralButton("重新申请") { _,_ -> vm.resetAttempt(); vm.prepare() }
        dialog = builder.show()
    }
    private fun handleError(error: AdoptionError, submission: Boolean) {
        clearDialog()
        if (submission && error.status != 401 && error.code != 401) refresh()
        when {
            error.status == 401 || error.code == 401 -> { vm.pendingLogin = false; login.launch(Intent(host,LoginActivity::class.java)) }
            error.errorCode == "PROFILE_REQUIRED" -> requireProfile()
            error.errorCode == "PROFILE_CHANGED" -> {
                vm.resetAttempt(); refresh(); message("资料已更新","领养资料已变化，请重新点击申请领养。")
            }
            submission && error.code in listOf(209,210) -> {
                val unbound = error.code == 209
                dialog = AlertDialog.Builder(host).setTitle("申请失败").setMessage(if (unbound) "未绑定手机号" else "未验证手机号")
                    .setNegativeButton("取消",null).setPositiveButton(if (unbound) "去绑定" else "去验证") { _,_ ->
                        login.launch(Intent(host,LoginActivity::class.java).putExtra("type",if (unbound) "bindPhone" else "checkPhone"))
                    }.show()
            }
            else -> {
                if (error.errorCode == "IDEMPOTENCY_CONFLICT") vm.resetAttempt()
                val generic = error.message in listOf("操作失败，请刷新重试","拒绝访问")
                val text = if (generic && (error.errorCode == "BLACKLISTED" || error.code == 202)) "当前账号受限制，无法申请领养。"
                    else if (generic && submission) "申请失败，请稍后重试。" else error.message
                if (error.status == 409) refresh()
                message(if (submission) "申请失败" else null,text)
            }
        }
    }
    fun disappeared() { clearDialog(); vm.event.value = null; vm.cancelWork() }
    fun manage() { if(vm.state.value?.can_manage == true) host.startActivity(Intent(host, AdoptionApplicationsActivity::class.java).putExtra("topic_id",vm.topic)) }
    fun ownerMore() {
        val state = vm.state.value ?: run { refresh(); return }
        if(!state.can_manage) { host.notice("当前账号无权管理此帖"); return }
        dialog = AlertDialog.Builder(host).setTitle("管理送养").setItems(arrayOf("管理申请 / 选择接宠人完成送养", "关闭送养", "重新送养", "删除帖子")) { _,i -> when(i) {
            0 -> manage()
            1 -> closeTopic()
            2 -> vm.run {
                val current = vm.repository.state(vm.topic); vm.state.value = current
                if(current.can_manage && current.application_count == 0 && current.workflow_status == "adopted") host.reopenLegacyTopic()
                else host.notice("有申请历史或已关闭的帖子不能重开，请重新发布")
            }
            3 -> host.confirmDeleteTopic()
        } }.show()
    }
    private fun closeTopic() {
        dialog = AlertDialog.Builder(host).setTitle("关闭送养？").setMessage("所有有效申请将结束，不会记录为完成送养。")
            .setPositiveButton("确认关闭") { _,_ -> vm.run {
                val state=vm.repository.state(vm.topic)
                if(!state.can_manage || state.workflow_status!="open") throw com.rescue.flutter_720yun.adoption.repository.AdoptionError(409,message="帖子状态已变化，请刷新")
                val version=state.workflow_version ?: throw com.rescue.flutter_720yun.adoption.repository.AdoptionError(409,message="流程版本暂不可用，请刷新")
                val fingerprint="${vm.topic}:close:$version"
                vm.repository.topicAction(vm.topic,TopicAction("close",true,version),vm.operationKey(fingerprint))
                vm.operationSucceeded(fingerprint)
                vm.state.value=vm.repository.state(vm.topic); host.reloadAdoptionTopic()
            } }.setNegativeButton("取消",null).show()
    }
    fun clearDialog() { dialog?.setMessage(null); dialog?.dismiss(); dialog=null }
}
