package com.rescue.flutter_720yun.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.TopicReportItemBinding
import com.rescue.flutter_720yun.home.models.HomeListModel
import com.rescue.flutter_720yun.home.models.ReportTypeModel
import com.rescue.flutter_720yun.user.adapter.BlackListAdapter

class TopicReportAdapter(var list: MutableList<ReportTypeModel>, val onClickListener: (ReportTypeModel) -> Unit): RecyclerView.Adapter<TopicReportAdapter.TopicReportViewHolder>() {
    inner class TopicReportViewHolder(var binding: TopicReportItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ReportTypeModel) {
            binding.text.text = item.vio_name
            if (item.selected) {
                binding.selectButton.setImageResource(R.drawable.icon_tag_se)
            }else{
                binding.selectButton.setImageResource(R.drawable.icon_tag_un)
            }

            binding.reportType.setOnClickListener {
                onClickListener(item)
            }

            binding.selectButton.setOnClickListener {
                onClickListener(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicReportViewHolder {
        val binding = TopicReportItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TopicReportViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: TopicReportViewHolder, position: Int) {
        val item = list[position]
        holder.bind(item)
    }

    fun reloadList(newItems: List<ReportTypeModel>) {
        list.clear()
        list.addAll(newItems)
        notifyDataSetChanged()
    }

//    fun uploadItem(item: ReportTypeModel) {
//        val position = list.indexOfFirst { it.id == item.id }
//        if (list.isNotEmpty()) {
//            list[position] = item
//            notifyItemChanged(position)
//        }
//    }
}