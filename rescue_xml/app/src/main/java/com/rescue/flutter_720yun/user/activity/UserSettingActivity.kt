package com.rescue.flutter_720yun.user.activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.ActivityController
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityUserSettingBinding
import com.rescue.flutter_720yun.home.models.LoginEvent
import com.rescue.flutter_720yun.message.adapter.MessageListAdapter
import com.rescue.flutter_720yun.message.adapter.MessageListItemClickListener
import com.rescue.flutter_720yun.message.adapter.MessageSystemItemAdapter
import com.rescue.flutter_720yun.user.adapter.UserSettingAdapter
import com.rescue.flutter_720yun.user.viewmodels.UserSettingViewModel
import com.rescue.flutter_720yun.util.LoadingDialog
import com.rescue.flutter_720yun.util.UserManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

class UserSettingActivity : BaseActivity(), MessageListItemClickListener {


    private var _binding: ActivityUserSettingBinding? = null
    private val binding get() = _binding!!

    private val viewModel by lazy {
        ViewModelProvider(this)[UserSettingViewModel::class.java]
    }

    private var pendingCategory: String?=null
    private val login=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val category=pendingCategory;pendingCategory=null
        if(UserManager.isLogin && category!=null) openEntry(category)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingCategory=savedInstanceState?.getString("pending_settings_category")
        setContentLayout(R.layout.activity_user_setting)
        setupToolbar(resources.getString(R.string.user_setting))
        _binding = ActivityUserSettingBinding.bind(baseBinding.contentFrame.getChildAt(2))

        binding.settingsVersion.text=com.rescue.flutter_720yun.BuildConfig.VERSION_NAME
        addViewModelObserver()
        UserManager.sessionRevision.observe(this) { viewModel.refreshAccountStatus() }
    }



    override fun addViewModelObserver() {
        super.addViewModelObserver()
        viewModel.messageList.observe(this) {
            val adapter = UserSettingAdapter(it,settingsStyle=true)
            binding.recyclerview.adapter = adapter
            binding.recyclerview.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            adapter.setClickListener(this)
        }
    }


    override fun itemClick(position: Int) {
        openEntry(viewModel.messageList.value?.getOrNull(position)?.category)
    }
    private fun openEntry(category: String?) {
        if(category in listOf("adoption_profile","find_pet_profile","password","feedback") && !UserManager.isLogin) {
            pendingCategory=category
            com.rescue.flutter_720yun.analytics.AnalyticsManager.loginClick()
            login.launch(Intent(this,com.rescue.flutter_720yun.home.activity.LoginActivity::class.java));return
        }
        when (category) {
            "bind_email" -> startActivity(Intent(this,com.rescue.flutter_720yun.home.activity.LoginActivity::class.java).putExtra("type","bindPhone"))
            "blacklist" -> startActivity(Intent(this,BlackListActivity::class.java))
            "adoption_profile" -> {
                startActivity(Intent(this, com.rescue.flutter_720yun.adoption.activity.AdoptionProfileActivity::class.java))
            }
            "find_pet_profile" -> startActivity(Intent(this, com.rescue.flutter_720yun.home.activity.FindPetDetailActivity::class.java))
            "share_video_music" -> startActivity(Intent(this, com.rescue.flutter_720yun.home.share.ShareVideoMusicActivity::class.java))
            "password" -> {
                val intent = Intent(this, UserChangePasswordActivity::class.java)
                startActivity(intent)
            }

            "feedback" -> {
                val intent = Intent(this, UserFeedbackActivity::class.java)
                startActivity(intent)
            }

            "account_security" -> {
                val intent = Intent(this, UserAccountSafeActivity::class.java)
                intent.putExtra("localUrl","file:///android_asset/account_security.html")
                startActivity(intent)
            }
            "logout" -> {
                logoutDialog()
            }
        }
    }

    private fun logoutDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(resources.getString(R.string.logout_remind))
        builder.setCancelable(false)
        builder.setPositiveButton(resources.getString(R.string.confirm_d), DialogInterface.OnClickListener { dialogInterface, i ->
            com.rescue.flutter_720yun.analytics.AnalyticsManager.track("logout",interactive=true)
            logout()
        })
        builder.setNegativeButton(resources.getString(R.string.cancel), DialogInterface.OnClickListener { dialogInterface, i ->

        })
        val dialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.color_system))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.color_node))
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).textSize = 16F
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).textSize = 16F
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun logout() {
        LoadingDialog.show(this)
        UserManager.logout()
        EventBus.getDefault().post(LoginEvent(null))
        lifecycleScope.launch {
            LoadingDialog.hide()
            ActivityController.finishToLast()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) { outState.putString("pending_settings_category",pendingCategory);super.onSaveInstanceState(outState) }

    override fun onResume() { super.onResume();viewModel.refreshAccountStatus() }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}
