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
        vm.application.observe(viewLifecycleOwner) { if(it!=null) render(it) else root?.removeAllViews() }
        vm.busy.observe(viewLifecycleOwner) { busy -> root?.let { r -> for(i in 0 until r.childCount) r.getChildAt(i).isEnabled=busy!=true } }
        vm.error.observe(viewLifecycleOwner) { e -> if(e!=null) {
            requireContext().notice(e.message)
            if(e.http in listOf(401,403,404)) dismiss() else if(e.http==409) vm.load(applicationId)
        } }
        UserManager.sessionRevision.observe(viewLifecycleOwner) { if(!UserManager.isLogin || it != initialSession) dismiss() }
        vm.load(applicationId)
    }
    private fun render(app:AdoptionApplication) {
        val r=root ?: return;r.removeAllViews()
        r.label("${app.applicant?.nickname ?: "账号或资料已不可用"}\n${AdoptionLabels.status(app.status,app.result)}")
        if(app.topic?.unavailable==true || app.topic?.is_delete==0) { r.label("帖子已删除，不能操作");return }
        r.label("申请时资料 · 用户填写\n${AdoptionLabels.summary(app.profile_snapshot)}")
        r.label("养宠经验：${app.profile_snapshot?.pet_experience.orEmpty()}\n申请说明：${app.statement.orEmpty()}\n结束说明：${app.end_note.orEmpty()}")
        if(app.topic?.unavailable==true || app.topic?.is_delete==0) { r.label("帖子已删除，不能操作");return }
        if(!ownerHint) {
            val contact = AdoptionContact.visible(app.topic)
            r.label(if(contact != null) "送养人联系方式：$contact" else "暂无可显示的联系方式")
            if(contact != null) r.button("复制联系方式") { confirmation = AdoptionContactCopy.prompt(requireActivity(), vm, applicationId) { vm.application.value=it } }
        }
        if(ownerHint && vm.isOpen && app.profile_snapshot != null && app.applicant != null && app.status in listOf("applying","communicating")) {
            if(app.status == "applying") r.button("同意沟通") {
                confirm("同意沟通？","你的本帖联系方式将向这位申请人开放。") {
                    vm.run { vm.repository.action(applicationId,ApplicationAction("communicate",app.version));changed() }
                }
            }
            if(app.status=="communicating") r.button("选为实际接宠人，完成送养") {
                confirm("确认完成送养？","本申请将记为完成，其他有效申请结束为未完成。结果为送养人报告。") {
                    vm.run {
                        val topic=app.topic?.topic_id ?: return@run;val state=vm.repository.state(topic)
                        if(!state.can_manage) throw com.rescue.flutter_720yun.adoption.repository.AdoptionError(403,message="无管理权限")
                        vm.repository.topicAction(topic,TopicAction("complete",true,state.workflow_version ?: return@run,applicationId));changed()
                    }
                }
            }
        }
        if(app.status in listOf("applying","communicating")) r.button(if(ownerHint) "结束申请" else "放弃申请") {
            val box=LinearLayout(requireContext()).apply { orientation=LinearLayout.VERTICAL;setPadding(24,12,24,12) }
            val note=box.field("结束说明（选填，最多200字）")
            confirmation=AlertDialog.Builder(requireContext()).setTitle("确认结束本份申请？").setView(box)
                .setPositiveButton("确认结束") { _,_ -> vm.run { vm.repository.action(applicationId,ApplicationAction("end",app.version,note.text.toString().trim()));changed() } }
                .setNegativeButton("取消",null).show()
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
