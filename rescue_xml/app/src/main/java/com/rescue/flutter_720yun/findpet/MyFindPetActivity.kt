package com.rescue.flutter_720yun.findpet

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adoption.activity.AdoptionActivity
import com.rescue.flutter_720yun.adoption.ui.notice
import com.rescue.flutter_720yun.databinding.ActivityMyFindPetBinding
import com.rescue.flutter_720yun.home.activity.FindPetDetailActivity
import com.rescue.flutter_720yun.home.activity.HomeDetailActivity
import com.rescue.flutter_720yun.util.UserManager

class MyFindPetActivity : AdoptionActivity() {
    override val contentLayoutRes get() = R.layout.activity_adoption_applications
    private val vm by lazy { ViewModelProvider(this)[MyFindPetViewModel::class.java] }
    private var binding: ActivityMyFindPetBinding? = null
    private val adapter = RecommendationAdapter { vm.open(it) }
    private var routeComplete = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        routeComplete = savedInstanceState?.getBoolean("route_complete") ?: false
        setupToolbar("我的找宠")
        render(); observe(vm, false)
        vm.items.observe(this) { update() }; vm.state.observe(this) { update() }
        vm.busy.observe(this) { update() }; vm.error.observe(this) { update() }
        vm.destination.observe(this) { destination -> if (destination != null) {
            vm.destination.value = null; routeComplete = true
            if (destination.available) startActivity(Intent(this, HomeDetailActivity::class.java).putExtra("topic_id", destination.topicId))
            else { notice("帖子已删除或不可用"); binding?.root?.post { if (!isFinishing && UserManager.isLogin) vm.load() } }
        } }
        requireLogin()
    }
    private fun render() {
        content.removeAllViews()
        val b = ActivityMyFindPetBinding.inflate(layoutInflater, content, true); binding = b
        b.findPetRecommendations.layoutManager = LinearLayoutManager(this)
        b.findPetRecommendations.adapter = adapter
        b.findPetManage.setOnClickListener { startActivity(Intent(this, FindPetDetailActivity::class.java)) }
        b.findPetRefresh.setOnRefreshListener { vm.load() }
        b.findPetRefresh.setOnLoadMoreListener { vm.load(false) }
        b.findPetError.setOnClickListener { loadOrOpen() }
        b.findPetEmpty.setOnClickListener {
            loadOrOpen()
        }
        update()
    }
    private fun update() {
        val b = binding?.takeIf { it.root.parent == content } ?: return
        val busy = vm.busy.value == true
        val empty = vm.items.value.isNullOrEmpty()
        val state = vm.state.value
        adapter.submit(vm.items.value.orEmpty(), busy)
        val restricted = vm.error.value?.status == 403
        val failed = vm.error.value != null && vm.error.value?.status !in listOf(401,403)
        b.findPetState.text = when {
            restricted -> "账号受限，暂时无法查看找宠推荐。"
            failed -> if (empty) "同城推荐暂时无法加载。" else "推荐加载失败，可重试；已有推荐仍可查看。"
            busy && empty -> "正在加载同城推荐…"
            state == "no_request" -> "发布找宠信息，接收同城送养推荐。"
            state == "closed" -> "找宠已关闭。开启后可继续接收同城推荐，历史推荐仍可查看。"
            else -> if (empty) "暂无同城推荐，下拉刷新试试。" else "为你推荐同城送养信息，点击查看详情。"
        }
        b.findPetManage.text = when (state) { "no_request" -> "发布找宠信息"; "closed" -> "开启找宠信息"; else -> "管理找宠信息" }
        b.findPetManage.isVisible = !state.isNullOrBlank()
        b.findPetManage.isEnabled = !busy && !restricted
        b.findPetManage.alpha = if (busy || restricted) 0.5f else 1f
        b.findPetLoading.isVisible = busy && empty && !b.findPetRefresh.isRefreshing
        b.findPetError.isVisible = false
        b.findPetError.text = vm.error.value?.message.orEmpty() + "\n点击重试"
        b.findPetEmpty.isVisible = !busy && empty
        b.findPetEmpty.translationY=-resources.displayMetrics.heightPixels*.1f
        b.findPetEmpty.text = when {
            vm.error.value!=null -> if(vm.error.value!!.status in listOf(401,403,404)) vm.error.value!!.message+"\n请点击重试" else "网络加载失败\n请检查网络后重试"
            state=="no_request" -> "尚未发布找宠信息，请通过上方入口发布\n请点击重试"
            state=="closed" -> "找宠已关闭，暂无历史推荐\n请点击重试"
            state=="open" -> "暂无同城推荐\n下拉刷新或点击重试"
            else -> "暂无数据\n请点击重试"
        }
        b.findPetRefresh.setEnableLoadMore(!empty)
        if (!busy) { b.findPetRefresh.finishRefresh(200); b.findPetRefresh.finishLoadMore(200);b.findPetRefresh.setNoMoreData(!vm.hasMore) }
    }
    private fun loadOrOpen() {
        val id = intent.getIntExtra("recommendation_id", 0)
        if (id > 0 && !routeComplete) vm.open(id, intent.getIntExtra("topic_id", 0)) else vm.load()
    }
    override fun onAuthenticated() {
        if (binding?.root?.parent != content) render()
        loadOrOpen()
    }
    override fun onResume() {
        super.onResume()
        if (UserManager.isLogin && vm.busy.value != true) loadOrOpen()
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putBoolean("route_complete", routeComplete); super.onSaveInstanceState(outState) }
    override fun onPause() { if (vm.reading) vm.cancelWork(); super.onPause() }
    override fun onDestroy() { binding?.findPetRecommendations?.adapter = null; binding = null; super.onDestroy() }
}
