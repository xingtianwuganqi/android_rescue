package com.rescue.flutter_720yun.message.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.ActivityChooserView.InnerLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.makeramen.roundedimageview.RoundedImageView
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.ActivityMessageSingleBinding
import com.rescue.flutter_720yun.databinding.MessageSingleItemBinding
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.message.models.MessageSingleListModel
import com.rescue.flutter_720yun.util.toImgUrl

class MessageSingleItemAdapter(var list: MutableList<MessageSingleListModel>): RecyclerView.Adapter<MessageSingleItemAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: MessageSingleItemBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MessageSingleListModel) {
            item.from_info?.avator?.let {
                Glide.with(BaseApplication.context).load(it.toImgUrl())
                    .placeholder(ContextCompat.getDrawable(BaseApplication.context, R.drawable.icon_eee))
                    .into(binding.roundImage)

            }
            binding.nickName.text = item.from_info?.username
            binding.timeText.text = item.create_time
            item.topicInfo?.imgs?.first()?.let {
                Glide.with(binding.root).load(it.toImgUrl())
                    .placeholder(ContextCompat.getDrawable(BaseApplication.context, R.drawable.icon_eee))
                    .into(binding.topicImage)
            }

            val msg_type = item.msg_type
            if (msg_type == 1 || msg_type == 5 || msg_type == 10) {
                binding.desc.text = "赞了这条帖子"
            }else if (msg_type == 2 || msg_type == 6 || msg_type == 11) {
                binding.desc.text = "收藏了这条帖子"
            }else if (msg_type == 3 || msg_type == 4 || msg_type == 7 || msg_type == 8 || msg_type == 12 || msg_type == 13)  {
                if (item.reply_type == 1) {
                    binding.desc.text = "评论说：${item.commentInfo?.content ?: ""}"

                }else{
                    binding.desc.text = "回复说：${item.replyInfo?.content ?: ""}"
                }
            }
            binding.content.text = item.topicInfo?.content
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = MessageSingleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.bind(item)
    }

    fun refreshItem(items: List<MessageSingleListModel>) {
        list.clear()
        list.addAll(items)
        notifyDataSetChanged()
    }

    fun addItems(newList: List<MessageSingleListModel>) {
        val startPosition = list.size
        list.addAll(newList)
        notifyItemRangeInserted(startPosition, newList.size)
    }

    fun uploadItem(item: MessageSingleListModel) {
        val position = list.indexOfFirst { it.msg_id == item.msg_id }
        list[position] = item
        notifyItemChanged(position)
    }
}