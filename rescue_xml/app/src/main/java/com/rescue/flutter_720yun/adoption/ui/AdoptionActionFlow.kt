package com.rescue.flutter_720yun.adoption.ui

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.rescue.flutter_720yun.adoption.models.*
import com.rescue.flutter_720yun.adoption.repository.AdoptionError
import com.rescue.flutter_720yun.adoption.viewmodels.AdoptionViewModel
import com.rescue.flutter_720yun.util.UserManager

/** Both list identities use the same validation and write paths. */
class AdoptionActionFlow(private val host: FragmentActivity, private val vm: AdoptionViewModel,
    private val mine: Boolean, private val changed: () -> Unit) {
    private var dialog: AlertDialog? = null
    fun dismiss() { dialog?.dismiss(); dialog=null }
    fun perform(original: AdoptionApplication, action: String) {
        if(vm.busy.value==true || dialog!=null) return
        val revision=UserManager.sessionRevision.value
        vm.run {
            val app=vm.repository.application(original.application_id)
            if(app.topic?.topic_id != original.topic?.topic_id || action !in AdoptionOperations.actions(app,mine))
                throw AdoptionError(409,message="申请状态已变化，请刷新后重试")
            if(mine && app.applicant?.user_id != UserManager.userId) throw AdoptionError(403,message="无申请权限")
            if(!mine) {
                val state=vm.repository.state(app.topic!!.topic_id)
                if(!state.can_manage || state.workflow_status != "open") throw AdoptionError(409,message="当前帖子不能执行此操作")
            }
            if(!host.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) return@run
            val message=when(action) {
                "communicate" -> "同意沟通将会把联系方式显示给申请人，确认沟通吗"
                "reject" -> "确认拒绝该申请人的沟通申请吗？"
                "complete" -> "确认该申请人已实际接走宠物并完成领养吗？确认后，这条送养帖子将结束，其他进行中的申请也会自动结束。"
                "end" -> "确认结束与该申请人的沟通吗？"
                else -> "确认放弃申请吗？"
            }
            val box=android.widget.LinearLayout(host).apply { orientation=android.widget.LinearLayout.VERTICAL;setPadding(32,16,32,16);label(message) }
            val note=if(action=="end") box.field("结束说明（选填，最多200字）") else null
            dialog=AlertDialog.Builder(host).setView(box).setNegativeButton("取消") { _,_ -> dialog=null }
                .setPositiveButton("确认") { d,_ ->
                    d.dismiss(); dialog=null
                    if(revision==UserManager.sessionRevision.value) vm.run {
                        val fresh=vm.repository.application(app.application_id)
                        if(fresh.version != app.version || fresh.topic?.topic_id != app.topic?.topic_id || action !in AdoptionOperations.actions(fresh,mine))
                            throw AdoptionError(409,message="申请状态已变化，请重新确认")
                        var fingerprint="${app.application_id}:$action:${fresh.version}:${note?.text?.toString()?.trim().orEmpty()}"
                        if(action=="complete") {
                            val state=vm.repository.state(app.topic!!.topic_id)
                            if(!state.can_manage || state.workflow_status!="open" || state.workflow_version==null)
                                throw AdoptionError(409,message="送养状态已变化，请刷新")
                            fingerprint="${app.application_id}:complete:${state.workflow_version}"
                            vm.repository.topicAction(app.topic.topic_id,TopicAction("complete",true,state.workflow_version,app.application_id),vm.operationKey(fingerprint))
                        } else vm.repository.action(app.application_id,ApplicationAction(if(action=="communicate") "communicate" else "end",fresh.version,
                            if(action=="reject") "拒绝沟通申请" else note?.text?.toString()?.trim()),vm.operationKey(fingerprint))
                        vm.operationSucceeded(fingerprint)
                        AdoptionNotificationChanges.changed()
                        host.notice("操作成功")
                        // Schedule refresh after AdoptionViewModel.run releases its busy flag.
                        host.window.decorView.post { changed() }
                    }
                }.create().apply { setOnDismissListener { dialog=null }; show() }
        }
    }
}
