package com.rescue.flutter_720yun.home.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityCitySelectBinding
import com.rescue.flutter_720yun.home.adapter.CitySelectAdapter
import com.rescue.flutter_720yun.home.models.AddressItem
import com.rescue.flutter_720yun.home.models.CityModel
import com.rescue.flutter_720yun.home.viewmodels.CitySelectViewModel
import com.rescue.flutter_720yun.util.UiState

class CitySelectActivity : BaseActivity() {

    private var _binding: ActivityCitySelectBinding? = null
    private val binding get() = _binding!!
    private val viewModel by lazy {
        ViewModelProvider(this)[CitySelectViewModel::class.java]
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_city_select)
        setupToolbar("请选择城市")
        _binding = ActivityCitySelectBinding.bind(baseBinding.contentFrame.getChildAt(2))

        addViewModelObserver()
        addBackListener()
    }

    override fun addViewModelObserver() {
        viewModel.cityModels.observe(this) {
            when (it) {
                is UiState.FirstLoading -> {
                    showLoading()
                }

                is UiState.Success -> {
                    loadCityAdapter(it.data)
                }

                is UiState.Error -> {

                }
            }
        }
    }

    private fun loadCityAdapter(cities: List<AddressItem>) {
        binding.cityRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.cityRecyclerview.adapter = CitySelectAdapter(cities)
    }


    private fun addBackListener() {
        // 注册返回事件的回调
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                sendResultAndFinish()
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun finishAction() {
        super.finishAction()
        sendResultAndFinish()
    }

    private fun sendResultAndFinish() {
        viewModel.cityName.value.let {
            val intent = Intent()
            intent.putExtra("local_city", it)
            setResult(Activity.RESULT_OK, intent)
        }
    }
}