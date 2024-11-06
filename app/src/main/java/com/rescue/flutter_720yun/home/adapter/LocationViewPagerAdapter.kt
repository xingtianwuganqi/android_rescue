package com.rescue.flutter_720yun.home.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.rescue.flutter_720yun.home.fragment.HomeFragment
import com.rescue.flutter_720yun.home.fragment.LocationListFragment

class LocationViewPagerAdapter(fragment: Fragment): FragmentStateAdapter(fragment) {

    private val fragments = listOf(LocationListFragment(), LocationListFragment(), LocationListFragment())

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}