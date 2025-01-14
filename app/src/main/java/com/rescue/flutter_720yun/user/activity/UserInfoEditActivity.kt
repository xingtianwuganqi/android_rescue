package com.rescue.flutter_720yun.user.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityUserInfoEditBinding

class UserInfoEditActivity : BaseActivity() {

    private var _binding: ActivityUserInfoEditBinding? = null
    private val binding get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_user_info_edit)
        _binding = ActivityUserInfoEditBinding.bind(baseBinding.contentFrame.getChildAt(2))
        setupToolbar(resources.getString(R.string.user_edit))
    }

    override fun addViewAction() {
        super.addViewAction()

    }

    override fun addViewModelObserver() {
        super.addViewModelObserver()

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}