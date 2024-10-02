package com.rescue.flutter_720yun.activity

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityHomeDetailBinding

class HomeDetailActivity : BaseActivity() {

    private var _binding: ActivityHomeDetailBinding? = null
    val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityHomeDetailBinding.inflate(layoutInflater)
        // 将子 Activity 的布局内容加载到 content_frame 中
        val contentFrame: FrameLayout = findViewById(R.id.content_frame)
        layoutInflater.inflate(R.layout.activity_home_detail, contentFrame, true)
        setupToolbar("详情")
    }
}