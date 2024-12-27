package com.rescue.flutter_720yun.message.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.CommentListItemBinding
import com.rescue.flutter_720yun.show.models.CommentListModel

class CommentListAdapter(val list: MutableList<CommentListModel>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = CommentListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentListViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return 10
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

    }


}

class CommentListViewHolder(var binding: CommentListItemBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(item: CommentListModel) {
        item.userInfo?.let {
            Glide.with(BaseApplication.context).load(it)
                .placeholder(ContextCompat.getDrawable(BaseApplication.context, R.drawable.icon_eee))
                .into(binding.headImg
            )

            item.userInfo?.username?.let { userName ->
                binding.nickName.text = userName
            }


        }
    }
}