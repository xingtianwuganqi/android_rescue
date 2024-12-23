package com.rescue.flutter_720yun.show.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.makeramen.roundedimageview.RoundedImageView
import com.rbrooks.indefinitepagerindicator.IndefinitePagerIndicator
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.show.models.ShowPageModel
import com.rescue.flutter_720yun.util.formatTime
import com.rescue.flutter_720yun.util.getImages
import com.rescue.flutter_720yun.util.toImgUrl
import com.webtoonscorp.android.readmore.ReadMoreTextView

class ShowPageListAdapter(val list: MutableList<ShowPageModel>): RecyclerView.Adapter<ShowPageListAdapter.ViewHolder>() {
    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val roundedImage: RoundedImageView = view.findViewById(R.id.head_img)
        val nickName: TextView = view.findViewById(R.id.nick_name)
        val timeText: TextView = view.findViewById(R.id.time_text)
        val moreButton: ImageButton = view.findViewById(R.id.more_btn)
        val viewPager: ViewPager2 = view.findViewById(R.id.viewPager2)
        val content: ReadMoreTextView = view.findViewById(R.id.content)
        val comment: TextView = view.findViewById(R.id.comment)
        var indicator: IndefinitePagerIndicator = view.findViewById(R.id.viewpager_pager_indicator)
        val gambitButton: MaterialButton = view.findViewById(R.id.gambit_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.show_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        item.user?.avator?.let {
            val imgStr = it.toImgUrl()
            Glide.with(BaseApplication.context)
                .load(imgStr)
                .placeholder(R.drawable.icon_eee)
                .into(holder.roundedImage)
        }
        holder.timeText.text = item.create_time?.formatTime()
        holder.nickName.text = item.user?.username
        if (item.gambit_type != null) {
            holder.gambitButton.visibility = View.VISIBLE
            holder.gambitButton.text = item.gambit_type?.descript
        }else{
            holder.gambitButton.visibility = View.GONE
        }

        val images = item.getImages()
        val adapter = images?.map {
            it.toImgUrl()
        }?.let { ImagePagerAdapter(it) }

        holder.viewPager.adapter = adapter
        holder.indicator.attachToViewPager2(holder.viewPager)
        if ((images?.size ?: 0) > 1) {
            holder.indicator.visibility = View.VISIBLE
        }else{
            holder.indicator.visibility = View.GONE
        }
        holder.content.text = item.instruction
    }

    fun refreshItem(newList: List<ShowPageModel>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    fun addItems(newList: List<ShowPageModel>) {
        val startPosition = list.size
        list.addAll(newList)
        notifyItemRangeInserted(startPosition, newList.size)
    }

    fun uploadItem(item: ShowPageModel) {
        val position = list.indexOfFirst { it.show_id == item.show_id }
        list[position] = item
        notifyItemChanged(position)
    }

    // ViewPager2 的适配器
    class ImagePagerAdapter(private val imageUrls: List<String>) :
        RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val imageView = ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            return ImageViewHolder(imageView)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val imageUrl = imageUrls[position]
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .into(holder.imageView)
        }

        override fun getItemCount(): Int = imageUrls.size

        inner class ImageViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)
    }
}