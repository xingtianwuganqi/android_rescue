package com.rescue.flutter_720yun.user.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityUserChangePasswordBinding
import com.rescue.flutter_720yun.user.viewmodels.UserChangePasswordViewModel

class UserChangePasswordActivity : BaseActivity() {

    private var _binding: ActivityUserChangePasswordBinding? = null
    private val binding get() = _binding!!

    private val viewmodel by lazy {
        ViewModelProvider(this)[UserChangePasswordViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_user_change_password)
        setupToolbar(resources.getString(R.string.user_change_password))
        _binding = ActivityUserChangePasswordBinding.bind(baseBinding.contentFrame.getChildAt(2))

    }


    override fun addViewAction() {
        super.addViewAction()

    }

    override fun addViewModelObserver() {
        super.addViewModelObserver()

    }

    override fun onDestroy() {
        super.onDestroy()

    }

}