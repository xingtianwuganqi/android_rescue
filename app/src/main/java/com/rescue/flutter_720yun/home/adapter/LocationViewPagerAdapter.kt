package com.rescue.flutter_720yun.home.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.fragment.LocationListFragment
import com.rescue.flutter_720yun.home.models.AddressItem

class LocationViewPagerAdapter(fragment: Fragment): FragmentStateAdapter(fragment) {

    val fragments = mutableListOf<LocationListFragment>()

    fun setFragments(newFragments: List<LocationListFragment>) {
        fragments.clear()
        fragments.addAll(newFragments)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}


class LocationListAdapter(var list: MutableList<AddressItem>): RecyclerView.Adapter<LocationListAdapter.ViewHolder>() {
    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val addressTitle: TextView = view.findViewById(R.id.address_title)
    }

    private var onItemClick: ((AddressItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.address_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.addressTitle.text = item.name
        holder.addressTitle.setOnClickListener {
            onItemClick?.let { it1 -> it1(item) }
        }
    }

    fun setItems(items: List<AddressItem>, onItemClick: (AddressItem) -> Unit) {
        this.onItemClick = onItemClick
        list.clear()
        list.addAll(items)
        notifyDataSetChanged()
    }
}