package com.rescue.flutter_720yun.home.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.util.dpToPx
import com.rescue.flutter_720yun.util.toImgUrl

class TopicImgAdapter(
    private val totalImages: List<String>,
    private val imgStr: List<String>,
    private val listener: OnChildItemClickListener
    ): RecyclerView.Adapter<TopicImageViewHolder>() {
    interface OnChildItemClickListener  {
        fun onChildItemClick(position: Int)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.topic_img_item, parent, false)
        return TopicImageViewHolder(view)
    }

    override fun getItemCount(): Int {
        return imgStr.size
    }

    override fun onBindViewHolder(holder: TopicImageViewHolder, position: Int) {
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
                holder.textView.visibility = View.GONE
            }else{
                val layoutParams = holder.imgView.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.setMargins(2.dpToPx(), 0, 0, 0)
                holder.imgView.layoutParams = layoutParams
                if (totalImages.size > 2) {
                    holder.textView.visibility = View.VISIBLE
                    holder.textView.text = "+${totalImages.size - 2}"
                }else{
                    holder.textView.visibility = View.GONE
                }
            }
        }

        holder.imgView.setOnClickListener{
            listener.onChildItemClick(position)
        }
    }
}


class TopicImageViewHolder(view: View): RecyclerView.ViewHolder(view) {
    val imgView: ImageView = view.findViewById(R.id.topic_img)
    val textView: TextView = view.findViewById(R.id.number_text)
}