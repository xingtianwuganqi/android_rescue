package com.rescue.flutter_720yun.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.models.DrawerListModel
import com.rescue.flutter_720yun.util.imageResourcesId

class DrawerListAdapter(val list: List<DrawerListModel>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val HEAD = 0
        const val DEFAULT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> HEAD
            else -> DEFAULT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when(viewType) {
            0 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.drawer_head_item, parent, false)
                return DrawerHeadViewHolder(view)
            }else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.drawer_info_item, parent, false)
                return DrawerInfoViewHolder(view)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list[position]
        when (holder) {
            is DrawerHeadViewHolder -> {
                holder.nickName.text = "登录/注册"
            }
            is DrawerInfoViewHolder -> {
                holder.headImage.setImageResource(item.icon.imageResourcesId())
                holder.title.text = item.name

            }
        }
    }
}

class DrawerHeadViewHolder(view: View): RecyclerView.ViewHolder(view){
    val headImage: ImageView = view.findViewById(R.id.head_img)
    val nickName: TextView = view.findViewById(R.id.nick_name)
}


class DrawerInfoViewHolder(view: View): RecyclerView.ViewHolder(view){
    val headImage: ImageView = view.findViewById(R.id.icon_image)
    val title: TextView = view.findViewById(R.id.title)
    val numberBtn: MaterialButton = view.findViewById(R.id.number_text)
}