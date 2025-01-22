package com.rescue.flutter_720yun.user.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.MessageListModel
import com.rescue.flutter_720yun.message.adapter.MessageListItemClickListener

class UserSettingAdapter(private val list: List<MessageListModel>): RecyclerView.Adapter<UserSettingAdapter.ItemViewHolder>() {

    private var clickListener: MessageListItemClickListener? = null
    inner class ItemViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val iconImage: ImageView = view.findViewById(R.id.icon_image)
        val title: TextView = view.findViewById(R.id.title)
        val rightIcon: ImageView = view.findViewById(R.id.right_icon)
        val content_layout: LinearLayout = view.findViewById(R.id.content_layout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val holder = LayoutInflater.from(parent.context).inflate(R.layout.user_setting_item, parent, false)
        return ItemViewHolder(holder)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = list[position]
        val packageName = BaseApplication.context.packageName
        val drawableId = BaseApplication.context.resources.getIdentifier(item.icon, "drawable", packageName)
        if (drawableId != 0) {
            holder.iconImage.setImageResource(drawableId)
        }
        holder.title.text = item.title

        holder.content_layout.setOnClickListener {
            clickListener?.itemClick(position)
        }
    }

    fun setClickListener(item: MessageListItemClickListener?) {
        clickListener = item
    }
}