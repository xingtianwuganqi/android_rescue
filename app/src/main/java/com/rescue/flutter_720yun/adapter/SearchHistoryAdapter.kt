package com.rescue.flutter_720yun.adapter

import android.opengl.Visibility
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.models.SearchHistoryItemModel
import com.rescue.flutter_720yun.models.SearchKeywordModel



interface SearchHistoryItemClickListener {
    fun itemClick(keyword: String?)
    fun onDeleteClick()
}

class SearchHistoryAdapter(private val list: List<SearchHistoryItemModel>): RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder>() {

    private var clickListener: SearchHistoryItemClickListener? = null
    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.history_title)
        var recyclerView: RecyclerView = view.findViewById(R.id.item_list)
        val deleteBtn: ImageButton = view.findViewById(R.id.delete_button)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.search_history_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == 0) {
            holder.deleteBtn.visibility = View.GONE
        }else{
            holder.deleteBtn.visibility = View.VISIBLE
        }
        holder.textView.text = list[position].title
        val adapter = SearchKeywordAdapter(list[position].list)
        val layoutManager = FlexboxLayoutManager(BaseApplication.context)
        holder.recyclerView.layoutManager = layoutManager
        holder.recyclerView.adapter = adapter
        adapter.setListener(this.clickListener)
        holder.deleteBtn.setOnClickListener {
            this.clickListener?.onDeleteClick()
        }
    }

    fun setItemClickListener(listener: SearchHistoryItemClickListener){
        this.clickListener = listener
    }
}

class SearchKeywordAdapter(private val list: List<SearchKeywordModel>?): RecyclerView.Adapter<SearchKeywordAdapter.ViewHolder>() {

    private var listener: SearchHistoryItemClickListener? = null
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

    //给函数式接口传值，也就是传递一个函数，至于函数实现什么功能，用户自己定义
    fun setListener(listener: SearchHistoryItemClickListener?){
        this.listener = listener
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        list?.let {
            val keyword = it[position].keyword
            holder.textView.text = keyword
            holder.textView.setOnClickListener {
                listener?.itemClick(keyword)
            }
        }
    }
}