package com.rescue.flutter_720yun.user.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.rescue.flutter_720yun.home.fragment.HomeFragment
import com.rescue.flutter_720yun.show.fragment.ShowFragment

class UserCollectionAdapter(fragmentActivity: FragmentActivity): FragmentStateAdapter(fragmentActivity) {

    val list = listOf(HomeFragment.newInstance("3"), ShowFragment.newInstance(-1))

    override fun getItemCount(): Int {
        return list.size
    }

    override fun createFragment(position: Int): Fragment {
        return list[position]
    }

}