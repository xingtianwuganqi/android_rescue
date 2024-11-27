package com.rescue.flutter_720yun.home.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.HomeListModel

class HomeListAdapter(private val list: MutableList<HomeListModel>,
                      private val context: Context,
                      private val listener: OnItemClickListener
):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemCount(): Int {
        return  list.size
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.home_item, parent, false)
        return HomeListViewHolder(view)
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
        notifyItemRangeChanged(0, newList.size) // 刷新一个范围内的项。
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
}


class LoadingViewHolder(view:View): RecyclerView.ViewHolder(view) {
    var progressBar = view.findViewById<TextView>(R.id.progress_bar)
}