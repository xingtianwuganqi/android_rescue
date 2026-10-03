package com.rescue.flutter_720yun.ads

import android.app.Activity
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.BuildConfig
import java.util.IdentityHashMap

/** Interleaves per-page ads without inserting fake records into business models or pagination. */
class NativeFeedAdapter(
    private val activity: Activity,
    private val owner: LifecycleOwner,
    contentAdapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
    private val placement: FeedAdPlacement,
    private val placementId: String = BuildConfig.TAKU_NATIVE_ID
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), DefaultLifecycleObserver {
    @Suppress("UNCHECKED_CAST")
    private val content = contentAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
    private data class Slot(var anchor: Int, val adapter: NativeAdAdapter, val observer: RecyclerView.AdapterDataObserver)
    private sealed class Row {
        data class Content(val index: Int) : Row()
        data class Ad(val adapter: NativeAdAdapter) : Row()
    }
    private val slots = mutableListOf<Slot>()
    private val boundAds = IdentityHashMap<RecyclerView.ViewHolder, NativeAdAdapter>()
    private var rows = emptyList<Row>()
    private var destroyed = false
    private val contentObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() = rebuild()
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) = rebuild()
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) = rebuild()
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = rebuild()
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            slots.forEach { if (it.anchor > positionStart) it.anchor -= minOf(itemCount, it.anchor - positionStart) }
            rebuild()
        }
    }

    init {
        content.registerAdapterDataObserver(contentObserver)
        owner.lifecycle.addObserver(this)
        rebuild()
    }

    /** Call after applying a successful refresh/append to the business adapter. */
    fun pageLoaded(pageSize: Int, refresh: Boolean) {
        if (destroyed) return
        if (refresh) releaseSlots()
        val index = placement.insertionIndex(pageSize, refresh)
        if (index != null && placementId.isNotBlank()) {
            val start = if (refresh) 0 else content.itemCount - pageSize
            val adapter = NativeAdAdapter(activity, owner, placementId)
            val observer = object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() = rebuild()
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = rebuild()
                override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = rebuild()
            }
            adapter.registerAdapterDataObserver(observer)
            slots.add(Slot(start + index, adapter, observer))
            adapter.load()
        }
        rebuild()
    }

    private fun rebuild() {
        if (destroyed) return
        val visible = slots.filter { it.adapter.itemCount > 0 }
        rows = FeedAdLayout.rows(content.itemCount, visible.map { it.anchor }).map {
            when (it) {
                is FeedAdRow.Content -> Row.Content(it.index)
                is FeedAdRow.Ad -> Row.Ad(visible[it.slotIndex].adapter)
            }
        }
        notifyDataSetChanged()
    }

    override fun getItemCount() = rows.size
    override fun getItemViewType(position: Int) = when (val row = rows[position]) {
        is Row.Ad -> Int.MIN_VALUE
        is Row.Content -> content.getItemViewType(row.index)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == Int.MIN_VALUE) NativeAdAdapter.Holder(com.anythink.nativead.api.ATNativeAdView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }) else content.onCreateViewHolder(parent, viewType)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Content -> content.onBindViewHolder(holder, row.index)
            is Row.Ad -> {
                boundAds.put(holder, row.adapter)?.takeIf { it !== row.adapter }?.onViewRecycled(holder as NativeAdAdapter.Holder)
                row.adapter.onBindViewHolder(holder as NativeAdAdapter.Holder, 0)
            }
        }
    }
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        val ad = boundAds.remove(holder)
        if (holder is NativeAdAdapter.Holder) ad?.onViewRecycled(holder) else content.onViewRecycled(holder)
    }

    private fun releaseSlots() {
        slots.forEach { it.adapter.unregisterAdapterDataObserver(it.observer); it.adapter.destroy() }
        slots.clear()
        boundAds.clear()
    }
    fun destroy() {
        if (destroyed) return
        destroyed = true
        owner.lifecycle.removeObserver(this)
        content.unregisterAdapterDataObserver(contentObserver)
        releaseSlots()
        rows = emptyList()
    }
    override fun onDestroy(owner: LifecycleOwner) = destroy()
}
