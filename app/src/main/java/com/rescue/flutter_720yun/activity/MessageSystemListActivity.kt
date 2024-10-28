package com.rescue.flutter_720yun.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityMessageSystemListBinding
import com.rescue.flutter_720yun.models.MessageSystemListViewModel

class MessageSystemListActivity : BaseActivity() {

    private var _binding: ActivityMessageSystemListBinding? = null
    private val binding get() = _binding!!

    private val viewModel by lazy {
        ViewModelProvider(this)[MessageSystemListViewModel::class.java]
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_message_system_list)
        setupToolbar(BaseApplication.context.resources.getString(R.string.message_system))
        _binding = ActivityMessageSystemListBinding.bind(baseBinding.contentFrame.getChildAt(2))


    }
}