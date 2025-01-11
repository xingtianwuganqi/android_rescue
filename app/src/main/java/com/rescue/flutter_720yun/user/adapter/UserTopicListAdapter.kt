package com.rescue.flutter_720yun.user.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.UserTopicItemBinding
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.home.viewmodels.HomeViewModel
import com.rescue.flutter_720yun.show.models.CommentItemModel
import com.rescue.flutter_720yun.util.getImages
import com.rescue.flutter_720yun.util.toImgUrl

class UserTopicListAdapter(var list: MutableList<HomeListModel>,
                           val clickListener: (HomeListModel) -> Unit
): RecyclerView.Adapter<UserTopicListAdapter.ViewHolder>() {

    inner class ViewHolder(var binding: UserTopicItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HomeListModel){
            Glide.with(BaseApplication.context).load(item.getImages()?.first()?.toImgUrl())
                .placeholder(R.drawable.icon_eee)
                .into(binding.topicImage)

            Glide.with(BaseApplication.context).load(item.userInfo?.avator?.toImgUrl())
                .placeholder(R.drawable.icon_eee)
                .into(binding.headImg)

            binding.content.text = item.content

            binding.imgBack.setOnClickListener {
                clickListener(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = UserTopicItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.bind(item)
    }

    fun refreshItem(items: List<HomeListModel>) {
        list.clear()
        list.addAll(items)
        notifyDataSetChanged()
    }

    fun addItems(newList: List<HomeListModel>) {
        val startPosition = list.size
        list.addAll(newList)
        notifyItemRangeInserted(startPosition, newList.size)
    }
}