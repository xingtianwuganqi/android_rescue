package com.rescue.flutter_720yun.adapter

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
import com.rescue.flutter_720yun.models.TagInfoModel


interface TagListClickListener {
    fun onItemClick(item: TagInfoModel)
}

class TagListAdapter(private val list: List<TagInfoModel>?): RecyclerView.Adapter<TagListAdapter.ViewHolder>() {

    private var listener: TagListClickListener? = null
    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val textView: MaterialButton = view.findViewById(R.id.keyword)
    }

    override fun getItemCount(): Int {
        return list?.size ?: 0
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
        list?.let {
            val item = it[position]
            val keyword = item.tag_name
            holder.textView.text = keyword
            holder.textView.setOnClickListener {
                listener?.onItemClick(item)
            }
            if (item.isSelected) {
                val colorValue = ContextCompat.getColor(BaseApplication.context, R.color.color_system)
                holder.textView.strokeColor = ColorStateList.valueOf(colorValue)
                holder.textView.setTextColor(colorValue)
            }else{
                val colorValue = ContextCompat.getColor(BaseApplication.context, R.color.color_eee)
                val textColor = ContextCompat.getColor(BaseApplication.context, R.color.color_node)
                holder.textView.strokeColor = ColorStateList.valueOf(colorValue)
                holder.textView.setTextColor(textColor)
            }
        }
    }
}