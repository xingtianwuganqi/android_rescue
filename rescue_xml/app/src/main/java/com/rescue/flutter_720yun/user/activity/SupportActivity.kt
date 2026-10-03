package com.rescue.flutter_720yun.user.activity

import android.os.Bundle
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.ads.FullscreenAdController
import com.rescue.flutter_720yun.databinding.ActivitySupportBinding
import com.rescue.flutter_720yun.util.toastString

/** Mirrors iOS SupportViewController: user-initiated rewarded video, with a thank-you on reward. */
class SupportActivity : BaseActivity() {
    private lateinit var ads: FullscreenAdController
    private lateinit var binding: ActivitySupportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_support)
        binding = ActivitySupportBinding.bind(baseBinding.contentFrame.getChildAt(2))
        setupToolbar(getString(R.string.drawer_help))
        ads = FullscreenAdController(this, this,
            onEvent = { event ->
                when {
                    event == "rewarded loaded" -> binding.supportButton.isEnabled = true
                    event == "rewarded closed" || event.startsWith("rewarded play failed") -> ads.loadRewarded()
                    event.startsWith("rewarded load failed") -> getString(R.string.support_ad_unavailable).toastString()
                }
            },
            onReward = { getString(R.string.support_thanks).toastString() }
        )
        ads.loadRewarded()
        binding.supportButton.setOnClickListener {
            if (!ads.showRewarded()) {
                getString(R.string.support_ad_unavailable).toastString()
                ads.loadRewarded()
            }
        }
    }
}
