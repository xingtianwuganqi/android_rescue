package com.rescue.flutter_720yun.user.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.BlackListItemBinding
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.user.models.BlackListModel

class BlackListAdapter(var list: MutableList<BlackListModel>): RecyclerView.Adapter<BlackListAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: BlackListItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BlackListModel) {
            binding.phoneText.text = item.contact
            binding.desc.text = item.desc
            binding.roleText.text = if (item.black_type == 1) "领养人" else "送养人"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = BlackListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.bind(item)
    }

    fun refreshItem(items: List<BlackListModel>) {
        list.clear()
        list.addAll(items)
        notifyDataSetChanged()
    }

    fun addItems(newList: List<BlackListModel>) {
        val startPosition = list.size
        list.addAll(newList)
        notifyItemRangeInserted(startPosition, newList.size)
    }
}