package com.rescue.flutter_720yun.adoption.ui

import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.rescue.flutter_720yun.R
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
                throw AdoptionError(409,message=host.getString(R.string.adoption_status_changed_retry))
            if(mine && app.applicant?.user_id != UserManager.userId) throw AdoptionError(403,message=host.getString(R.string.adoption_no_permission))
            if(!mine) {
                val state=vm.repository.state(app.topic!!.topic_id)
                if(!state.can_manage || state.workflow_status != "open") throw AdoptionError(409,message=host.getString(R.string.adoption_action_unavailable))
            }
            if(!host.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) return@run
            val message=when(action) {
                "communicate" -> host.getString(R.string.adoption_confirm_communicate)
                "reject" -> host.getString(R.string.adoption_confirm_reject_communication)
                "complete" -> host.getString(R.string.adoption_confirm_complete)
                "end" -> host.getString(R.string.adoption_confirm_end_communication)
                else -> host.getString(R.string.adoption_confirm_abandon)
            }
            val box=android.widget.LinearLayout(host).apply { orientation=android.widget.LinearLayout.VERTICAL;setPadding(32,16,32,16);label(message) }
            val note=if(action=="end") box.field(host.getString(R.string.adoption_end_note_hint)) else null
            val actionDialog=AlertDialog.Builder(host).setView(box).setNegativeButton(host.getString(R.string.cancel)) { _,_ -> dialog=null }
                .setPositiveButton(host.getString(R.string.confirm_d)) { d,_ ->
                    d.dismiss(); dialog=null
                    if(revision==UserManager.sessionRevision.value) vm.run {
                        val fresh=vm.repository.application(app.application_id)
                        if(fresh.version != app.version || fresh.topic?.topic_id != app.topic?.topic_id || action !in AdoptionOperations.actions(fresh,mine))
                            throw AdoptionError(409,message=host.getString(R.string.adoption_status_changed_confirm))
                        var fingerprint="${app.application_id}:$action:${fresh.version}:${note?.text?.toString()?.trim().orEmpty()}"
                        if(action=="complete") {
                            val state=vm.repository.state(app.topic!!.topic_id)
                            if(!state.can_manage || state.workflow_status!="open" || state.workflow_version==null)
                                throw AdoptionError(409,message=host.getString(R.string.adoption_workflow_changed_refresh))
                            fingerprint="${app.application_id}:complete:${state.workflow_version}"
                            vm.repository.topicAction(app.topic.topic_id,TopicAction("complete",true,state.workflow_version,app.application_id),vm.operationKey(fingerprint))
                        } else vm.repository.action(app.application_id,ApplicationAction(if(action=="communicate") "communicate" else "end",fresh.version,
                            if(action=="reject") "拒绝沟通申请" else note?.text?.toString()?.trim()),vm.operationKey(fingerprint))
                        vm.operationSucceeded(fingerprint)
                        AdoptionNotificationChanges.changed()
                        host.notice(host.getString(R.string.adoption_operation_success))
                        // Schedule refresh after AdoptionViewModel.run releases its busy flag.
                        host.window.decorView.post { changed() }
                    }
                }.create().apply {
                    window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                    setOnShowListener {
                        getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                            setTextColor(ContextCompat.getColor(host,R.color.color_system));textSize=16f
                        }
                        getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                            setTextColor(ContextCompat.getColor(host,R.color.color_node));textSize=16f
                        }
                    }
                    setOnDismissListener { dialog=null }
                }
            dialog=actionDialog
            actionDialog.show()
        }
    }
}
