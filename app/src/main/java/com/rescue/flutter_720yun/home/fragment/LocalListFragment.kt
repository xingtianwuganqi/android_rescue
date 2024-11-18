package com.rescue.flutter_720yun.home.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.FragmentLocalListBinding
import com.rescue.flutter_720yun.home.activity.CitySelectActivity
import com.rescue.flutter_720yun.home.adapter.CitySelectAdapter
import com.rescue.flutter_720yun.home.models.AddressItem
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.home.viewmodels.LocalListViewModel

class LocalListFragment : Fragment() {

    private var _binding: FragmentLocalListBinding? = null
    private val binding get() = _binding!!
    private lateinit var homeFragment: HomeFragment

    private val viewModel by lazy {
        ViewModelProvider(this)[LocalListViewModel::class.java]
    }

    private val localSelectLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val resultData = data?.getParcelableExtra<AddressItem>("local_city")
            uploadLocalCityData(resultData)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocalListBinding.inflate(inflater, container, false)

        // 避免重复添加
        if (!::homeFragment.isInitialized) {
            homeFragment = HomeFragment()
        }
        if (childFragmentManager.findFragmentById(R.id.local_bottom) == null) {
            childFragmentManager.beginTransaction()
                .add(R.id.local_bottom, homeFragment)
                .commit()
        }

        binding.localTop.setOnClickListener {
            val citySelector = Intent(activity,CitySelectActivity::class.java)
            localSelectLauncher.launch(citySelector)
        }

        viewModel.cityName.observe(viewLifecycleOwner) {
            binding.localTitle.text = "当前位置：$it"
        }

        return binding.root
    }


    private fun uploadLocalCityData(model: AddressItem?) {

        model?.name?.let {
            viewModel.uploadCitySharePreInfo(it)
            homeFragment.beginLoadLocalCity(it)
        }
    }
}