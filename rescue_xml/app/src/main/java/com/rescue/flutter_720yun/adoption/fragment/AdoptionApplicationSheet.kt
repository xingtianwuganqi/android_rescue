package com.rescue.flutter_720yun.adoption.fragment
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rescue.flutter_720yun.adoption.models.*
import com.rescue.flutter_720yun.adoption.viewmodels.AdoptionViewModel
import com.rescue.flutter_720yun.adoption.ui.*
import com.rescue.flutter_720yun.util.UserManager
class ApplicationSheetViewModel:AdoptionViewModel() {
    val application=androidx.lifecycle.MutableLiveData<AdoptionApplication?>()
    var canManage = false
    var isOpen = false
    suspend fun reload(id: Int) {
        application.value = null; canManage = false; isOpen = false
        val app = repository.application(id)
        val topic = app.topic
        if(topic != null && !topic.unavailable && topic.is_delete != 0) {
            val state = repository.state(topic.topic_id); canManage = state.can_manage; isOpen = state.workflow_status == "open"
        }
        application.value = app
    }
    fun load(id:Int)=run { reload(id) }
    override fun clearPrivateState() { application.value=null; canManage=false }
}
class AdoptionApplicationSheet:BottomSheetDialogFragment() {
    private val vm by lazy { ViewModelProvider(this)[ApplicationSheetViewModel::class.java] }
    private var root:LinearLayout?=null
    private var confirmation:AlertDialog?=null
    private val applicationId get()=arguments?.getInt("application_id",0) ?: 0
    private val ownerHint get()=vm.canManage
    private val initialSession = UserManager.sessionRevision.value
    override fun onCreateView(inflater:LayoutInflater,container:ViewGroup?,state:Bundle?):View {
        val scroll=ScrollView(requireContext()); root=LinearLayout(requireContext()).apply { orientation=LinearLayout.VERTICAL;setPadding(24,20,24,24);isSaveEnabled=false }
        scroll.addView(root);return scroll
    }
    override fun onViewCreated(view:View,state:Bundle?) {
        if(applicationId<=0 || !UserManager.isLogin) { dismiss();return }
        vm.application.observe(viewLifecycleOwner) { renderState() }
        vm.busy.observe(viewLifecycleOwner) { renderState() }
        vm.error.observe(viewLifecycleOwner) { e -> if(e!=null) {
            requireContext().notice(e.message)
            if(e.status in listOf(401,403,404)) dismiss() else renderState()
        } else renderState() }
        UserManager.sessionRevision.observe(viewLifecycleOwner) { if(!UserManager.isLogin || it != initialSession) dismiss() }
        vm.load(applicationId)
    }
    private fun renderState() {
        val r = root ?: return
        val error = vm.error.value
        val busy = vm.busy.value == true
        val app = vm.application.value
        when {
            error != null -> {
                r.removeAllViews()
                r.label(error.message)
                r.button("重新加载") { vm.load(applicationId) }.isEnabled = !busy
            }
            app != null -> render(app)
            else -> {
                r.removeAllViews()
                r.label(if(busy) "正在加载申请资料…" else "暂无申请资料")
                if(!busy) r.button("重新加载") { vm.load(applicationId) }
            }
        }
        for(i in 0 until r.childCount) r.getChildAt(i).isEnabled = !busy
    }
    private fun render(app:AdoptionApplication) {
        val r=root ?: return;r.removeAllViews()
        r.label("${app.applicant?.nickname ?: "账号或资料已不可用"}\n${AdoptionOperations.status(app)}")
        if(app.topic?.unavailable==true || app.topic?.is_delete==0) { r.label("帖子已删除，不能操作");return }
        r.label("申请时资料 · 用户填写\n${AdoptionLabels.summary(app.profile_snapshot)}")
        r.label("养宠经验：${app.profile_snapshot?.pet_experience.orEmpty()}\n申请说明：${app.statement.orEmpty()}\n结束说明：${app.end_note.orEmpty()}")
        if(app.topic?.unavailable==true || app.topic?.is_delete==0) { r.label("帖子已删除，不能操作");return }
        if(!ownerHint) {
            val contact = AdoptionContact.visible(app.topic)
            r.label(if(contact != null) "送养人联系方式：$contact" else "暂无可显示的联系方式")
            if(contact != null) r.button("复制联系方式") { AdoptionContactCopy.copy(requireActivity(), vm, applicationId) { vm.application.value=it } }
        }
    }

    private suspend fun changed() { vm.reload(applicationId);setFragmentResult("adoption_changed",Bundle()) }
    private fun confirm(title:String,message:String,action:()->Unit) { confirmation=AlertDialog.Builder(requireContext()).setTitle(title).setMessage(message)
        .setPositiveButton("确认") { _,_ -> action() }.setNegativeButton("取消",null).show() }
    override fun onResume() { super.onResume(); if(UserManager.isLogin && vm.busy.value != true) vm.load(applicationId) }
    override fun onPause() { confirmation?.dismiss(); confirmation=null; vm.cancelWork(); vm.application.value=null; root?.removeAllViews(); super.onPause() }
    override fun onDestroyView() { confirmation?.dismiss();confirmation=null;root?.removeAllViews();root=null;super.onDestroyView() }
    companion object { fun newInstance(id:Int,owner:Boolean)=AdoptionApplicationSheet().apply { arguments=Bundle().apply { putInt("application_id",id);putBoolean("owner",owner) } } }
}
