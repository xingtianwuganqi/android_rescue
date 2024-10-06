package com.rescue.flutter_720yun.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.makeramen.roundedimageview.RoundedImageView
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.models.HomeDetailModel
import com.rescue.flutter_720yun.models.HomeListModel
import com.rescue.flutter_720yun.ui.home.TagInfoAdapter
import com.rescue.flutter_720yun.util.dpToPx
import com.rescue.flutter_720yun.util.getImages
import com.rescue.flutter_720yun.util.toImgUrl

class HomeDetailAdapter(
    private val detailList: List<HomeDetailModel>
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val DETAIL = 0
        const val IMAGE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (detailList[position].type) {
            0 -> DETAIL
            1 -> IMAGE
            else -> throw IllegalArgumentException("Invalid item type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            DETAIL -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.home_detail_content,
                    parent,
                    false)
                HomeDetailContentViewHolder(view)
            }
            IMAGE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.topic_img_item,
                    parent,
                    false)
                HomeDetailImageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount(): Int {
        return detailList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HomeDetailContentViewHolder -> holder.bind(detailList[position])
            is HomeDetailImageViewHolder -> holder.bind(detailList[position])
        }
    }
}

class HomeDetailContentViewHolder(view: View): RecyclerView.ViewHolder(view) {
    val headImg: RoundedImageView = view.findViewById(R.id.head_img)
    val nickName: TextView = view.findViewById(R.id.nick_name)
    val tagInfo: RecyclerView = view.findViewById(R.id.tag_info)
    val contentText: TextView = view.findViewById(R.id.content)

    fun bind(detailData: HomeDetailModel) {
        detailData.data?.userInfo?.avator?.let {
            val imgStr = it.toImgUrl()
            Glide.with(BaseApplication.context)
                .load(imgStr)
                .placeholder(R.drawable.icon_eee)
                .into(headImg)
        }
        nickName.text = detailData.data?.userInfo?.username

        if (detailData.data?.tagInfos?.isNotEmpty() == true) {
            detailData.data?.tagInfos?.let {
                tagInfo.visibility = View.VISIBLE
                tagInfo.adapter = TagInfoAdapter(it)
                tagInfo.layoutManager = LinearLayoutManager(BaseApplication.context,
                    LinearLayoutManager.HORIZONTAL,
                    false)
            }
            val paddingTop = 26 * BaseApplication.context.resources.displayMetrics.density
            contentText.setPadding(0, paddingTop.toInt(), 0, 0)
        }else{
            tagInfo.visibility = View.GONE
            contentText.setPadding(0, 0, 0, 0)
        }

        contentText.text = detailData.data?.content
    }
}

class HomeDetailImageViewHolder(view: View): RecyclerView.ViewHolder(view) {
    var imageView: ImageView = view.findViewById(R.id.topic_img)
    fun bind(detail: HomeDetailModel) {
        detail.imageStr?.let {
            val imgStr = it.toImgUrl()
            Glide.with(BaseApplication.context)
                .load(imgStr)
                .placeholder(R.drawable.icon_eee)
                .into(imageView)
        }
        val layoutParams = imageView.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.setMargins(0, 0, 0, 10.dpToPx())
        imageView.layoutParams = layoutParams

    }
}