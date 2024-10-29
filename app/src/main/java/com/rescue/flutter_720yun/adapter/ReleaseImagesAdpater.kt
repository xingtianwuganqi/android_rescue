package com.rescue.flutter_720yun.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.models.CoachReleasePhoto

class ReleaseImagesAdapter(var list: MutableList<CoachReleasePhoto>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
        when (viewType) {
            0 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.release_add_item, parent, false)
                return ReleaseAddViewHolder(view)
            }else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.release_image_item, parent, false)
                return ReleaseImageViewHolder(view)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

    }
}


class ReleaseAddViewHolder(view: View): RecyclerView.ViewHolder(view) {
    fun bind(item: CoachReleasePhoto) {

    }
}

class ReleaseImageViewHolder(view: View): RecyclerView.ViewHolder(view) {

}