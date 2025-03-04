package com.rescue.flutter_720yun

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.home.activity.SearchActivity
import com.rescue.flutter_720yun.home.adapter.DrawerListAdapter
import com.rescue.flutter_720yun.databinding.ActivityMainBinding
import com.rescue.flutter_720yun.home.adapter.DrawerListClickListener
import com.rescue.flutter_720yun.home.models.DrawerListModel
import com.rescue.flutter_720yun.util.UserManager
import com.rescue.flutter_720yun.home.viewmodels.MainViewModel
import com.rescue.flutter_720yun.user.activity.AdoptAboutActivity
import com.rescue.flutter_720yun.user.activity.BlackListActivity
import com.rescue.flutter_720yun.user.activity.UserAccountSafeActivity
import com.rescue.flutter_720yun.user.activity.UserCollectionActivity
import com.rescue.flutter_720yun.user.activity.UserSettingActivity
import com.rescue.flutter_720yun.user.activity.WebPageActivity
import com.rescue.flutter_720yun.user.adapter.UserCollectionAdapter
import com.rescue.flutter_720yun.util.BuildConfig


class MainActivity : AppCompatActivity(), DrawerListClickListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout

    private val viewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ActivityController.addActivity(this)

        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        drawerLayout = binding.drawerLayout

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // 保存状态，防止 Fragment 被销毁

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_show, R.id.navigation_message, R.id.navigation_user
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)



        navController.addOnDestinationChangedListener { _, destination, _ ->
            val searchBtn = binding.searchButton
            when (destination.id) {
                R.id.navigation_home -> {
                    supportActionBar?.title = "首页"
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    supportActionBar?.setHomeAsUpIndicator(R.drawable.icon_draw_more)
                    searchBtn.visibility = View.VISIBLE
                    binding.searchButton.setImageDrawable(
                        ContextCompat.getDrawable(BaseApplication.context, R.drawable.icon_draw_search)
                    )
                }

                R.id.navigation_show -> {
                    supportActionBar?.title = "秀宠"
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                    searchBtn.visibility = View.GONE
                }

                R.id.navigation_message -> {
                    supportActionBar?.title = "消息"
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                    searchBtn.visibility = View.GONE
                }

                R.id.navigation_user -> {
                    supportActionBar?.title = "我的"
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                    searchBtn.visibility = View.VISIBLE
                    binding.searchButton.setImageDrawable(
                        ContextCompat.getDrawable(BaseApplication.context, R.drawable.icon_draw_setting)
                    )
                }


                else -> {
                    supportActionBar?.title = ""
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                    searchBtn.visibility = View.GONE
                }
            }
        }
        // 获取个人信息
        UserManager.getUserInfo()

        // 搜索添加点击
        binding.searchButton.setOnClickListener{
            if (supportActionBar?.title == "首页") {
                val intent = Intent(this, SearchActivity::class.java)
                startActivity(intent)
            }else if (supportActionBar?.title == "我的"){
                val intent = Intent(this, UserSettingActivity::class.java)
                startActivity(intent)
            }
        }

        addViewModelObserver()
    }

    private fun addViewModelObserver() {
        viewModel.drawerList.observe(this) {
            val adapter = DrawerListAdapter(it)
            adapter.setListener(this)
            binding.drawerRecyclerview.layoutManager = LinearLayoutManager(this)
            binding.drawerRecyclerview.adapter = adapter
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        when (item.itemId) {
            android.R.id.home -> drawerLayout.openDrawer(GravityCompat.START)
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityController.removeActivity(this)
    }

    override fun clickHeader() {

    }

    override fun clickItem(item: DrawerListModel) {
        if (item.name == resources.getString(R.string.drawer_footer)) {

        }else if (item.name == resources.getString(R.string.drawer_collect)) {
            val intent = Intent(this, UserCollectionActivity::class.java)
            startActivity(intent)
        }else if (item.name == resources.getString(R.string.drawer_black)) {
            val intent = Intent(this, BlackListActivity::class.java)
            startActivity(intent)
        }else if (item.name == resources.getString(R.string.drawer_rescue)) {
            val intent = Intent(this, AdoptAboutActivity::class.java)
            startActivity(intent)
        }else if (item.name == resources.getString(R.string.drawer_upload)) {

        }else if (item.name == resources.getString(R.string.drawer_agreement)) {
            val intent = Intent(this, WebPageActivity::class.java)
            val baseUrl = BuildConfig.BASEURL + BuildConfig.USERAGREEN_URL
            intent.putExtra("webUrl",baseUrl)
            startActivity(intent)
        }else if (item.name == resources.getString(R.string.drawer_privacy)) {
            val intent = Intent(this, WebPageActivity::class.java)
            val baseUrl = BuildConfig.BASEURL + BuildConfig.PRAVICY_URL
            Log.d("TAG", baseUrl)
            intent.putExtra("webUrl",baseUrl)
            startActivity(intent)
        }else if (item.name == resources.getString(R.string.drawer_about)) {
            val intent = Intent(this, WebPageActivity::class.java)
            val baseUrl = BuildConfig.BASEURL + BuildConfig.ABOUTUS
            Log.d("TAG", baseUrl)
            intent.putExtra("webUrl",baseUrl)
            startActivity(intent)
        }else if (item.name == resources.getString(R.string.drawer_help)) {

        }else if (item.name == resources.getString(R.string.drawer_recommend)) {

        }
    }


}


