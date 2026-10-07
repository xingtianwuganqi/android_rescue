package com.rescue.flutter_720yun.adoption.adapter

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.adoption.models.AdoptionNotification
import com.rescue.flutter_720yun.adoption.ui.*
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AdoptionReminderAdapter(private val click: (AdoptionNotification) -> Unit) : RecyclerView.Adapter<AdoptionReminderAdapter.Holder>() {
    private var items=emptyList<AdoptionNotification>()
    private var reading: Int?=null
    class Holder(val root: LinearLayout): RecyclerView.ViewHolder(root)
    fun submit(value: List<AdoptionNotification>, readingId: Int?) { items=value; reading=readingId; notifyDataSetChanged() }
    override fun getItemCount()=items.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)=Holder(LinearLayout(parent.context).apply {
        orientation=LinearLayout.VERTICAL; setPadding(16,8,16,16); isSaveEnabled=false
        layoutParams=RecyclerView.LayoutParams(-1,-2)
    })
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item=items[position]; holder.root.removeAllViews()
        val time=runCatching { OffsetDateTime.parse(item.created_at).atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) }.getOrNull() ?: item.created_at.orEmpty()
        holder.root.label(item.message.orEmpty()+"\n"+time)
        holder.root.button(if(reading==item.id) "处理中…" else "查看提醒") { click(item) }.isEnabled=reading==null
    }
}
