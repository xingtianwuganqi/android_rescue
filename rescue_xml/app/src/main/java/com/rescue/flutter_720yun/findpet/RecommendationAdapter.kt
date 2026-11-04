package com.rescue.flutter_720yun.findpet

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adoption.models.AdoptionTime
import com.rescue.flutter_720yun.databinding.FindPetRecommendationItemBinding
import com.rescue.flutter_720yun.util.toImgUrl

class RecommendationAdapter(private val click: (PetRecommendation) -> Unit) : RecyclerView.Adapter<RecommendationAdapter.Holder>() {
    private var items = emptyList<PetRecommendation>()
    private var busy = false
    class Holder(val b: FindPetRecommendationItemBinding) : RecyclerView.ViewHolder(b.root)
    fun submit(items: List<PetRecommendation>, busy: Boolean) { this.items = items; this.busy = busy; notifyDataSetChanged() }
    override fun getItemCount() = items.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(FindPetRecommendationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]; val b = holder.b
        b.recommendationStatus.text = item.statusLabel
        b.recommendationContent.text = if (item.available) item.content?.takeIf { it.isNotBlank() } ?: "查看送养帖子" else "帖子已删除或不可公开"
        b.recommendationCity.text = if (item.available) item.city_name.orEmpty() else ""
        b.recommendationCity.isVisible = item.available && !item.city_name.isNullOrBlank()
        b.recommendationTime.text = "系统推荐 · " + AdoptionTime.display(item.created_at)
        b.recommendationUnread.isVisible = !item.read
        Glide.with(b.recommendationPhoto).clear(b.recommendationPhoto)
        if (item.available) Glide.with(b.recommendationPhoto).load(item.image?.toImgUrl()).placeholder(R.drawable.icon_eee).error(R.drawable.icon_eee).into(b.recommendationPhoto)
        else b.recommendationPhoto.setImageResource(R.drawable.icon_eee)
        b.root.isEnabled = !busy
        b.root.setOnClickListener { click(item) }
        b.root.contentDescription = if (item.available) "查看送养帖子" else "帖子不可用，点击标记已读"
    }
    override fun onViewRecycled(holder: Holder) { Glide.with(holder.b.recommendationPhoto).clear(holder.b.recommendationPhoto); super.onViewRecycled(holder) }
}
