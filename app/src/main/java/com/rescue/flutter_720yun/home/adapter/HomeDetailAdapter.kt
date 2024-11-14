package com.rescue.flutter_720yun.home.adapter

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
import com.rescue.flutter_720yun.home.models.HomeDetailModel
import com.rescue.flutter_720yun.util.dpToPx
import com.rescue.flutter_720yun.util.loadScaleImage
import com.rescue.flutter_720yun.util.toImgUrl

interface DetailImgClickListener {
    fun clickItem(model: List<HomeDetailModel>, position: Int)
}

class HomeDetailAdapter(
    private val detailList: List<HomeDetailModel>
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onClickListener: DetailImgClickListener? = null

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
                val view = LayoutInflater.from(parent.context).inflate(R.layout.detail_image_item,
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
            is HomeDetailImageViewHolder -> {
                val item = detailList[position]
                item.imageStr?.let {
                    val imgStr = it.toImgUrl()
                    loadScaleImage(BaseApplication.context, imgStr, holder.imageView)
                }
                val layoutParams = holder.imageView.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.setMargins(0, 0, 0, 10.dpToPx())
                holder.imageView.layoutParams = layoutParams

                holder.imageView.setOnClickListener {
                    onClickListener?.clickItem(detailList, position)
                }
            }
        }
    }

    fun setOnClickListener(listener: DetailImgClickListener) {
        onClickListener = listener
    }
}

class HomeDetailContentViewHolder(view: View): RecyclerView.ViewHolder(view) {
    private val headImg: RoundedImageView = view.findViewById(R.id.head_img)
    private val nickName: TextView = view.findViewById(R.id.nick_name)
    private val tagInfo: RecyclerView = view.findViewById(R.id.tag_info)
    private val contentText: TextView = view.findViewById(R.id.content)

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
}