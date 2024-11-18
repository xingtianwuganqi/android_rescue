package com.rescue.flutter_720yun.home.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.FragmentLocationSheetBinding
import android.graphics.Point
import android.util.Log
import android.view.MotionEvent
import androidx.core.view.get
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.home.adapter.LocationViewPagerAdapter
import com.rescue.flutter_720yun.home.models.AddressItem
import com.rescue.flutter_720yun.util.ParameterizedTypeImpl
import java.io.IOException
import java.io.InputStream

class LocationSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentLocationSheetBinding? = null
    private val binding get() = _binding!!
    private var selectedItems = mutableListOf<AddressItem>()

    private lateinit var addressData: List<AddressItem>
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: LocationViewPagerAdapter
    var selectCompletion: ((MutableList<AddressItem>) -> Unit)? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationSheetBinding.inflate(inflater, container, false)

        tabLayout = binding.tabLayout
        viewPager = binding.viewPager
        viewPager.isUserInputEnabled = false
        val data = loadProvinceData()
        addressData = data ?: emptyList()
        setupTabsAndViewPager()

        return binding.root
    }

    private fun setupTabsAndViewPager() {

        pagerAdapter = LocationViewPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        repeat(3) {
            binding.tabLayout.addTab(tabLayout.newTab()
                .setText(if (it == 0) "请选择" else ""))
        }

        updatePage(0, addressData)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabSelected(p0: TabLayout.Tab?) {
                if (tabLayout.getTabAt(p0?.position!!)?.text != ""){
                    viewPager.currentItem = p0?.position!!
                }
            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {

            }

            override fun onTabReselected(p0: TabLayout.Tab?) {

            }
        })


    }

    private fun updatePage(level: Int, items: List<AddressItem>) {
        val fragment = pagerAdapter.fragments[level].apply {
            setAddressItem(items) {
                if (selectedItems.size > level) {
                    selectedItems[level] = it
                }else{
                    selectedItems.add(it)
                }

                tabLayout.getTabAt(level)?.text = it.name

                if (it.children == null) {
                    sendResultAndDismiss()
                }else{
                    updatePage(level + 1, it.children ?: emptyList())
                    viewPager.currentItem = level + 1
                    tabLayout.getTabAt(level + 1)?.text = "请选择"
                }
            }
        }

        tabLayout.getTabAt(level + 1)?.select()

        if (level == 0) {
            tabLayout.getTabAt(1)?.text = ""
            tabLayout.getTabAt(2)?.text = ""
        }else if (level == 1) {
            tabLayout.getTabAt(2)?.text = ""
        }

    }

    private fun sendResultAndDismiss() {
        selectCompletion?.let { completion ->
            completion(selectedItems)
        }
        dismiss()
    }
    private fun loadProvinceData(): List<AddressItem>? {
        val data = readJsonFromRaw(BaseApplication.context, R.raw.location)
        return data?.let {
            converStringToList(it)
        }
    }

    private fun converStringToList(jsonString: String): List<AddressItem>? {
        // 初始化Gson实例
        val gson = Gson()

        // 创建一个具体类型的TypeToken
        val listType = ParameterizedTypeImpl(List::class.java, arrayOf(AddressItem::class.java))

        // 将jsonString解析成List<T>
        return try {
            gson.fromJson(jsonString, listType)
        } catch (e: Exception) {
            e.printStackTrace()
            null // 转换失败返回null
        }
    }

    private fun readJsonFromRaw(context: Context, resId: Int): String? {
        return try {
            val inputStream: InputStream = context.resources.openRawResource(resId)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}