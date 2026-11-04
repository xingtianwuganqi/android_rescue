package com.rescue.flutter_720yun.promotion

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.rescue.flutter_720yun.ads.TakuAds
import com.rescue.flutter_720yun.adoption.ui.notice
import com.rescue.flutter_720yun.home.activity.LoginActivity
import com.rescue.flutter_720yun.util.UserManager

/** Rewards can arrive while the ad covers the page; present feedback after dismissal/resume. */
class PromotionFeedback(private val host: FragmentActivity, private val owner: LifecycleOwner,
    private val accepts: (RewardedTopicPromotionCoordinator.Update) -> Boolean) : DefaultLifecycleObserver {
    init {
        owner.lifecycle.addObserver(this)
        RewardedTopicPromotionCoordinator.updates.observe(owner) { deliver() }
        RewardedTopicPromotionCoordinator.loginRequired.observe(owner) { deliver() }
    }
    private fun deliver() {
        if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || TakuAds.fullscreenPresented) return
        val account = RewardedTopicPromotionCoordinator.loginRequired.value
        if (account != null && (!UserManager.isLogin || UserManager.userId == account)) {
            RewardedTopicPromotionCoordinator.loginRequired.value = null
            host.notice("登录已失效，观看奖励已保存，请重新登录原账号后继续提交")
            host.startActivity(Intent(host,LoginActivity::class.java)); return
        }
        val update = RewardedTopicPromotionCoordinator.updates.value ?: return
        if (update.account != UserManager.userId || update.message == null || !accepts(update)) return
        RewardedTopicPromotionCoordinator.updates.value = update.copy(message = null)
        host.notice(update.message)
    }
    override fun onResume(owner: LifecycleOwner) { deliver() }
}
