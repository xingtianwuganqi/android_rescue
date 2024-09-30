package com.rescue.flutter_720yun.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.util.dpToPx
import com.rescue.flutter_720yun.util.toImgUrl

class TopicImgAdapter(
    private val imgStr: List<String>,
    private val listener: OnChildItemClickListener
    ): RecyclerView.Adapter<TopicImgAdapter.ViewHolder>() {
    interface OnChildItemClickListener  {
        fun onChildItemClick(position: Int)
    }
    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val imgView: ImageView = view.findViewById(R.id.topic_img)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.topic_img_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return imgStr.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imgUrl = imgStr[position]
        Glide.with(BaseApplication.context)
            .load(imgUrl.toImgUrl())
            .placeholder(R.drawable.icon_eee)
            .into(holder.imgView)
        if (imgStr.size > 1) {
            if (position == 0) {
                val layoutParams = holder.imgView.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.setMargins(0, 0, 2.dpToPx(), 0)
                holder.imgView.layoutParams = layoutParams
            }else{
                val layoutParams = holder.imgView.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.setMargins(2.dpToPx(), 0, 0, 0)
                holder.imgView.layoutParams = layoutParams
            }
        }

        holder.imgView.setOnClickListener{
            listener.onChildItemClick(position)
        }
    }
}