package com.rescue.flutter_720yun.show.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityShowReleaseBinding
import com.rescue.flutter_720yun.show.viewmodels.ShowReleaseViewModel

class ShowReleaseActivity : BaseActivity() {

    private var _binding: ActivityShowReleaseBinding? = null
    private val binding get() = _binding!!
    private val viewModel by lazy {
        ViewModelProvider(this)[ShowReleaseViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_show_release)
        setupToolbar(resources.getString(R.string.release_upload))
        _binding = ActivityShowReleaseBinding.bind(baseBinding.contentFrame.getChildAt(2))
        
    }
}