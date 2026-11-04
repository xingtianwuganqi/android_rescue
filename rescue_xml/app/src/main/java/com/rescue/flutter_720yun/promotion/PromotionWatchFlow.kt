package com.rescue.flutter_720yun.promotion

import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.rescue.flutter_720yun.BuildConfig
import com.rescue.flutter_720yun.adoption.ui.notice
import com.rescue.flutter_720yun.promotion.models.PromotionPolicy
import com.rescue.flutter_720yun.promotion.repository.PromotionRepository
import com.rescue.flutter_720yun.util.UserManager
import kotlinx.coroutines.*

/** Eligibility precedes confirmation; a view lifecycle prevents ads appearing on a different tab. */
class PromotionWatchFlow(private val host: FragmentActivity, private val owner: LifecycleOwner) : DefaultLifecycleObserver {
    private val configuration = PromotionConfiguration(host)
    private var job: Job? = null
    private var dialog: AlertDialog? = null
    private var reservation: Any? = null
    private var generation = 0
    init {
        owner.lifecycle.addObserver(this)
        UserManager.sessionRevision.observe(owner) { cancel() }
    }
    private fun visible() = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && !host.isFinishing
    private fun reserve(): Boolean {
        val token = Any()
        if (!RewardedTopicPromotionCoordinator.acquirePreparation(token)) { host.notice("请先完成当前视频或推广提交"); return false }
        reservation = token; return true
    }
    private fun release() { reservation?.let { RewardedTopicPromotionCoordinator.releasePreparation(it) }; reservation = null }
    fun start(topic: Int, scene: String) {
        if (!BuildConfig.ADS_ENABLED || !visible() || topic <= 0) return
        if (!UserManager.isLogin) { host.notice("请先登录"); return }
        com.rescue.flutter_720yun.analytics.AnalyticsManager.rewardClick(topic,scene)
        if (RewardedTopicPromotionCoordinator.hasPendingReward(topic)) {
            host.notice("上次观看奖励正在提交，请稍后重试，无需再看视频")
            RewardedTopicPromotionCoordinator.restore(); return
        }
        if (!reserve()) return
        val operation = ++generation; val revision = UserManager.sessionRevision.value
        job = owner.lifecycleScope.launch {
            try {
                val state = PromotionRepository().state(topic)
                if (operation != generation || revision != UserManager.sessionRevision.value || !visible()) return@launch
                if (!state.can_promote) { host.notice(PromotionPolicy.reason(state.reason)); RewardedTopicPromotionCoordinator.refreshLists(); return@launch }
                configuration.update(state.duration_seconds)
                dialog = AlertDialog.Builder(host).setTitle("增加曝光")
                    .setMessage("观看视频后，该帖子将获得${configuration.duration}优先展示。再次观看成功将重新计算推广时间，是否继续？")
                    .setNegativeButton("取消",null).setPositiveButton("观看视频") { _,_ ->
                        release()
                        if (operation == generation && revision == UserManager.sessionRevision.value && visible())
                            RewardedTopicPromotionCoordinator.start(host,topic,scene,owner)
                    }.create().apply {
                        setOnDismissListener { if (operation == generation) { dialog = null; release() } }
                        show()
                    }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (operation == generation && visible()) host.notice((e as? com.rescue.flutter_720yun.adoption.repository.AdoptionError)?.message ?: "推广资格查询失败，请重试") }
            finally { if (operation == generation && dialog == null) release() }
        }
    }
    fun showIntroduction() {
        if (!BuildConfig.ADS_ENABLED || !visible() || !reserve()) return
        val operation = ++generation
        val text = TextView(host).apply { textSize = 15f; setTextColor(android.graphics.Color.DKGRAY); setPadding(32,24,32,24) }
        fun update() {
            val duration = configuration.duration
            text.text = "1. 如何获得推广\n必须完整观看广告视频，并收到广告平台的奖励成功结果。提前关闭、播放失败或仅播放结束，都不会获得推广。推广接口提交成功后才正式生效。\n\n2. 展示位置与时长\n推广生效后，帖子获得${duration}首页优先展示，刷新首页第一页即可查看。有效推广帖排在普通帖之前，最近一次推广成功的帖子排在最前面。若推广帖子超过一页，部分帖子会展示在后续页，并不保证整个推广期间始终位于第一页。符合城市条件的同城列表也会优先展示。\n\n3. 再次观看与续期\n同一个帖子可以再次完整观看视频。成功后从这次推广生效时间重新计算${duration}，并更新排序；不会新增重复帖子，也不会在原剩余时间上累加${duration}。\n\n4. 何时停止推广\n超过${duration}后恢复普通排序；帖子删除、结束送养或不再符合展示条件时，也不再享有推广展示。推广只增加曝光，不保证浏览量或领养结果。\n\n5. 次数与网络异常\n仅可推广本人符合条件的帖子，每日次数以页面和服务端限制为准。推广暂未开放或次数用完时无法观看。获得奖励后遇到网络异常，会为原账号保留待提交记录，并在24小时内尝试恢复；看到推广成功提示后再刷新首页查看效果。"
        }
        update()
        dialog = AlertDialog.Builder(host).setTitle("帖子推广说明").setView(ScrollView(host).apply { addView(text) })
            .setPositiveButton("我知道了",null).create().apply { setOnDismissListener { if (operation == generation) { dialog = null; release() } }; show() }
        job = owner.lifecycleScope.launch { configuration.refresh(); if (operation == generation && visible()) update() }
    }
    private fun cancel() { generation++; job?.cancel(); job = null; dialog?.dismiss(); dialog = null; release() }
    override fun onPause(owner: LifecycleOwner) { cancel() }
    override fun onDestroy(owner: LifecycleOwner) { cancel() }
}
