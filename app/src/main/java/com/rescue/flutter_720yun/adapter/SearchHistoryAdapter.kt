package com.rescue.flutter_720yun.adapter

import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.models.SearchHistoryItemModel
import com.rescue.flutter_720yun.models.SearchKeywordModel

class SearchHistoryAdapter(private val list: List<SearchHistoryItemModel>): RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.history_title)
        var recyclerView: RecyclerView = view.findViewById(R.id.item_list)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.search_history_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = list[position].title
        val adapter = SearchKeywordAdapter(list[position].list)
        holder.recyclerView.layoutManager = LinearLayoutManager(BaseApplication.context,LinearLayoutManager.HORIZONTAL, false)
        holder.recyclerView.adapter = adapter
    }
}

class SearchKeywordAdapter(private val list: List<SearchKeywordModel>?): RecyclerView.Adapter<SearchKeywordAdapter.ViewHolder>() {

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.keyword)
    }

    override fun getItemCount(): Int {
        return list?.size ?: 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.search_keyword_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        list?.let {
            holder.textView.text = it[position].keyword
            // 设置背景颜色
            val background = GradientDrawable()
            val colorValue = ContextCompat.getColor(BaseApplication.context, R.color.color_system)
            background.setColor(colorValue)
            val cornerRadius: Float = 5F
            background.cornerRadius = cornerRadius
// 将这个背景应用到TextView
            holder.textView.background = background
        }
    }
}