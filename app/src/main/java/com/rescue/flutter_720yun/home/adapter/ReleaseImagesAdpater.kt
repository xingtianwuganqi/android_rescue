package com.rescue.flutter_720yun.home.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.CoachReleaseInfo
import com.rescue.flutter_720yun.home.models.CoachReleasePhoto
import com.rescue.flutter_720yun.home.models.HomeListModel

interface ReleaseImageClickListener{
    fun addImageClick()
    fun deleteImageClick(item: CoachReleasePhoto)
}

class ReleaseImagesAdapter(val list: MutableList<CoachReleasePhoto>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var listener: ReleaseImageClickListener? = null
    companion object {
        const val ADD = 0
        const val IMAGE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (list[position].isAdd) {
            true -> ADD
            else -> IMAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.release_add_item, parent, false)
                ReleaseAddViewHolder(view)
            }else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.release_image_item, parent, false)
                ReleaseImageViewHolder(view)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list[position]
        when (holder) {
            is ReleaseAddViewHolder -> {
                holder.linearLayout.setOnClickListener {
                    listener?.addImageClick()
                }
            }
            is ReleaseImageViewHolder -> {
                holder.cleanButton.setOnClickListener {
                    listener?.deleteImageClick(item)
                }
                Glide.with(BaseApplication.context)
                    .load(item.media?.compressPath)
                    .into(holder.imageView)
            }
        }
    }

    fun setClickListener(listener: ReleaseImageClickListener) {
        this.listener = listener
    }

    fun refreshItems(values: List<CoachReleasePhoto>) {
        list.clear()
        list.addAll(values)
        notifyDataSetChanged()
    }
}


class ReleaseAddViewHolder(view: View): RecyclerView.ViewHolder(view) {
    val linearLayout: LinearLayout = view.findViewById(R.id.linear_layout)

}

class ReleaseImageViewHolder(view: View): RecyclerView.ViewHolder(view) {
    val cleanButton: ImageView = view.findViewById(R.id.clear_button)
    val imageView: ImageView = view.findViewById(R.id.imageView)
}