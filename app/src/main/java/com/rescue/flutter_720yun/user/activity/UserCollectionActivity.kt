 package com.rescue.flutter_720yun.user.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.tabs.TabLayoutMediator
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityUserCollectionBinding
import com.rescue.flutter_720yun.home.adapter.ViewPagerAdapter
import com.rescue.flutter_720yun.user.adapter.UserCollectionAdapter

 class UserCollectionActivity : BaseActivity() {
     private var _binding: ActivityUserCollectionBinding? = null
     private val binding get() = _binding!!

     override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         setContentLayout(R.layout.activity_user_collection)
         _binding = ActivityUserCollectionBinding.bind(baseBinding.contentFrame.getChildAt(2))
         setupToolbar(resources.getString(R.string.user_collection))
         addViewAction()
         addViewModelObserver()
     }

     override fun addViewAction() {
         super.addViewAction()
         val tabLayout = binding.tabLayout
         val viewPager = binding.viewPager
         val adapter = UserCollectionAdapter(this)
         viewPager.adapter = adapter

         TabLayoutMediator(
             tabLayout,
             viewPager
         ) { tab, position ->
             when(position) {
                 0 -> tab.text = "领养"
                 1 -> tab.text = "秀宠"
             }
         }.attach()
         viewPager.offscreenPageLimit = 3
     }

     override fun addViewModelObserver() {
         super.addViewModelObserver()

     }

     override fun onDestroy() {
         super.onDestroy()
         _binding = null
     }
}