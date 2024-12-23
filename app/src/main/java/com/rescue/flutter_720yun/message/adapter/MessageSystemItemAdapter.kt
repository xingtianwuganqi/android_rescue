package com.rescue.flutter_720yun.message.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.MessageSystemItemBinding
import com.rescue.flutter_720yun.message.models.MessageSingleListModel
import com.rescue.flutter_720yun.message.models.MessageSystemListModel
import com.rescue.flutter_720yun.util.formatTime

class MessageSystemItemAdapter(val list: MutableList<MessageSystemListModel>): RecyclerView.Adapter<MessageSystemItemAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: MessageSystemItemBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MessageSystemListModel) {
            binding.titleText.text = ContextCompat.getString(BaseApplication.context, R.string.message_title)
            binding.timeText.text = item.create_time?.formatTime()
            binding.content.text = item.content
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = MessageSystemItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.bind(item)
    }

    fun refreshItem(items: List<MessageSystemListModel>) {
        list.clear()
        list.addAll(items)
        notifyDataSetChanged()
    }

    fun addItems(newList: List<MessageSystemListModel>) {
        val startPosition = list.size
        list.addAll(newList)
        notifyItemRangeInserted(startPosition, newList.size)
    }

    fun uploadItem(item: MessageSystemListModel) {
        val position = list.indexOfFirst { it.id == item.id }
        list[position] = item
        notifyItemChanged(position)
    }
}