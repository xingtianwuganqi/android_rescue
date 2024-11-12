package com.rescue.flutter_720yun.home.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.home.models.TagInfoModel


interface TagListClickListener {
    fun onItemClick(item: TagInfoModel)
}

class TagListAdapter(private val list: MutableList<TagInfoModel>): RecyclerView.Adapter<TagListAdapter.ViewHolder>() {

    private var listener: TagListClickListener? = null
    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val textView: MaterialButton = view.findViewById(R.id.keyword)
    }

    override fun getItemCount(): Int {
        return list.size ?: 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.search_keyword_item, parent, false)
        return ViewHolder(view)
    }

    //给函数式接口传值，也就是传递一个函数，至于函数实现什么功能，用户自己定义
    fun setListener(listener: TagListClickListener?){
        this.listener = listener
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val keyword = item.tag_name
        holder.textView.text = keyword
        holder.textView.setOnClickListener {
            if (item.tag_type == 0) {
                listener?.onItemClick(item)
            }else{

            }
        }
        if (item.isSelected) {
            val colorValue = ContextCompat.getColor(BaseApplication.context, R.color.color_system)
            holder.textView.strokeColor = ColorStateList.valueOf(colorValue)
            holder.textView.setTextColor(colorValue)
        }else{
            val colorValue = ContextCompat.getColor(BaseApplication.context, R.color.color_bbb)
            val textColor = ContextCompat.getColor(BaseApplication.context, R.color.color_node)
            holder.textView.strokeColor = ColorStateList.valueOf(colorValue)
            holder.textView.setTextColor(textColor)
        }
    }

    fun addItems(items: List<TagInfoModel>) {
        list.clear()
        list.addAll(items)
        notifyDataSetChanged() // 通知适配器刷新所有项
    }

    fun uploadItem(item: TagInfoModel) {
        val position = list.indexOfFirst { it.id == item.id }
        list[position] = item
        notifyItemChanged(position)
    }

    fun clearItems() {
        list.clear()
        notifyDataSetChanged()
    }
}