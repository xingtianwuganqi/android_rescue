package com.rescue.flutter_720yun.user.activity

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.*
import com.bumptech.glide.Glide
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adoption.activity.AdoptionActivity
import com.rescue.flutter_720yun.adoption.ui.notice
import com.rescue.flutter_720yun.databinding.ActivityMyFindPetBinding
import com.rescue.flutter_720yun.home.activity.HomeDetailActivity
import com.rescue.flutter_720yun.user.viewmodels.*
import com.rescue.flutter_720yun.util.toImgUrl
import com.rescue.flutter_720yun.util.findActivity

class BrowsingHistoryActivity : AdoptionActivity() {
    override val contentLayoutRes get()=R.layout.activity_adoption_applications
    private val vm by lazy { ViewModelProvider(this)[BrowsingHistoryViewModel::class.java] }
    private lateinit var b: ActivityMyFindPetBinding
    private val adapter=HistoryAdapter { row -> if(row.available) startActivity(Intent(this,HomeDetailActivity::class.java).putExtra("topic_id",row.topic)) else notice(getString(R.string.common_post_deleted)) }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state);setupToolbar(getString(R.string.drawer_footer));render();observe(vm,false)
        vm.rows.observe(this) { update() };vm.busy.observe(this) { update() };vm.error.observe(this) { update() }
        requireLogin()
    }
    private fun render() {
        content.removeAllViews();b=ActivityMyFindPetBinding.inflate(layoutInflater,content,true)
        (b.findPetState.parent as View).isVisible=false
        b.findPetRecommendations.layoutManager=LinearLayoutManager(this);b.findPetRecommendations.adapter=adapter
        b.findPetRefresh.setOnRefreshListener { vm.load() };b.findPetRefresh.setOnLoadMoreListener { vm.load(false) }
        b.findPetEmpty.setOnClickListener { vm.load() };update()
    }
    private fun update() {
        if(!::b.isInitialized || b.root.parent!=content) return
        val empty=vm.rows.value.isNullOrEmpty();val busy=vm.busy.value==true
        adapter.rows=vm.rows.value.orEmpty();adapter.notifyDataSetChanged()
        b.findPetLoading.isVisible=busy && empty && !b.findPetRefresh.isRefreshing
        b.findPetEmpty.isVisible=!busy && empty
        b.findPetEmpty.translationY=-resources.displayMetrics.heightPixels*.1f
        b.findPetEmpty.text=if(vm.error.value!=null) getString(R.string.common_network_load_failed) else getString(R.string.empty_retry)
        if(!busy) { b.findPetRefresh.finishRefresh(200,vm.error.value==null,false);b.findPetRefresh.finishLoadMore(200,vm.error.value==null,false);b.findPetRefresh.setNoMoreData(vm.error.value==null && !vm.hasMore) }
        b.findPetRefresh.setEnableLoadMore(!empty)
    }
    override fun onAuthenticated() { if(!::b.isInitialized || b.root.parent!=content) render();vm.load() }
    override fun onDestroy() { super.onDestroy() }
}
private class HistoryAdapter(private val click: (BrowsingRecord)->Unit) : RecyclerView.Adapter<HistoryAdapter.Holder>() {
    var rows=emptyList<BrowsingRecord>()
    class Holder(val row: LinearLayout,val image: ImageView,val text: TextView): RecyclerView.ViewHolder(row)
    override fun getItemCount()=rows.size
    override fun onCreateViewHolder(parent: ViewGroup, type: Int): Holder {
        val context=parent.context;fun dp(v: Int)=(v*context.resources.displayMetrics.density).toInt()
        val row=LinearLayout(context).apply { gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(12),dp(12),dp(12));setBackgroundResource(R.drawable.adoption_card_background)
            layoutParams=RecyclerView.LayoutParams(-1,-2).apply { setMargins(dp(15),dp(6),dp(15),dp(6)) } }
        val image=ImageView(context).apply { scaleType=ImageView.ScaleType.CENTER_CROP };row.addView(image,LinearLayout.LayoutParams(dp(88),dp(88)))
        val text=TextView(context).apply { textSize=15f;setTextColor(android.graphics.Color.DKGRAY);maxLines=5 }
        row.addView(text,LinearLayout.LayoutParams(0,-2,1f).apply { marginStart=dp(12) });return Holder(row,image,text)
    }
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item=rows[position];holder.text.text=if(item.available) listOf(item.content.ifBlank { holder.text.context.getString(R.string.find_pet_recommend_view) },item.address).filter(String::isNotBlank).joinToString("\n") else holder.text.context.getString(R.string.common_post_deleted)
        Glide.with(holder.image).load(if(item.available) item.image?.toImgUrl() else null).placeholder(R.drawable.icon_eee).into(holder.image)
        holder.row.setOnClickListener { click(item) }
    }
    override fun onViewRecycled(holder: Holder) {
        val activity = holder.image.context.findActivity()
        if (activity == null || !activity.isDestroyed) Glide.with(holder.image).clear(holder.image)
    }
}
