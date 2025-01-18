package com.rescue.flutter_720yun.user.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bumptech.glide.Glide
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityUserInfoEditBinding
import com.rescue.flutter_720yun.home.models.UserInfoModel
import com.rescue.flutter_720yun.user.viewmodels.UserInfoEditViewModel
import com.rescue.flutter_720yun.util.toImgUrl

class UserInfoEditActivity : BaseActivity() {

    private var _binding: ActivityUserInfoEditBinding? = null
    private val binding get() = _binding!!

    private val viewModel by lazy {
        ViewModelProvider(this)[UserInfoEditViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_user_info_edit)
        _binding = ActivityUserInfoEditBinding.bind(baseBinding.contentFrame.getChildAt(2))
        setupToolbar(resources.getString(R.string.user_edit))

        val userInfo = intent.getParcelableArrayListExtra<UserInfoModel>("userInfo")
        userInfo?.first()?.let {
            viewModel.uploadUserModel(it)
        }

        addViewAction()
        addViewModelObserver()
    }

    override fun addViewAction() {
        super.addViewAction()
        binding.headImg.setOnClickListener {
            
        }
    }

    override fun addViewModelObserver() {
        super.addViewModelObserver()
        viewModel.userInfo.observe(this) {
            Glide.with(this).load(it.avator?.toImgUrl())
                .placeholder(R.drawable.icon_eee)
                .into(binding.headImg)

            binding.username.setText(it.username)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}