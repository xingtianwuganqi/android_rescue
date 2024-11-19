package com.rescue.flutter_720yun.home.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.rescue.flutter_720yun.BaseApplication
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
    private var cityName: String? = null


    private val viewModel by lazy {
        ViewModelProvider(this)[LocalListViewModel::class.java]
    }

    private val localSelectLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val resultData = data?.getStringExtra("local_city")
            uploadLocalCityData(resultData)
            Log.d("TAG", "change city is $resultData")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocalListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 避免重复添加
        if (!::homeFragment.isInitialized) {
            homeFragment = HomeFragment.newInstance("2")
        }
        if (childFragmentManager.findFragmentById(R.id.local_bottom) == null) {
            childFragmentManager.beginTransaction()
                .add(R.id.local_bottom, homeFragment)
                .commit()
        }

        cityName = viewModel.cityName.value

        binding.localTop.setOnClickListener {
            val citySelector = Intent(activity,CitySelectActivity::class.java)
            localSelectLauncher.launch(citySelector)
        }

        viewModel.cityName.observe(viewLifecycleOwner) {
            it?.let {
                if (it != cityName) {
                    homeFragment.beginLoadLocalCity(it)
                }
            }
            val str = BaseApplication.context.resources.getString(R.string.local_name, it)
            binding.localTitle.text = str
        }
        viewModel.loadCityInfo()
        Log.d("TAG","LocalList Fragment onCreateView networking")
    }


    private fun uploadLocalCityData(city: String?) {

        city?.let {
            viewModel.uploadCitySharePreInfo(it)
            homeFragment.beginLoadLocalCity(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}