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
        vm.pendingLogin = false
        if(UserManager.isLogin) { vm.refresh() }
    }
    private val profile = host.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refresh()
    }
    init {
        vm.state.observe(host) { render(it) }
        vm.busy.observe(host) { button.isEnabled = it != true && (vm.state.value != null || vm.error.value != null || !UserManager.isLogin) }
        vm.error.observe(host) { error -> if(error != null) {
            clearDialog(); vm.event.value = null
            if(error.status in listOf(401,403,404)) vm.state.value = null
            host.notice(error.message); render(vm.state.value)
            when(error.errorCode) {
                "IDEMPOTENCY_CONFLICT" -> { vm.resetAttempt(); vm.refresh() }
                "PROFILE_REQUIRED" -> { vm.event.value=AdoptionDetailEvent.EditProfile; consumeEvent() }
                "PROFILE_CHANGED" -> { host.notice("资料已更新，请重新查看并确认"); vm.prepare() }
                else -> if(error.status == 409) vm.refresh()
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
            AdoptionDetailEvent.EditProfile -> {
                dialog = AlertDialog.Builder(host).setMessage("没有领养资料，需要填写后再申请。")
                    .setNegativeButton("取消", null).setPositiveButton("去申请") { d,_ ->
                        d.dismiss(); profile.launch(Intent(host, AdoptionProfileActivity::class.java))
                    }.show()
            }
            AdoptionDetailEvent.Applied -> host.notice("申请成功，等待送养人反馈。")
            is AdoptionDetailEvent.Notice -> host.notice(event.message)
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
            state.workflow_status !in listOf("open", "adopted", "closed") -> "状态暂不可用，点击重试"
            state.can_manage -> "管理申请"
            state.workflow_status == "adopted" -> "完成领养"
            state.workflow_status == "closed" -> "结束领养"
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
        if(state.workflow_status !in listOf("open","adopted","closed")) { refresh();return }
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
            else -> refresh()
        }
    }
    private fun confirm(profileData: ProfileData) {
        val root = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL; setPadding(32,16,32,16) }
        root.label("请仔细阅读《领养说明》，不要相信任何理由的提前转账要求，如定金、运费等。若是红包领养，请当面给送养人。领养更多是一种爱心行为，一些必要的程序，如领养协议、互换身份证复印件等必不可少。宠物是生命不是物品或工具，一切领养活动都应在为生命负责的态度下进行。你的领养资料将提供给送养人。")
        root.label(AdoptionLabels.summary(profileData.profile))
        root.button("《领养说明》") {
            clearDialog()
            host.startActivity(Intent(host, com.rescue.flutter_720yun.user.activity.UserAccountSafeActivity::class.java)
                .putExtra("localUrl", "file:///android_asset/lyinstruction.html").putExtra("title", "领养说明"))
        }
        dialog = AlertDialog.Builder(host).setTitle("领养须知").setView(root)
            .setPositiveButton("继续申请") { d,_ -> d.dismiss(); vm.submit(profileData, "") }
            .setNegativeButton("取消",null).show()
    }
    private fun end() {
        dialog = AlertDialog.Builder(host).setMessage("确认放弃申请吗？")
            .setPositiveButton("确认") { d,_ -> d.dismiss(); vm.end("") }.setNegativeButton("取消",null).show()
    }
    private fun showContact(app: AdoptionApplication) {
        val contact = AdoptionContact.visible(app.topic)
        if(contact == null) { host.notice("当前没有可显示的联系方式，请查看我的申请"); return }
        dialog = AlertDialog.Builder(host).setTitle("送养人联系方式")
            .setMessage(contact + "\n\n" + AdoptionContactCopy.SAFETY_TIP)
            .setPositiveButton("复制") { _,_ -> AdoptionContactCopy.copy(host, vm, app.application_id) }
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
                if(!state.can_manage || state.workflow_status!="open") throw com.rescue.flutter_720yun.adoption.repository.AdoptionError(409,message="帖子状态已变化，请刷新")
                val version=state.workflow_version ?: throw com.rescue.flutter_720yun.adoption.repository.AdoptionError(409,message="流程版本暂不可用，请刷新")
                val fingerprint="${vm.topic}:close:$version"
                vm.repository.topicAction(vm.topic,TopicAction("close",true,version),vm.operationKey(fingerprint))
                vm.operationSucceeded(fingerprint)
                vm.state.value=vm.repository.state(vm.topic); host.reloadAdoptionTopic()
            } }.setNegativeButton("取消",null).show()
    }
    fun clearDialog() { dialog?.dismiss(); dialog=null }
}
