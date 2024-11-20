package com.rescue.flutter_720yun.home.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.rescue.flutter_720yun.home.adapter.ViewPagerAdapter
import com.rescue.flutter_720yun.databinding.FragmentHomeLayoutBinding

class HomeLayoutFragment : Fragment() {
    private var _binding: FragmentHomeLayoutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeLayoutBinding.inflate(inflater, container, false)


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val root: View = binding.root

        val tabLayout = binding.tabLayout
        val viewPager = binding.viewPager
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(
            tabLayout,
            viewPager
        ) { tab, position ->
            when(position) {
                0 -> tab.text = "推荐"
                1 -> tab.text = "同城"
                2 -> tab.text = "找宠"
            }
        }.attach()
        viewPager.offscreenPageLimit = 3
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}