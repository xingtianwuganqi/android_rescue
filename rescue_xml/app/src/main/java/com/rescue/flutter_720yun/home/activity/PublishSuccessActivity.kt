package com.rescue.flutter_720yun.home.activity

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityPublishSuccessBinding

class PublishSuccessActivity: BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_publish_success)
        val binding = ActivityPublishSuccessBinding.bind(baseBinding.contentFrame.getChildAt(2))
        setupToolbar("")
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val topic = intent.getIntExtra("topic_id", 0)
        binding.successMessage.text = if (topic > 0) {
            getString(R.string.publish_success_with_post)
        } else {
            getString(R.string.publish_success_only)
        }
        binding.viewTopic.isVisible = topic > 0
        binding.viewTopic.setOnClickListener {
            if (topic > 0) {
                startActivity(Intent(this, HomeDetailActivity::class.java).putExtra("topic_id", topic))
            }
        }
        binding.done.setOnClickListener { finish() }
        binding.successAnimation.playAnimation()
    }
}
