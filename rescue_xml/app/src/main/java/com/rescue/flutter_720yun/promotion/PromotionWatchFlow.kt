package com.rescue.flutter_720yun.promotion

import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.rescue.flutter_720yun.BuildConfig
import com.rescue.flutter_720yun.R
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
        if (!RewardedTopicPromotionCoordinator.acquirePreparation(token)) { host.notice(host.getString(R.string.promotion_finish_current)); return false }
        reservation = token; return true
    }
    private fun release() { reservation?.let { RewardedTopicPromotionCoordinator.releasePreparation(it) }; reservation = null }
    fun start(topic: Int, scene: String) {
        if (!BuildConfig.ADS_ENABLED || !visible() || topic <= 0) return
        if (!UserManager.isLogin) { host.notice(host.getString(R.string.common_login_required)); return }
        com.rescue.flutter_720yun.analytics.AnalyticsManager.rewardClick(topic,scene)
        if (RewardedTopicPromotionCoordinator.hasPendingReward(topic)) {
            host.notice(host.getString(R.string.promotion_last_submitting))
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
                dialog = AlertDialog.Builder(host).setTitle(host.getString(R.string.promotion_exposure))
                    .setMessage(host.getString(R.string.promotion_confirm_watch,configuration.duration))
                    .setNegativeButton(host.getString(R.string.cancel),null).setPositiveButton(host.getString(R.string.promotion_watch_video)) { _,_ ->
                        release()
                        if (operation == generation && revision == UserManager.sessionRevision.value && visible())
                            RewardedTopicPromotionCoordinator.start(host,topic,scene,owner)
                    }.create().apply {
                        setOnDismissListener { if (operation == generation) { dialog = null; release() } }
                        show()
                    }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (operation == generation && visible()) host.notice((e as? com.rescue.flutter_720yun.adoption.repository.AdoptionError)?.message ?: host.getString(R.string.promotion_qualification_failed)) }
            finally { if (operation == generation && dialog == null) release() }
        }
    }
    fun showIntroduction() {
        if (!BuildConfig.ADS_ENABLED || !visible() || !reserve()) return
        val operation = ++generation
        val text = TextView(host).apply { textSize = 15f; setTextColor(android.graphics.Color.DKGRAY); setPadding(32,24,32,24) }
        fun update() {
            val duration = configuration.duration
            text.text = host.getString(R.string.promotion_help_format,duration,duration,duration,duration)
        }
        update()
        val maxHeight = (host.resources.displayMetrics.heightPixels * 0.55f).toInt()
        val scroll = com.rescue.flutter_720yun.views.MaxHeightScrollView(host).apply {
            this.maxHeight = maxHeight
            addView(text)
        }
        dialog = AlertDialog.Builder(host).setTitle(host.getString(R.string.promotion_help_title)).setView(scroll)
            .setPositiveButton(host.getString(R.string.promotion_i_know),null).create().apply {
                setOnDismissListener { if (operation == generation) { dialog = null; release() } }
                show()
            }
        job = owner.lifecycleScope.launch { configuration.refresh(); if (operation == generation && visible()) update() }
    }
    private fun cancel() { generation++; job?.cancel(); job = null; dialog?.dismiss(); dialog = null; release() }
    override fun onPause(owner: LifecycleOwner) { cancel() }
    override fun onDestroy(owner: LifecycleOwner) { cancel() }
}
