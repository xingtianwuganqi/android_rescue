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

class UserSettingAdapter(private val list: List<MessageListModel>, private val adoptionStyle: Boolean = false, private val settingsStyle: Boolean = false): RecyclerView.Adapter<UserSettingAdapter.ItemViewHolder>() {

    private var clickListener: MessageListItemClickListener? = null
    inner class ItemViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val iconImage: ImageView = view.findViewById(R.id.icon_image)
        val title: TextView = view.findViewById(R.id.title)
        val rightIcon: ImageView = view.findViewById(R.id.right_icon)
        val content_layout: LinearLayout = view.findViewById(R.id.content_layout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val holder = LayoutInflater.from(parent.context).inflate(R.layout.user_setting_item, parent, false)
        if(adoptionStyle) {
            holder.layoutParams.height=(50*parent.resources.displayMetrics.density).toInt()
            holder.setBackgroundColor(android.graphics.Color.WHITE)
            holder.findViewById<View>(R.id.lineView).visibility=View.GONE
        }
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
        if(settingsStyle) {
            fun dp(value: Int)=(value*holder.itemView.resources.displayMetrics.density).toInt()
            val logout=item.category=="logout"
            holder.itemView.layoutParams.height=dp(if(logout) 60 else 50)
            holder.itemView.setPadding(0,if(logout) dp(10) else 0,0,0)
            holder.itemView.setBackgroundColor(android.graphics.Color.rgb(245,245,245))
            holder.content_layout.setBackgroundColor(android.graphics.Color.WHITE)
            holder.iconImage.setImageDrawable(if(drawableId!=0) androidx.core.content.ContextCompat.getDrawable(holder.itemView.context,drawableId) else null)
            holder.iconImage.visibility=if(logout) View.GONE else View.VISIBLE
            holder.iconImage.layoutParams.width=dp(22);holder.iconImage.layoutParams.height=dp(22)
            holder.rightIcon.visibility=if(logout) View.GONE else View.VISIBLE
            holder.rightIcon.layoutParams.width=dp(7);holder.rightIcon.layoutParams.height=dp(12)
            holder.title.gravity=if(logout) android.view.Gravity.CENTER else android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            holder.title.textSize=16f
            holder.itemView.findViewById<View>(R.id.lineView).visibility=if(logout) View.GONE else View.VISIBLE
        }

        holder.content_layout.setOnClickListener {
            clickListener?.itemClick(position)
        }
    }

    fun setClickListener(item: MessageListItemClickListener?) {
        clickListener = item
    }
}