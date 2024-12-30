package com.rescue.flutter_720yun.message.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.CommentListItemBinding
import com.rescue.flutter_720yun.message.models.MessageSingleListModel
import com.rescue.flutter_720yun.show.models.CommentListModel
import com.rescue.flutter_720yun.util.formatTime
import com.rescue.flutter_720yun.util.timeToStr
import com.rescue.flutter_720yun.util.toImgUrl

class CommentListAdapter(val list: MutableList<CommentListModel>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = CommentListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentListViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list[position]
        when (holder) {
            is CommentListViewHolder -> {
                holder.bind(item)
            }
        }
    }


    fun refreshItem(items: List<CommentListModel>) {
        list.clear()
        list.addAll(items)
        notifyDataSetChanged()
    }

    fun addItems(newList: List<CommentListModel>) {
        val startPosition = list.size
        list.addAll(newList)
        notifyItemRangeInserted(startPosition, newList.size)
    }

    fun uploadItem(item: CommentListModel) {
        val position = list.indexOfFirst { it.comment_id == item.comment_id }
        list[position] = item
        notifyItemChanged(position)
    }
}

class CommentListViewHolder(var binding: CommentListItemBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(item: CommentListModel) {
        item.userInfo?.avator?.let {
            Glide.with(BaseApplication.context).load(it.toImgUrl())
                .placeholder(ContextCompat.getDrawable(BaseApplication.context, R.drawable.icon_eee))
                .into(binding.headImg)

            item.userInfo?.username?.let { userName ->
                binding.nickName.text = userName
            }

            binding.timeText.text = item.create_time?.formatTime()
            binding.content.text = item.content

        }
    }
}