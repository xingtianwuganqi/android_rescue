package com.rescue.flutter_720yun.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.TopicReportItemBinding
import com.rescue.flutter_720yun.user.adapter.BlackListAdapter

class TopicReportAdapter: RecyclerView.Adapter<TopicReportAdapter.TopicReportViewHolder>() {
    inner class TopicReportViewHolder(var binding: TopicReportItemBinding): RecyclerView.ViewHolder(binding.root) {
//        fun bind(item: )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicReportViewHolder {
        val binding = TopicReportItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TopicReportViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return 10
    }

    override fun onBindViewHolder(holder: TopicReportViewHolder, position: Int) {

    }
}