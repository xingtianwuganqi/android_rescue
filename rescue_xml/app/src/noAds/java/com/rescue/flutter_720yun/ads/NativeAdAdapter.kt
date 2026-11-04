package com.rescue.flutter_720yun.ads

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView

class NativeAdAdapter(activity: Activity, owner: LifecycleOwner, placementId: String = "") :
    RecyclerView.Adapter<NativeAdAdapter.Holder>() {
    class Holder(view: FrameLayout) : RecyclerView.ViewHolder(view)
    companion object {
        fun createHolder(parent: ViewGroup) = Holder(FrameLayout(parent.context))
    }
    override fun getItemCount() = 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createHolder(parent)
    override fun onBindViewHolder(holder: Holder, position: Int) = Unit
    fun load() = Unit
    fun destroy() = Unit
}
