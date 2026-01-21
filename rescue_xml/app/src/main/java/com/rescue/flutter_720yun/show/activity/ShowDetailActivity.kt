package com.rescue.flutter_720yun.show.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityShowDetailBinding
import com.rescue.flutter_720yun.home.fragment.HomeFragment
import com.rescue.flutter_720yun.show.fragment.ShowFragment

class ShowDetailActivity : BaseActivity() {

    private var _binding: ActivityShowDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var showFragment: ShowFragment


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_show_detail)
        setupToolbar(resources.getString(R.string.title_dashboard))

        val showId = intent.getIntExtra("show_id", 0)
        // 避免重复添加
        if (savedInstanceState == null) {
            showFragment = ShowFragment.newInstance(showId)
            supportFragmentManager.beginTransaction()
                .add(R.id.content_frame, showFragment)
                .commit()
        }
    }
}