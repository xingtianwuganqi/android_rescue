package com.rescue.flutter_720yun.user.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityUserSettingBinding

class UserSettingActivity : BaseActivity() {


    private var _binding: ActivityUserSettingBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_user_setting)
        setupToolbar(resources.getString(R.string.user_setting))
        _binding = ActivityUserSettingBinding.bind(baseBinding.contentFrame.getChildAt(2))


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