package com.rescue.flutter_720yun.user.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityAdoptAboutBinding
import com.rescue.flutter_720yun.message.adapter.MessageListItemClickListener
import com.rescue.flutter_720yun.user.adapter.UserSettingAdapter
import com.rescue.flutter_720yun.user.viewmodels.AdoptAboutViewModel
import com.rescue.flutter_720yun.util.BuildConfig

class AdoptAboutActivity : BaseActivity(), MessageListItemClickListener {

    private var _binding: ActivityAdoptAboutBinding? = null
    private val binding get() = _binding!!
    private val viewModel by lazy {
        ViewModelProvider(this)[AdoptAboutViewModel::class.java]
    }
    private lateinit var adapter: UserSettingAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_adopt_about)
        setupToolbar(resources.getString(R.string.drawer_adopt))
        _binding = ActivityAdoptAboutBinding.bind(baseBinding.contentFrame.getChildAt(2))
        addViewAction()
        addViewModelObserver()
    }

    override fun addViewAction() {
        super.addViewAction()

    }

    override fun addViewModelObserver() {
        super.addViewModelObserver()
        viewModel.messageList.observe(this) {
            adapter = UserSettingAdapter(it)
            adapter.setClickListener(this)
            binding.recyclerview.layoutManager = LinearLayoutManager(this)
            binding.recyclerview.adapter = adapter
        }
    }

    override fun itemClick(position: Int) {
        when (position) {
            0 -> {
                val intent = Intent(this, UserAccountSafeActivity::class.java)
                intent.putExtra("localUrl","file:///android_asset/prevention.html")
                startActivity(intent)
            }
            1 -> {
                val intent = Intent(this, UserAccountSafeActivity::class.java)
                intent.putExtra("localUrl","file:///android_asset/lyinstruction.html")
                startActivity(intent)
            }
            else -> {

            }
        }
    }


}