package com.rescue.flutter_720yun.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.models.HomeListModel
import com.rescue.flutter_720yun.ui.home.HomeListViewHolder
import com.rescue.flutter_720yun.ui.home.OnItemClickListener

class HomeListAdapter(private val list: MutableList<HomeListModel>,
                      private val context: Context,
                      private val listener: OnItemClickListener):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_ITEM = 1
    private val VIEW_TYPE_LOADING = 2

    private var showNoMore = false
    override fun getItemCount(): Int {
        return if (showNoMore) list.size + 1 else list.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < list.size) VIEW_TYPE_ITEM else VIEW_TYPE_LOADING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_LOADING) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.loading_item, parent, false)
            return LoadingViewHolder(view)
        }else {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.home_item, parent, false)
            return HomeListViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HomeListViewHolder) {
            val item = list[position]
            holder.bind(context, item, listener)
        }
    }

    fun refreshItem(newList: List<HomeListModel>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged() // 通知适配器刷新所有项
    }

    fun addItems(newList: List<HomeListModel>) {
        val startPosition = list.size
        list.addAll(newList)
        notifyItemRangeInserted(startPosition, newList.size)
    }

    fun uploadItem(item: HomeListModel) {
        val position = list.indexOfFirst { it.topic_id == item.topic_id }
        list[position] = item
        notifyItemChanged(position)
    }

    fun clearItems() {
        list.clear()
        notifyDataSetChanged()
    }

    fun showNoMoreData() {
        showNoMore = true
        notifyItemInserted(list.size)
    }

    fun hideNoMoreData() {
        showNoMore = false
        notifyItemRemoved(list.size)
    }
}


class LoadingViewHolder(view:View): RecyclerView.ViewHolder(view) {
    var progressBar = view.findViewById<TextView>(R.id.progress_bar)
}