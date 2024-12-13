package com.rescue.flutter_720yun.show.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.show.activity.GambitListActivity
import com.rescue.flutter_720yun.show.models.GambitListModel
import com.rescue.flutter_720yun.show.viewmodels.GambitListViewModel

class GambitListAdapter(var list: MutableList<GambitListModel>, val tapListener: (GambitListModel) -> Unit): RecyclerView.Adapter<GambitListAdapter.ViewHolder>() {

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val backLinear: LinearLayout = view.findViewById(R.id.back_linear)
        val iconImage: ImageView = view.findViewById(R.id.icon_image)
        val textView: TextView = view.findViewById(R.id.gambit)
        val selectImage: ImageView = view.findViewById(R.id.select_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.gambit_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.textView.text = item.descript

        if (item.selected) {
            // 获取 drawable 资源（图标）
            val newIcon: Drawable? = ContextCompat.getDrawable(BaseApplication.context, R.drawable.icon_tag_se)
            // 设置新图标
            holder.selectImage.setImageDrawable(newIcon)
        }else{
            // 获取 drawable 资源（图标）
            val newIcon: Drawable? = ContextCompat.getDrawable(BaseApplication.context, R.drawable.icon_tag_un)
            // 设置新图标
            holder.selectImage.setImageDrawable(newIcon)
        }

        holder.backLinear.setOnClickListener {
            tapListener(item)
        }
    }

    fun refreshItem(newList: List<GambitListModel>) {
        list.clear()
        list.addAll(newList)
        notifyItemRangeChanged(0, newList.size) // 刷新一个范围内的项。
    }

    fun uploadItem(item: GambitListModel) {
        list = list.map { value ->
            value.selected = value.id == item.id
            value
        }.toMutableList()
        notifyItemRangeChanged(0, list.size) // 刷新一个范围内的项。
    }
}