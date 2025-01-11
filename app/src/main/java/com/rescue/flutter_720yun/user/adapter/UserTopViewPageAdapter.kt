package com.rescue.flutter_720yun.user.adapter

import androidx.fragment.app.Fragment
import com.rescue.flutter_720yun.user.fragment.UserShowFragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.rescue.flutter_720yun.user.fragment.UserTopicFragment

class UserTopViewPageAdapter(fragment: Fragment, private var userId: Int): FragmentStateAdapter(fragment) {



    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return UserTopicFragment.newInstance(userId, position)
    }
}