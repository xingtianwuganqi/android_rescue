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
        setupToolbar(getString(R.string.find_pet_my_title))
        render(); observe(vm, false)
        vm.items.observe(this) { update() }; vm.state.observe(this) { update() }
        vm.busy.observe(this) { update() }; vm.error.observe(this) { update() }
        vm.destination.observe(this) { destination -> if (destination != null) {
            vm.destination.value = null; routeComplete = true
            if (destination.available) startActivity(Intent(this, HomeDetailActivity::class.java).putExtra("topic_id", destination.topicId))
            else { notice(getString(R.string.common_post_deleted)); binding?.root?.post { if (!isFinishing && UserManager.isLogin) vm.load() } }
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
            restricted -> getString(R.string.find_pet_restricted)
            failed -> getString(if (empty) R.string.find_pet_recommend_unavailable else R.string.find_pet_recommend_failed_keep)
            busy && empty -> getString(R.string.find_pet_recommend_loading)
            state == "no_request" -> getString(R.string.find_pet_recommend_prompt)
            state == "closed" -> getString(R.string.find_pet_closed_prompt)
            else -> getString(if (empty) R.string.find_pet_recommend_empty else R.string.find_pet_recommend_desc)
        }
        b.findPetManage.text = when (state) { "no_request" -> getString(R.string.find_pet_publish); "closed" -> getString(R.string.find_pet_open); else -> getString(R.string.find_pet_manage_info) }
        b.findPetManage.isVisible = !state.isNullOrBlank()
        b.findPetManage.isEnabled = !busy && !restricted
        b.findPetManage.alpha = if (busy || restricted) 0.5f else 1f
        b.findPetLoading.isVisible = busy && empty && !b.findPetRefresh.isRefreshing
        b.findPetError.isVisible = false
        b.findPetError.text = vm.error.value?.message.orEmpty() + getString(R.string.common_tap_retry_suffix)
        b.findPetEmpty.isVisible = !busy && empty
        b.findPetEmpty.translationY=-resources.displayMetrics.heightPixels*.1f
        b.findPetEmpty.text = when {
            vm.error.value!=null -> if(vm.error.value!!.status in listOf(401,403,404)) vm.error.value!!.message+getString(R.string.common_tap_retry_suffix) else getString(R.string.common_network_load_failed)
            state=="no_request" -> getString(R.string.find_pet_not_published_empty)
            state=="closed" -> getString(R.string.find_pet_closed_empty)
            state=="open" -> getString(R.string.find_pet_no_recommend)
            else -> getString(R.string.empty_retry)
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
    override fun onDestroy() { binding = null; super.onDestroy() }
}
