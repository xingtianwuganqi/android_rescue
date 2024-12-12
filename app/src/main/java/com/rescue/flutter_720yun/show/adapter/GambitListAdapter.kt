package com.rescue.flutter_720yun.show.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.show.activity.GambitListActivity
import com.rescue.flutter_720yun.show.models.GambitListModel
import com.rescue.flutter_720yun.show.viewmodels.GambitListViewModel

class GambitListAdapter(val list: MutableList<GambitListModel>): RecyclerView.Adapter<GambitListAdapter.ViewHolder>() {

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.gambit)
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
    }

    fun refreshItem(newList: List<GambitListModel>) {
        list.clear()
        list.addAll(newList)
        notifyItemRangeChanged(0, newList.size) // 刷新一个范围内的项。
    }
}