package com.rescue.flutter_720yun.message.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.CommentListItemBinding
import com.rescue.flutter_720yun.databinding.ReplyBottomItemBinding
import com.rescue.flutter_720yun.databinding.ReplyListItemBinding
import com.rescue.flutter_720yun.message.models.MessageSingleListModel
import com.rescue.flutter_720yun.show.models.CommentItemModel
import com.rescue.flutter_720yun.show.models.CommentListModel
import com.rescue.flutter_720yun.show.models.ReplyListModel
import com.rescue.flutter_720yun.util.formatTime
import com.rescue.flutter_720yun.util.timeToStr
import com.rescue.flutter_720yun.util.toImgUrl


interface CommentClickListener {
    fun commentAction(item: CommentListModel)
    fun replyAction(item: ReplyListModel)
    fun loadMoreReplys(item: CommentListModel)
}

class CommentListAdapter(val list: MutableList<CommentItemModel>, val listener: CommentClickListener): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val COMMENT = 0
        const val REPLY = 1
        const val REPLYBOTTOM = 2
    }

    override fun getItemViewType(position: Int): Int {
        val item = list[position]
        return if (item.type == 1) {
            COMMENT
        }else if (item.type == 2) {
            REPLY
        }else{
            REPLYBOTTOM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            0 -> {
                val binding = CommentListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return CommentListViewHolder(binding)
            }
            1 -> {
                val binding = ReplyListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return ReplyListViewHolder(binding)
            }
            else -> {
                val binding = ReplyBottomItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return ReplyBottomViewHolder(binding)
            }
        }

    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list[position]
        when (holder) {
            is CommentListViewHolder -> {
                item.commentItem?.let {
                    holder.bind(it, listener)
                }
            }
            is ReplyListViewHolder -> {
                item.replyItem?.let {
                    holder.bind(it, listener)
                }
            }
            is ReplyBottomViewHolder -> {
                item.commentItem?.let {
                    holder.bind(it, listener)
                }
            }
        }
    }


    fun refreshItem(items: List<CommentItemModel>) {
        list.clear()
        list.addAll(items)
        notifyDataSetChanged()
    }

    fun addItems(newList: List<CommentItemModel>) {
        val startPosition = list.size
        list.addAll(newList)
        notifyItemRangeInserted(startPosition, newList.size)
    }

    fun uploadItem(item: CommentItemModel) {
        if (item.commentItem != null) {
            val position = list.indexOfFirst { it.commentItem?.comment_id == item.commentItem?.comment_id }
            list[position] = item
            notifyItemChanged(position)

        }else if (item.replyItem != null) {
            val position = list.indexOfFirst { it.replyItem?.reply_id == item.replyItem?.reply_id }
            list[position] = item
            notifyItemChanged(position)
        }
    }
}

class CommentListViewHolder(var binding: CommentListItemBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(item: CommentListModel, listener: CommentClickListener) {
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

        binding.replyButton.setOnClickListener {
            listener.commentAction(item)
        }
    }
}

class ReplyListViewHolder(var binding: ReplyListItemBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(item: ReplyListModel, listener: CommentClickListener) {
        item.fromInfo?.avator?.let {
            Glide.with(BaseApplication.context).load(it.toImgUrl())
                .placeholder(ContextCompat.getDrawable(BaseApplication.context, R.drawable.icon_eee))
                .into(binding.headImg)
        }

        binding.timeText.text = item.create_time?.formatTime()
        binding.content.text = item.content

        binding.nickName.text = "${item.fromInfo?.username} 回复 ${item.toInfo?.username}"

        binding.replyButton.setOnClickListener {
            listener.replyAction(item)
        }
    }
}

class ReplyBottomViewHolder(var binding: ReplyBottomItemBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(item: CommentListModel, listener: CommentClickListener) {
        if (item.replys?.size == item.reply_count) {
            binding.replyBottomText.text = "暂无更多评论"
        }else{
            binding.replyBottomText.text = "加载更多评论"
            binding.replyBottomText.setOnClickListener{
                listener.loadMoreReplys(item)
            }
        }

    }
}