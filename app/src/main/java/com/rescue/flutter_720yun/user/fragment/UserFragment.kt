package com.rescue.flutter_720yun.user.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.FragmentUserBinding
import com.rescue.flutter_720yun.user.activity.UserInfoEditActivity
import com.rescue.flutter_720yun.user.adapter.UserTopViewPageAdapter
import com.rescue.flutter_720yun.user.viewmodels.UserViewModel
import com.rescue.flutter_720yun.util.UserManager
import com.rescue.flutter_720yun.util.toImgUrl

class UserFragment : Fragment() {
    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!
    private val viewModel by lazy {
        ViewModelProvider(this)[UserViewModel::class.java]
    }

    private val editActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            viewModel.userId = it.getInt("userId")
        }
        if (UserManager.isLogin && viewModel.userId == null) {
            viewModel.userId = UserManager.userId
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = UserTopViewPageAdapter(this, viewModel.userId ?: 0)
        binding.viewPager.adapter = adapter

        if (UserManager.isLogin) {
            viewModel.userIdGetUserInfoNetworking()
        }else{
            binding.username.text = resources.getString(R.string.user_login)
            binding.headImg.setImageDrawable(ContextCompat.getDrawable(BaseApplication.context, R.drawable.icon_eee))
            binding.rightButton.visibility = View.GONE
        }

        binding.backLayout.setOnClickListener {
            val intent = Intent(activity, UserInfoEditActivity::class.java)
            editActivityLauncher.launch(intent)
        }

        viewModel.userInfo.observe(viewLifecycleOwner) {
            binding.username.text = it.username
            Glide.with(this)
                .load(it.avator?.toImgUrl())
                .placeholder(R.drawable.icon_eee).into(binding.headImg)
            binding.rightButton.visibility = View.VISIBLE
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when(position) {
                0 -> tab.text = "领养"
                1 -> tab.text = "秀宠"
            }
        }.attach()
    }

    companion object {
        @JvmStatic
        fun newInstance(userId: Int) =
            UserFragment().apply {
                arguments = Bundle().apply {
                    putInt("userId", userId)
                }
            }
    }
}