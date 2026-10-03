package com.rescue.flutter_720yun.adoption.ui

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.rescue.flutter_720yun.home.activity.HomeDetailActivity
import com.rescue.flutter_720yun.home.activity.LoginActivity
import com.rescue.flutter_720yun.adoption.activity.*
import com.rescue.flutter_720yun.adoption.viewmodels.*
import com.rescue.flutter_720yun.adoption.models.*
import com.rescue.flutter_720yun.util.UserManager

/** Holds dialogs only on the current foreground page, never persists profile/contact in Bundles. */
class AdoptionDetailFlow(private val host: HomeDetailActivity, private val button: android.widget.TextView) {
    private val vm = ViewModelProvider(host)[AdoptionDetailViewModel::class.java]
    private var dialog: AlertDialog? = null
    private val login = host.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val resume = vm.pendingLogin; vm.pendingLogin = false
        if(UserManager.isLogin) { if(resume) vm.prepare() else vm.refresh() }
    }
    private val profile = host.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if(it.resultCode == android.app.Activity.RESULT_OK && UserManager.isLogin) vm.prepare() else refresh()
    }
    init {
        vm.state.observe(host) { render(it) }
        vm.busy.observe(host) { button.isEnabled = it != true && (vm.state.value != null || vm.error.value != null || !UserManager.isLogin) }
        vm.error.observe(host) { error -> if(error != null) {
            clearDialog(); vm.event.value = null
            if(error.http in listOf(401,403,404)) vm.state.value = null
            host.notice(error.message); render(vm.state.value)
            when(error.errorCode) {
                "IDEMPOTENCY_CONFLICT" -> { vm.resetAttempt(); vm.refresh() }
                "PROFILE_REQUIRED" -> profile.launch(Intent(host, AdoptionProfileActivity::class.java))
                "PROFILE_CHANGED" -> { host.notice("资料已更新，请重新查看并确认"); vm.prepare() }
                else -> if(error.http == 409) vm.refresh()
            }
        } }
        vm.event.observe(host) {
            if(it is AdoptionDetailEvent.Contact && !host.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) vm.event.value=null
            else consumeEvent()
        }
        host.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) { consumeEvent() }
        })
        UserManager.sessionRevision.observe(host) { clearDialog(); render(null) }
        host.supportFragmentManager.setFragmentResultListener("adoption_changed",host) { _,_ -> host.reloadAdoptionTopic() }
        button.setOnClickListener { click() }
    }
    private fun consumeEvent() {
        if(!host.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        val event = vm.event.value ?: return
        vm.event.value = null
        when(event) {
            AdoptionDetailEvent.EditProfile -> profile.launch(Intent(host, AdoptionProfileActivity::class.java))
            is AdoptionDetailEvent.Confirm -> confirm(event.profile)
            is AdoptionDetailEvent.Contact -> showContact(event.value)
        }
    }
    fun setTopic(topic: Int) { vm.topic = topic }
    fun refresh() { if(UserManager.isLogin && vm.topic > 0) vm.refresh() else render(null) }
    private fun render(state: ApplicationState?) {
        button.text = when {
            !UserManager.isLogin -> "获取联系方式"
            state == null -> if(vm.error.value != null) "加载失败，点击重试" else "正在检查申请状态"
            state.can_manage -> "管理申请"
            state.application_status == "applying" -> "申请中 · 等待同意"
            state.application_status == "communicating" -> if(state.contact_authorized) "查看联系方式" else "沟通中 · 查看申请"
            state.application_status == "ended" -> AdoptionLabels.status("ended", state.application_result)
            state.can_apply -> "获取联系方式"
            else -> "已关闭或暂不可申请"
        }
        button.isEnabled = vm.busy.value != true
    }
    private fun click() {
        if(!UserManager.isLogin) { vm.pendingLogin = true; login.launch(Intent(host, LoginActivity::class.java)); return }
        val state = vm.state.value ?: run { refresh(); return }
        when {
            state.can_manage -> manage()
            state.application_status == "communicating" -> vm.readContact()
            state.application_status in listOf("applying", "communicating", "ended") -> {
                val choices = mutableListOf("查看申请详情")
                if(state.application_status != "ended") choices.add("放弃申请")
                if(state.can_apply) choices.add("重新申请")
                dialog = AlertDialog.Builder(host).setTitle(AdoptionLabels.status(state.application_status,state.application_result))
                    .setItems(choices.toTypedArray()) { _,i -> when(choices[i]) {
                        "放弃申请" -> end(); "重新申请" -> { vm.resetAttempt(); vm.prepare() }
                        else -> state.application_id?.let { com.rescue.flutter_720yun.adoption.fragment.AdoptionApplicationSheet.newInstance(it, false).show(host.supportFragmentManager, "adoption_application") }
                    } }.show()
            }
            state.can_apply -> vm.prepare()
            else -> host.notice("帖子已关闭或申请入口暂未开放")
        }
    }
    private fun confirm(profileData: ProfileData) {
        val root = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL; setPadding(32,16,32,16) }
        root.label(AdoptionLabels.summary(profileData.profile))
        val statement = root.field("申请说明（选填，最多500字）",500).apply { setText(vm.statementDraft) }
        dialog = AlertDialog.Builder(host).setTitle("你的领养资料将提供给本帖送养人").setView(root)
            .setPositiveButton("确认申请") { _,_ -> vm.submit(profileData, statement.text.toString()) }
            .setNeutralButton("编辑资料") { _,_ -> profile.launch(Intent(host, AdoptionProfileActivity::class.java)) }
            .setNegativeButton("取消",null).show()
    }
    private fun end() {
        val root = LinearLayout(host).apply { orientation=LinearLayout.VERTICAL; setPadding(32,16,32,16) }
        val note = root.field("结束说明（选填，最多200字）")
        dialog = AlertDialog.Builder(host).setTitle("放弃本次申请？").setView(root)
            .setPositiveButton("确认放弃") { _,_ -> vm.end(note.text.toString()) }.setNegativeButton("取消",null).show()
    }
    private fun showContact(app: AdoptionApplication) {
        val contact = AdoptionContact.visible(app.topic)
        if(contact == null) { host.notice("当前没有可显示的联系方式，请查看我的申请"); return }
        dialog = AlertDialog.Builder(host).setTitle("送养人联系方式")
            .setMessage(contact + "\n\n" + AdoptionContactCopy.SAFETY_TIP)
            .setPositiveButton("复制") { _,_ -> dialog = AdoptionContactCopy.prompt(host, vm, app.application_id) }
            .setNegativeButton("关闭", null).show()
    }
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
                vm.repository.topicAction(vm.topic,TopicAction("close",true,state.workflow_version ?: return@run))
                vm.state.value=vm.repository.state(vm.topic); host.reloadAdoptionTopic()
            } }.setNegativeButton("取消",null).show()
    }
    fun clearDialog() { dialog?.dismiss(); dialog=null }
}
