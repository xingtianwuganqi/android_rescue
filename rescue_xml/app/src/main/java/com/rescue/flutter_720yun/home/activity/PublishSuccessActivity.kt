package com.rescue.flutter_720yun.home.activity

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.adoption.ui.*
import com.rescue.flutter_720yun.promotion.PromotionEntry

class PublishSuccessActivity: BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupToolbar("发布成功")
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(32,32,32,32) }
        baseBinding.contentFrame.addView(root)
        root.label("发布成功")
        val topic=intent.getIntExtra("topic_id",0)
        val promote=root.button("观看视频，增加曝光") { }
        if(topic>0) {
            PromotionEntry(this,promote,topic,"publish_success").setOwner(true)
            root.button("查看帖子") { startActivity(Intent(this,HomeDetailActivity::class.java).putExtra("topic_id",topic)) }
        } else promote.visibility=android.view.View.GONE
        root.button("完成") { finish() }
    }
}
