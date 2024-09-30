package com.rescue.flutter_720yun.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.rescue.flutter_720yun.fragment.HomeLayoutFragment
import com.rescue.flutter_720yun.ui.home.HomeFragment

class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    private val fragments = listOf(HomeFragment(), HomeFragment())

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}