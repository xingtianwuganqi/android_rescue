package com.rescue.flutter_720yun.promotion

import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.rescue.flutter_720yun.adoption.ui.notice
import com.rescue.flutter_720yun.promotion.models.*
import com.rescue.flutter_720yun.promotion.repository.PromotionRepository
import com.rescue.flutter_720yun.util.UserManager
import kotlinx.coroutines.*

/** One page renders state, the application coordinator owns the rewarded attempt. */
class PromotionEntry(private val host: FragmentActivity, private val button: TextView,
    private val topic: Int, private val scene: String): DefaultLifecycleObserver {
    private val repository=PromotionRepository()
    private var owner=false
    private var workflow: String?=null
    private var state: PromotionState?=null
    private var job: Job?=null
    private var expiry: Job?=null
    private var generation=0L
    private var dialog: AlertDialog?=null
    init {
        button.visibility=View.GONE
        button.setOnClickListener { confirm() }
        host.lifecycle.addObserver(this)
        RewardedTopicPromotionCoordinator.updates.observe(host) { update ->
            render()
            if(update.account==UserManager.userId && update.topic==topic && update.message!=null &&
                host.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                host.notice(update.message)
                RewardedTopicPromotionCoordinator.updates.value=update.copy(message=null)
            }
        }
        RewardedTopicPromotionCoordinator.changes.observe(host) { refresh() }
        UserManager.sessionRevision.observe(host) { dialog?.dismiss();dialog=null;state=null;job?.cancel();expiry?.cancel();generation++;render() }
    }
    fun setOwner(value: Boolean,status: String?=null) {
        val changed=owner!=value || workflow!=status
        owner=value; workflow=status
        render()
        if(changed && owner) refresh()
    }
    fun refresh() {
        if(!owner || !UserManager.isLogin || topic<=0) { state=null;render();return }
        job?.cancel();val operation=++generation
        val revision=UserManager.sessionRevision.value
        job=host.lifecycleScope.launch {
            try {
                val result=repository.state(topic)
                if(operation!=generation || revision!=UserManager.sessionRevision.value) return@launch
                state=result; render()
                expiry?.cancel()
                val remaining=runCatching {
                    java.time.OffsetDateTime.parse(result.promotion?.effective_until).toInstant().toEpochMilli() -
                        java.time.OffsetDateTime.parse(result.server_time).toInstant().toEpochMilli()
                }.getOrNull()
                if(result.promotion?.is_active==true && remaining!=null && remaining>0) expiry=host.lifecycleScope.launch {
                    delay(remaining+500); refresh()
                }
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { if(operation==generation && revision==UserManager.sessionRevision.value) {
                state=null;render();button.text="推广状态加载失败，点击重试"
            } }
        }
    }
    private fun render() {
        button.visibility=if(owner && UserManager.isLogin && topic>0) View.VISIBLE else View.GONE
        button.text=when {
            workflow=="adopted" -> "完成领养"
            workflow=="closed" -> "结束领养"
            state==null -> "推广状态待刷新，点击重试"
            state?.can_promote!=true -> PromotionPolicy.reason(state?.reason)
            state?.promotion?.is_active==true -> "观看视频，延长推广 · 截至${PromotionDisplay.time(state?.promotion?.effective_until)}"
            state?.promotion?.promotion_id!=null -> "推广已结束 · 观看视频，增加曝光"
            else -> "观看视频，增加曝光"
        }
        val terminal=workflow in listOf("adopted","closed")
        button.isEnabled=!terminal && !RewardedTopicPromotionCoordinator.busy
        button.alpha=if(terminal || state?.can_promote==false) 0.5f else 1f
    }
    private fun confirm() {
        if(state==null) { refresh();return }
        if(state?.can_promote!=true) { host.notice(PromotionPolicy.reason(state?.reason));refresh();return }
        dialog=AlertDialog.Builder(host).setMessage("观看视频后，该帖子将获得一小时优先展示。再次观看成功将重新计算推广时间，是否继续？")
            .setNegativeButton("取消") { d,_ -> d.dismiss() }
            .setPositiveButton("观看视频") { d,_ -> d.dismiss();RewardedTopicPromotionCoordinator.start(host,topic,scene) }.show()
    }
    override fun onResume(owner: LifecycleOwner) { RewardedTopicPromotionCoordinator.restore();refresh() }
    override fun onPause(owner: LifecycleOwner) { dialog?.dismiss();dialog=null;job?.cancel();expiry?.cancel();generation++ }
}
