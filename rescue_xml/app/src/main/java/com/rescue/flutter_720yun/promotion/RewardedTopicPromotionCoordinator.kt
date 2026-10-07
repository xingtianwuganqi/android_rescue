package com.rescue.flutter_720yun.promotion

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.ads.*
import com.rescue.flutter_720yun.adoption.repository.AdoptionError
import com.rescue.flutter_720yun.promotion.models.*
import com.rescue.flutter_720yun.promotion.repository.*
import com.rescue.flutter_720yun.util.UserManager
import kotlinx.coroutines.*
import java.util.UUID

/** Application-owned writes. SDK callbacks retain the immutable attempt, never a page. */
object RewardedTopicPromotionCoordinator {
    data class Attempt(val account: Int, val revision: Long?, val topic: Int, val scene: String, val key: String) {
        val reward=RewardObservation(account,topic,scene,key)
        var phase="idle"
    }
    data class Update(val account: Int?, val topic: Int?, val phase: String, val message: String?=null)
    val updates=MutableLiveData(Update(null,null,"idle"))
    val changes=MutableLiveData(0L)
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val repository=PromotionRepository()
    private val store by lazy { PendingRewardStore(BaseApplication.context) }
    private var active: Attempt?=null
    private var task: Job?=null
    private val retryWakeJobs=mutableMapOf<String,Job>()
    private val submissionJobs=mutableMapOf<String,Job>()
    private val persistenceWakeJobs=mutableMapOf<String,Job>()
    private val pendingPersistence=mutableMapOf<String,PendingReward>()
    private val submitting=mutableSetOf<String>()
    private var initialized=false
    val busy get()=active!=null || submitting.isNotEmpty()
    fun initialize() {
        if(initialized) return
        initialized=true
        UserManager.sessionRevision.observeForever {
            task?.cancel();retryWakeJobs.values.toList().forEach { it.cancel() };retryWakeJobs.clear(); submissionJobs.values.toList().forEach { it.cancel() };submissionJobs.clear();active=null;submitting.clear()
            persistenceWakeJobs.values.toList().forEach { it.cancel() };persistenceWakeJobs.clear()
            updates.value=Update(UserManager.userId,null,"idle")
            restore()
        }
    }
    private fun current(attempt: Attempt)=attempt.account==UserManager.userId && attempt.revision==UserManager.sessionRevision.value
    private fun publish(a: Attempt,phase: String,message: String?=null) {
        a.phase=phase
        if(current(a)) updates.value=Update(a.account,a.topic,phase,message)
    }
    fun start(host: FragmentActivity,topic: Int,scene: String) {
        initialize()
        val account=UserManager.userId ?: return
        if(topic<=0 || scene !in listOf("my_posts","publish_success") || busy || TakuAds.fullscreenPresented) return
        val a=Attempt(account,UserManager.sessionRevision.value,topic,scene,UUID.randomUUID().toString())
        active=a; publish(a,"preparing")
        // A weak host reference lets the request finish without retaining a destroyed page.
        val hostRef=java.lang.ref.WeakReference(host)
        task=scope.launch {
            try {
                val state=repository.state(topic)
                if(!current(a) || active!=a) return@launch
                if(!state.can_promote) { active=null;publish(a,"failed",PromotionPolicy.reason(state.reason));return@launch }
                val page=hostRef.get()
                if(page==null || !page.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                    active=null;publish(a,"cancelled");return@launch
                }
                val controller=FullscreenAdController(page,page,onRewardEvent={ sdkEvent -> event(a,sdkEvent) })
                if(!controller.loadRewarded()) { active=null;publish(a,"failed","视频暂不可用，请稍后重试");return@launch }
                // Loading timeout releases the application gate without inventing a reward.
                scope.launch { delay(30000); if(active==a && a.phase=="preparing") {
                    active=null;publish(a,"failed","视频加载超时，请重试")
                } }
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { if(active==a) active=null;publish(a,"failed",(e as? AdoptionError)?.message ?: "推广资格查询失败，请重试") }
        }
    }
    private fun event(a: Attempt,event: RewardedAdEvent) {
        when(event) {
            is RewardedAdEvent.Loaded -> {
                if(active==a && current(a) && event.show()) publish(a,"playing")
                else if(active==a) { active=null;publish(a,"cancelled","请返回页面后重新观看") }
            }
            RewardedAdEvent.Reward -> {
                // Late reward still belongs to this attempt, even after close or account switch.
                val record=a.reward.receive(RewardObservation.Signal.REWARD,System.currentTimeMillis()) ?: return
                if(!persist(record)) {
                    if(active==a) active=null
                    publish(a,"storage_pending","奖励暂未保存，将重试保存，请保持应用打开，无需再看视频")
                    schedulePersistence(record)
                    return
                }
                publish(a,"reward_observed")
                if(current(a)) submit(a,record) else if(UserManager.userId==a.account) restore()
            }
            RewardedAdEvent.Closed -> {
                if(active==a && a.key !in submitting) active=null
                if(!a.reward.observed) publish(a,"cancelled")
            }
            RewardedAdEvent.Failed -> {
                if(active==a) active=null
                if(!a.reward.observed) publish(a,"failed","视频加载或播放失败，请重试")
            }
            RewardedAdEvent.Destroyed -> {
                if(active==a && a.key !in submitting) active=null
                if(!a.reward.observed) publish(a,"cancelled")
            }
            RewardedAdEvent.PlayEnded -> Unit
        }
    }
    private fun persist(record: PendingReward, attempts: Int = 1): Boolean {
        pendingPersistence[record.reward_attempt_id] = record
        repeat(attempts) {
            if(runCatching { store.put(record) }.getOrDefault(false)) {
                pendingPersistence.remove(record.reward_attempt_id)
                return true
            }
        }
        return false
    }
    private fun schedulePersistence(record: PendingReward) {
        if(record.account_id!=UserManager.userId || persistenceWakeJobs.containsKey(record.reward_attempt_id)) return
        val job=scope.launch(start=CoroutineStart.LAZY) {
            delay(1000)
            persistenceWakeJobs.remove(record.reward_attempt_id)
            if(record.account_id==UserManager.userId) restore()
        }
        persistenceWakeJobs[record.reward_attempt_id]=job
        job.start()
    }
    private fun submit(a: Attempt,record: PendingReward) {
        if(!current(a) || !submitting.add(a.key)) return
        publish(a,"submitting")
        val job=scope.launch(start=CoroutineStart.LAZY) {
            try {
                if(record.next_retry_at>0) repository.state(record.topic_id)
                // Persist immediately before POST, after any recovery status request.
                // This checkpoint protects a restart if saving Retry-After fails.
                val checkpoint=record.copy(next_retry_at=maxOf(record.next_retry_at,
                    System.currentTimeMillis()+PromotionPolicy.UNKNOWN_RESULT_BACKOFF).coerceAtMost(record.retry_until))
                if(!persist(checkpoint,2)) {
                    publish(a,"storage_pending","奖励保存失败，已暂停提交，将重试保存，无需再看视频")
                    schedulePersistence(checkpoint)
                    return@launch
                }
                repository.submit(record)
                if(!current(a)) return@launch
                store.remove(a.key)
                pendingPersistence.remove(a.key)
                changes.value=(changes.value ?: 0)+1
                val state=runCatching { repository.state(a.topic) }.getOrNull()
                publish(a,"succeeded",when {
                    state==null -> "本次推广已处理，请刷新确认当前状态"
                    state.promotion?.is_active==true -> "推广成功 · 截至"+PromotionDisplay.time(state.promotion.effective_until)
                    else -> "本次推广已处理，当前推广已结束"
                })
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) {
                val error=e as? AdoptionError
                val retry=PromotionPolicy.retryable(error?.status ?: 0)
                val seconds=(error?.retryAfter ?: 60).coerceAtLeast(1)
                val updated=record.copy(next_retry_at=System.currentTimeMillis()+seconds*1000,
                    retry_state=if(retry || error?.status==401) "pending" else "stopped")
                val saved=persist(updated,2)
                if(!saved) schedulePersistence(updated)
                if(error?.status in listOf(403,404,409) && current(a)) {
                    try { repository.state(a.topic) }
                    catch(e: CancellationException) { throw e }
                    catch(e: Exception) { /* The business refusal still remains authoritative. */ }
                    if(current(a)) changes.value=(changes.value ?: 0)+1
                }
                publish(a,if(retry && saved) "retry_pending" else if(retry) "storage_pending" else "failed",
                    if(retry && saved) "奖励已保存，将使用本次观看记录重试，无需再看视频"
                    else if(retry) "重试时间暂未保存，已暂停提交，请保持应用打开"
                    else error?.message ?: "推广失败")
                if(retry && saved && updated.next_retry_at<record.retry_until) scheduleRetry(updated)
            } finally {
                if(submissionJobs[a.key]===coroutineContext[Job]) {
                    submitting.remove(a.key);submissionJobs.remove(a.key)
                }
                if(active==a) active=null
                if(current(a)) updates.value=updates.value?.copy()
            }
        }
        submissionJobs[a.key]=job
        job.start()
    }
    private fun scheduleRetry(record: PendingReward) {
        if(record.account_id!=UserManager.userId || retryWakeJobs.containsKey(record.reward_attempt_id)) return
        retryWakeJobs[record.reward_attempt_id]=scope.launch {
            delay((record.next_retry_at-System.currentTimeMillis()).coerceAtLeast(1))
            retryWakeJobs.remove(record.reward_attempt_id)
            if(record.account_id==UserManager.userId) restore()
        }
    }
    fun restore() {
        val account=UserManager.userId ?: return
        val now=System.currentTimeMillis()
        (store.records()+pendingPersistence.values).associateBy { it.reward_attempt_id }.values
            .filter { it.account_id==account }.forEach { record ->
            if(record.reward_attempt_id in submitting) return@forEach
            if(record.reward_attempt_id in pendingPersistence && !persist(record,2)) {
                if(record.retry_until>now) schedulePersistence(record)
                updates.value=Update(account,record.topic_id,"storage_pending","奖励记录暂未保存，已暂停提交，请保持应用打开")
                return@forEach
            }
            if(record.retry_until<=now && record.retry_state=="pending") {
                store.put(record.copy(retry_state="expired"))
                val revision=UserManager.sessionRevision.value
                scope.launch {
                    runCatching { repository.state(record.topic_id) }
                    if(UserManager.userId==account && UserManager.sessionRevision.value==revision) updates.value=Update(account,record.topic_id,"failed","奖励恢复已超过24小时，请刷新确认推广状态")
                }
            } else if(record.retry_state=="pending" && record.next_retry_at>now && record.next_retry_at<record.retry_until) {
                scheduleRetry(record)
            } else if(PromotionPolicy.recoverable(record,account,now)) {
                submit(Attempt(account,UserManager.sessionRevision.value,record.topic_id,record.scene,record.reward_attempt_id),record)
            }
        }
    }
}
