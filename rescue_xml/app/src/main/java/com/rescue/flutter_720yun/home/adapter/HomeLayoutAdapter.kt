package com.rescue.flutter_720yun.home.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.rescue.flutter_720yun.home.fragment.FindPetFragment
import com.rescue.flutter_720yun.home.fragment.HomeFragment
import com.rescue.flutter_720yun.home.fragment.LocalListFragment

class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    private val fragments = listOf(HomeFragment(), LocalListFragment()) //, FindPetFragment()

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}