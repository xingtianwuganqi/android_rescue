package com.rescue.flutter_720yun.adoption.activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import com.rescue.flutter_720yun.adoption.adapter.AdoptionReminderAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adoption.adapter.AdoptionApplicationAdapter
import com.rescue.flutter_720yun.adoption.fragment.AdoptionApplicationSheet
import com.rescue.flutter_720yun.adoption.ui.notice
import com.rescue.flutter_720yun.adoption.viewmodels.AdoptionApplicationsViewModel
import com.rescue.flutter_720yun.home.activity.HomeDetailActivity

open class AdoptionApplicationsActivity : AdoptionActivity() {
    protected open val isMyApplications = false
    private val vm: AdoptionApplicationsViewModel by lazy {
        if(isMyApplications) ViewModelProvider(this)[com.rescue.flutter_720yun.adoption.viewmodels.AdoptionMyApplicationsViewModel::class.java]
        else ViewModelProvider(this)[AdoptionApplicationsViewModel::class.java]
    }
    private val actionFlow by lazy { com.rescue.flutter_720yun.adoption.ui.AdoptionActionFlow(this,vm,isMyApplications) { refreshLists() } }
    private var contactDialog: androidx.appcompat.app.AlertDialog? = null
    private lateinit var reminderAdapter: AdoptionReminderAdapter
    private var observing=false
    private lateinit var adapter: AdoptionApplicationAdapter
    private var focused=false
    override fun onCreate(state: Bundle?) {
        super.onCreate(state); focused = state?.getBoolean("focused") ?: false; setupToolbar(if(isMyApplications) "我的申请" else "收到的申请")
        val topic=intent.getIntExtra("topic_id",0)
        if(intent.hasExtra("topic_id") && topic<=0) { notice("帖子参数无效"); finish(); return }
        vm.topic=topic.takeIf { it>0 }
        com.rescue.flutter_720yun.util.UserManager.sessionRevision.observe(this) { actionFlow.dismiss();contactDialog?.dismiss();contactDialog=null }
        setupList(); observe(vm, false); observeLists(); requireLogin()
    }
    private fun setupList() {
        content.removeAllViews()
        layoutInflater.inflate(R.layout.adoption_list_content,content,true)
        val filters=findViewById<LinearLayout>(R.id.adoption_filters)
        val options = (if(isMyApplications) listOf("" to "全部") else emptyList()) +
            listOf("applying" to "申请中", "communicating" to "沟通中", "ended" to "已结束")
        for((code,label) in options) {
            filters.addView(Button(this).apply { text=label; tag=code; isSelected=code==vm.status; isEnabled=code!=vm.status; setOnClickListener { vm.status=code; for(i in 0 until filters.childCount) { val child=filters.getChildAt(i); child.isSelected=child.tag==code; child.isEnabled=child.tag!=code }; vm.load() } },LinearLayout.LayoutParams(0,-2,1f))
        }
        adapter=AdoptionApplicationAdapter({ app -> AdoptionApplicationSheet.newInstance(app.application_id,!isMyApplications)
            .show(supportFragmentManager,"application") },{ app -> app.topic?.takeUnless { it.unavailable || it.is_delete==0 }?.let {
                startActivity(Intent(this,HomeDetailActivity::class.java).putExtra("topic_id",it.topic_id)) } },
            copyContact=if(isMyApplications) { app -> com.rescue.flutter_720yun.adoption.ui.AdoptionContactCopy.copy(this,vm,app.application_id) { fresh -> vm.items.value=vm.items.value.orEmpty().map { if(it.application_id==fresh.application_id) fresh else it } } } else null, action={ app,code -> actionFlow.perform(app,code) }, busy={ vm.busy.value==true },previews={ vm.previewImages.value.orEmpty() },missingPreview={ vm.loadPreview(it) })
        findViewById<RecyclerView>(R.id.adoption_list).apply { layoutManager=LinearLayoutManager(this@AdoptionApplicationsActivity); adapter=this@AdoptionApplicationsActivity.adapter }
        findViewById<Button>(R.id.adoption_refresh).setOnClickListener { refreshLists() }
        findViewById<Button>(R.id.adoption_more).setOnClickListener { vm.load(false) }
        reminderAdapter=AdoptionReminderAdapter { vm.readReminder(it) }
        findViewById<RecyclerView>(R.id.adoption_reminder_list).apply {
            layoutManager=LinearLayoutManager(this@AdoptionApplicationsActivity); adapter=reminderAdapter
        }
        findViewById<Button>(R.id.adoption_reminder_retry).setOnClickListener { vm.retryReminders() }
        findViewById<Button>(R.id.adoption_reminder_more).setOnClickListener { vm.loadReminders(false) }
        findViewById<TextView>(R.id.adoption_list_empty).setOnClickListener { vm.load() }
        findViewById<TextView>(R.id.adoption_list_error).setOnClickListener { vm.retryApplications() }
        supportFragmentManager.setFragmentResultListener("adoption_changed",this) { _,_ -> refreshLists() }
        updateLists()
    }
    private fun observeLists() {
        if(observing) return; observing=true
        vm.previewImages.observe(this) { updateLists() }
        vm.items.observe(this) { updateLists() }
        vm.busy.observe(this) { updateLists() }
        vm.error.observe(this) { error ->
            updateLists()
            if(error?.status==409) window.decorView.post { if(lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) refreshLists() }
        }
        vm.reminders.observe(this) { updateLists() }
        vm.reminderBusy.observe(this) { updateLists() }
        vm.reminderError.observe(this) { updateLists() }
        vm.reminderCounts.observe(this) { updateLists() }
        vm.readingId.observe(this) { updateLists() }
        vm.readMessage.observe(this) { message ->
            if(message != null && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                vm.readMessage.value=null
                contactDialog=androidx.appcompat.app.AlertDialog.Builder(this).setTitle("申请提醒").setMessage(message).setPositiveButton("知道了",null).show()
            }
        }
    }
    private fun updateLists() {
        if(content.findViewById<RecyclerView>(R.id.adoption_list)==null || !::adapter.isInitialized || !::reminderAdapter.isInitialized) return
        adapter.submit(vm.items.value.orEmpty()); reminderAdapter.submit(vm.reminders.value.orEmpty(),vm.readingId.value)
        val busy=vm.busy.value==true; val empty=vm.items.value.isNullOrEmpty(); val error=vm.error.value
        findViewById<ProgressBar>(R.id.adoption_list_loading).visibility=if(busy && empty) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.adoption_list_empty).visibility=if(!busy && empty && error==null) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.adoption_list_error).apply {
            visibility=if(!busy && error!=null) View.VISIBLE else View.GONE
            text=error?.message.orEmpty()+"\n请点击重试"
        }
        findViewById<Button>(R.id.adoption_more).apply { visibility=if(!empty && vm.hasMore) View.VISIBLE else View.GONE; isEnabled=!busy }
        findViewById<LinearLayout>(R.id.adoption_reminder_container).visibility = if(vm.reminders.value.isNullOrEmpty() && vm.reminderBusy.value != true && vm.reminderError.value == null) View.GONE else View.VISIBLE
        val counts=vm.reminderCounts.value
        val number=if(isMyApplications) counts?.mine_unread_count else counts?.received_unread_count
        findViewById<TextView>(R.id.adoption_reminder_status).text=when {
            vm.reminderBusy.value==true -> "正在加载提醒…"
            number!=null && number>0 -> "未读提醒 $number 条"
            number==0 -> "暂无未读提醒"
            else -> "提醒计数待更新"
        }
        findViewById<ProgressBar>(R.id.adoption_reminder_loading).visibility=if(vm.reminderBusy.value==true) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.adoption_reminder_retry).apply {
            visibility=if(vm.reminderError.value!=null) View.VISIBLE else View.GONE
            text=vm.reminderError.value.orEmpty()+" · 点击重试"
        }
        findViewById<Button>(R.id.adoption_reminder_more).apply {
            visibility=if(vm.reminderHasMore && !vm.reminders.value.isNullOrEmpty()) View.VISIBLE else View.GONE
            isEnabled=vm.reminderBusy.value!=true && vm.readingId.value==null
        }
    }
    private fun refreshLists() { vm.load(); vm.loadReminders() }
    override fun onAuthenticated() {
        if(content.findViewById<RecyclerView>(R.id.adoption_list)==null) setupList()
        refreshLists()
        val id=intent.getIntExtra("application_id",0)
        if(id>0 && !focused) { focused=true; AdoptionApplicationSheet.newInstance(id,!isMyApplications).show(supportFragmentManager,"application") }
    }
    override fun onPause() {
        actionFlow.dismiss(); contactDialog?.dismiss(); contactDialog=null
        vm.stopReminders()
        vm.readMessage.value=null
        if(isMyApplications) { vm.cancelWork(); vm.items.value=emptyList(); vm.initialized=false }
        super.onPause()
    }
    override fun onSaveInstanceState(out: Bundle) { out.putBoolean("focused", focused); super.onSaveInstanceState(out) }
    override fun onResume() {
        super.onResume()
        if(com.rescue.flutter_720yun.util.UserManager.isLogin && ::adapter.isInitialized) {
            if(vm.busy.value!=true) vm.load()
            if(vm.reminderBusy.value!=true) vm.loadReminders()
        }
    }
}
