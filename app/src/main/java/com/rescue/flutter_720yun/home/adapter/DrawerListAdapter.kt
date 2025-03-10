package com.rescue.flutter_720yun.home.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.DrawerListModel
import com.rescue.flutter_720yun.util.UserManager
import com.rescue.flutter_720yun.util.imageResourcesId
import com.rescue.flutter_720yun.util.toImgUrl

interface DrawerListClickListener {
    fun clickHeader()
    fun clickItem(item: DrawerListModel)
}

class DrawerListAdapter(val list: List<DrawerListModel>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var listener: DrawerListClickListener? = null

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
                if (UserManager.isLogin) {
                    holder.nickName.text = UserManager.userInfo?.username
                    Glide.with(BaseApplication.context)
                        .load(UserManager.userInfo?.avator?.toImgUrl())
                        .placeholder(R.drawable.icon_eee)
                        .into(holder.headImage)
                }else {
                    holder.nickName.text = ContextCompat.getString(BaseApplication.context, R.string.user_login)
                    holder.headImage.setImageResource(R.drawable.icon_eee)
                }
                holder.headBack.setOnClickListener {
                    listener?.clickHeader()
                }
            }
            is DrawerInfoViewHolder -> {
                holder.headImage.setImageResource(item.icon.imageResourcesId())
                holder.title.text = item.name
                holder.contentBack.setOnClickListener { 
                    listener?.clickItem(item)
                }
            }
        }
    }

    fun setListener(listener: DrawerListClickListener) {
        this.listener = listener
    }
}

class DrawerHeadViewHolder(view: View): RecyclerView.ViewHolder(view){
    val headImage: ImageView = view.findViewById(R.id.head_img)
    val nickName: TextView = view.findViewById(R.id.nick_name)
    val headBack: LinearLayout = view.findViewById(R.id.head_back)
}


class DrawerInfoViewHolder(view: View): RecyclerView.ViewHolder(view){
    val headImage: ImageView = view.findViewById(R.id.icon_image)
    val title: TextView = view.findViewById(R.id.title)
    val numberBtn: MaterialButton = view.findViewById(R.id.number_text)
    val contentBack: LinearLayout = view.findViewById(R.id.content_back)
}