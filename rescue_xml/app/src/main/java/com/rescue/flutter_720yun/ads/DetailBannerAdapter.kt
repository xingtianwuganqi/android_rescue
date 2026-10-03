package com.rescue.flutter_720yun.ads

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView

/** A collapsible banner between detail text and images. */
class DetailBannerAdapter(activity: Activity, owner: LifecycleOwner) :
    RecyclerView.Adapter<DetailBannerAdapter.Holder>(), DefaultLifecycleObserver {
    private val container = FrameLayout(activity).apply {
        visibility = View.GONE
        val margin = (15 * resources.displayMetrics.density).toInt()
        setPadding(margin, 0, margin, 0)
        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    private val controller = BannerAdController(activity, container)
    init { owner.lifecycle.addObserver(this); controller.load() }
    class Holder(view: View) : RecyclerView.ViewHolder(view)
    override fun getItemCount() = 1
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        (container.parent as? ViewGroup)?.removeView(container)
        return Holder(container)
    }
    override fun onBindViewHolder(holder: Holder, position: Int) = Unit
    override fun onDestroy(owner: LifecycleOwner) { controller.destroy() }
}
