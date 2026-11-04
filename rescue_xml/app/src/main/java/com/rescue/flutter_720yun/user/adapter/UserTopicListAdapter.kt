package com.rescue.flutter_720yun.user.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rescue.flutter_720yun.BuildConfig
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.UserTopicItemBinding
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.show.models.ShowPageModel
import com.rescue.flutter_720yun.util.getImages
import com.rescue.flutter_720yun.util.toImgUrl

class UserTopicListAdapter(var list: MutableList<HomeListModel>,
                           val clickListener: (HomeListModel) -> Unit,
                           val promote: ((HomeListModel)->Unit)? = null,
                           val help: (() -> Unit)? = null
): RecyclerView.Adapter<UserTopicListAdapter.ViewHolder>() {

    inner class ViewHolder(var binding: UserTopicItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HomeListModel){
            Glide.with(BaseApplication.context).load(item.getImages()?.first()?.toImgUrl())
                .placeholder(R.drawable.icon_eee)
                .into(binding.topicImage)

            Glide.with(BaseApplication.context).load(item.userInfo?.avator?.toImgUrl())
                .placeholder(R.drawable.icon_eee)
                .into(binding.headImg)

            binding.content.text = item.userInfo?.username

            binding.imgBack.setOnClickListener {
                clickListener(item)
            }

            val own=(item.userInfo?.id ?: item.user)==com.rescue.flutter_720yun.util.UserManager.userId && com.rescue.flutter_720yun.util.UserManager.isLogin
            val visible=BuildConfig.ADS_ENABLED && own && promote!=null
            binding.promotionRow.visibility=if(visible) View.VISIBLE else View.GONE
            binding.promotionAction.visibility=if(visible) View.VISIBLE else View.GONE
            binding.promotionHelp.setOnClickListener { if(visible) help?.invoke() }
            binding.promotionStatus.text=com.rescue.flutter_720yun.promotion.PromotionDisplay.label(item.promotion)
            binding.promotionStatus.visibility=if(own && binding.promotionStatus.text.isNotEmpty()) View.VISIBLE else View.GONE
            binding.promotionAction.text=com.rescue.flutter_720yun.promotion.models.PromotionUiPolicy.title(item.is_complete==true,item.workflow_status,item.promotion)
            val enabled=com.rescue.flutter_720yun.promotion.models.PromotionUiPolicy.enabled(BuildConfig.ADS_ENABLED,own,item.is_complete==true,item.workflow_status,item.promotion,
                com.rescue.flutter_720yun.promotion.RewardedTopicPromotionCoordinator.busy)
            binding.promotionAction.isEnabled=enabled
            binding.promotionAction.alpha=if(enabled) 1f else 0.35f
            binding.promotionAction.setOnClickListener { if(enabled) promote?.invoke(item) }
            if (item.is_complete == true) {
                binding.completion.visibility = View.VISIBLE
            }else{
                binding.completion.visibility = View.GONE
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
        if (item is HomeListModel) {
            holder.bind(item)
        }
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

    fun cleanItems() {
        list.clear()
        notifyDataSetChanged()
    }

    fun uploadItem(item: HomeListModel) {
        val position = list.indexOfFirst { it.topic_id == item.topic_id }
        if (position>=0) {
            list[position] = item
            notifyItemChanged(position)
        }
    }
}


class UserShowListAdapter(var list: MutableList<ShowPageModel>,
                           val clickListener: (ShowPageModel) -> Unit
): RecyclerView.Adapter<UserShowListAdapter.ViewHolder>() {

    inner class ViewHolder(var binding: UserTopicItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ShowPageModel){

            Glide.with(BaseApplication.context).load(item.getImages()?.first()?.toImgUrl())
                .placeholder(R.drawable.icon_eee)
                .into(binding.topicImage)

            Glide.with(BaseApplication.context).load(item.user?.avator?.toImgUrl())
                .placeholder(R.drawable.icon_eee)
                .into(binding.headImg)

            binding.content.text = item.user?.username

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

    fun refreshItem(items: List<ShowPageModel>) {
        list.clear()
        list.addAll(items)
        notifyDataSetChanged()
    }

    fun addItems(newList: List<ShowPageModel>) {
        val startPosition = list.size
        list.addAll(newList)
        notifyItemRangeInserted(startPosition, newList.size)
    }
}