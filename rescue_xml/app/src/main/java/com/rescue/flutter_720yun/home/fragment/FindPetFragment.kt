package com.rescue.flutter_720yun.home.fragment

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.ads.NativeFeedAdapter
import com.rescue.flutter_720yun.ads.FeedAdPlacement
import com.rescue.flutter_720yun.databinding.FragmentFindPetBinding
import com.rescue.flutter_720yun.findpet.FindPetChanges
import com.rescue.flutter_720yun.home.activity.FindPetDetailActivity
import com.rescue.flutter_720yun.home.activity.LoginActivity
import com.rescue.flutter_720yun.home.activity.TopicReportActivity
import com.rescue.flutter_720yun.home.adapter.FindPetItemClickListener
import com.rescue.flutter_720yun.home.adapter.FindPetListAdapter
import com.rescue.flutter_720yun.home.models.FindPetModel
import com.rescue.flutter_720yun.home.viewmodels.FindPetViewModel
import com.rescue.flutter_720yun.message.activity.CommentListActivity
import com.rescue.flutter_720yun.util.*

class FindPetFragment : Fragment(), FindPetItemClickListener {
    private var _binding: FragmentFindPetBinding?=null
    private val binding get()=_binding!!
    private val vm by lazy { ViewModelProvider(this)[FindPetViewModel::class.java] }
    private var nativeFeed: NativeFeedAdapter?=null
    private lateinit var adapter: FindPetListAdapter
    private var navigationPending=false
    private var dialog: AlertDialog?=null
    private val editor=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        navigationPending=false
        if(it.resultCode==Activity.RESULT_OK) vm.findPetListNetworking(RefreshState.REFRESH)
    }
    private val login=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if(UserManager.isLogin) vm.findPetListNetworking(RefreshState.REFRESH)
    }
    private val comments=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { vm.findPetListNetworking(RefreshState.REFRESH) }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding=FragmentFindPetBinding.inflate(inflater,container,false);return binding.root
    }
    override fun onViewCreated(view: View,state: Bundle?) {
        if(resources.configuration.fontScale>1.25f && resources.configuration.screenWidthDp<400) {
            val constraints=androidx.constraintlayout.widget.ConstraintSet()
            constraints.clone(binding.backImage)
            constraints.clear(R.id.find_header_text,androidx.constraintlayout.widget.ConstraintSet.BOTTOM)
            constraints.connect(R.id.find_header_text,androidx.constraintlayout.widget.ConstraintSet.END,androidx.constraintlayout.widget.ConstraintSet.PARENT_ID,androidx.constraintlayout.widget.ConstraintSet.END,(20*resources.displayMetrics.density).toInt())
            constraints.clear(R.id.find_button,androidx.constraintlayout.widget.ConstraintSet.TOP)
            constraints.connect(R.id.find_button,androidx.constraintlayout.widget.ConstraintSet.TOP,R.id.find_header_text,androidx.constraintlayout.widget.ConstraintSet.BOTTOM,(12*resources.displayMetrics.density).toInt())
            constraints.constrainWidth(R.id.find_button,(150*resources.displayMetrics.density).toInt())
            constraints.applyTo(binding.backImage)
        }
        vm.collections=arguments?.getBoolean("collections")==true
        adapter=FindPetListAdapter(mutableListOf(),this)
        binding.findList.layoutManager=LinearLayoutManager(requireContext())
        nativeFeed=NativeFeedAdapter(requireActivity(),viewLifecycleOwner,adapter,FeedAdPlacement.FIND_PET)
        binding.findList.adapter=nativeFeed
        val manage=View.OnClickListener { if(!navigationPending) { com.rescue.flutter_720yun.analytics.AnalyticsManager.publishClick("find_pet");if(!UserManager.isLogin) com.rescue.flutter_720yun.analytics.AnalyticsManager.loginClick();navigationPending=true;editor.launch(Intent(requireContext(),FindPetDetailActivity::class.java)) } }
        binding.backImage.setOnClickListener(manage);binding.findButton.setOnClickListener(manage)
        binding.refreshLayout.setOnRefreshListener { vm.findPetListNetworking(RefreshState.REFRESH) }
        binding.refreshLayout.setOnLoadMoreListener { vm.findPetListNetworking(RefreshState.MORE) }
        binding.errorView.setOnClickListener { if(!vm.collections || UserManager.isLogin) vm.findPetListNetworking(RefreshState.REFRESH) else login.launch(Intent(requireContext(),LoginActivity::class.java)) }
        vm.uiState.observe(viewLifecycleOwner) { ui ->
            val empty=vm.items.value.isNullOrEmpty()
            binding.errorView.isVisible=ui is UiState.Error || (ui is UiState.Success && empty)
            binding.errorView.text=if(ui is UiState.Error) ui.message else "暂无数据\n请点击重试"
            binding.progressBar.isVisible=ui is UiState.FirstLoading && empty && !binding.refreshLayout.isRefreshing
            if(ui is UiState.Success) adapter.refreshItem(ui.data)
        }
        vm.isLoading.observe(viewLifecycleOwner) { busy ->
            if(!busy) {
                binding.progressBar.isVisible=false
                binding.refreshLayout.finishRefresh(200);binding.refreshLayout.finishLoadMore(200)
                binding.refreshLayout.setNoMoreData(vm.isLastPage.value==true)
            }
            binding.refreshLayout.setEnableLoadMore(!vm.items.value.isNullOrEmpty())
        }
        vm.operations.observe(viewLifecycleOwner) { adapter.setBusy(it) }
        vm.pageChange.observe(viewLifecycleOwner) { event -> if(event!=null) {
            nativeFeed?.pageLoaded(event.added,event.refresh);vm.pageChange.value=null
        } }
        vm.errorMsg.observe(viewLifecycleOwner) { if(it!=null) { it.toastString();vm.errorMsg.value=null } }
        vm.actionError.observe(viewLifecycleOwner) { error -> if(error!=null) {
            vm.actionError.value=null
            when(error.status) {
                401 -> login.launch(Intent(requireContext(),LoginActivity::class.java))
                209,210 -> { error.message.toastString();login.launch(Intent(requireContext(),LoginActivity::class.java).putExtra("type",if(error.status==209) "bindPhone" else "checkPhone")) }
                else -> error.message.toastString()
            }
        } }
        vm.copiedContact.observe(viewLifecycleOwner) { value -> if(value!=null) {
            vm.copiedContact.value=null
            (requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("找宠联系方式",value))
            "已复制".toastString()
        } }
        var wasLoggedIn=UserManager.isLogin
        UserManager.sessionRevision.observe(viewLifecycleOwner) {
            val expired=wasLoggedIn && !UserManager.isLogin;wasLoggedIn=UserManager.isLogin
            if(expired && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) login.launch(Intent(requireContext(),LoginActivity::class.java))
        }
        FindPetChanges.requestRevision.observe(viewLifecycleOwner) { if(vm.isLoading.value!=true && (!vm.collections || UserManager.isLogin)) vm.findPetListNetworking(RefreshState.REFRESH) }
    }
    private fun authenticated(model: FindPetModel?, action: (FindPetModel)->Unit) {
        if((model?.findId ?: 0)<=0) return
        if(UserManager.isLogin) action(model!!) else { com.rescue.flutter_720yun.analytics.AnalyticsManager.loginClick();login.launch(Intent(requireContext(),LoginActivity::class.java)) }
    }
    override fun userClick(model: FindPetModel?) = Unit
    override fun likeActionClick(model: FindPetModel?)=authenticated(model) { vm.interact(it,false) }
    override fun collectionClick(model: FindPetModel?)=authenticated(model) { vm.interact(it,true) }
    override fun getContactClick(model: FindPetModel?)=authenticated(model) { vm.getFindPetContactNetworking(it) }
    override fun commentClick(model: FindPetModel?)=authenticated(model) {
        comments.launch(Intent(requireContext(),CommentListActivity::class.java).putExtra("topicId",it.findId!!).putExtra("topicType",3).putExtra("toUid",it.user_id ?: 0))
    }
    override fun moreButtonClick(model: FindPetModel?)=authenticated(model) { item ->
        val own=item.user_id==UserManager.userId
        val labels=if(own) arrayOf("管理找宠资料","取消") else arrayOf("屏蔽此找宠信息","投诉举报","取消")
        dialog=AlertDialog.Builder(requireContext()).setItems(labels) { _,index ->
            if(own) { if(index==0) editor.launch(Intent(requireContext(),FindPetDetailActivity::class.java)) }
            else if(index==0) {
                dialog=AlertDialog.Builder(requireContext()).setMessage(R.string.more_black_desc).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.confirm_d) { _,_ ->
                    SharedPreferencesUtil.putStringSet("find_pet_list",SharedPreferencesUtil.getStringSet("find_pet_list",requireContext()).orEmpty()+item.findId.toString(),requireContext())
                    vm.hidden(item.findId!!)
                }.show()
            } else if(index==1) startActivity(Intent(requireContext(),TopicReportActivity::class.java).putExtra("reportType",7).putExtra("reportId",item.findId!!))
        }.show()
    }
    override fun onResume() { super.onResume();if(_binding!=null && vm.isLoading.value!=true && (!vm.collections || UserManager.isLogin)) vm.findPetListNetworking(RefreshState.REFRESH) }
    override fun onPause() { dialog?.dismiss();vm.stop();super.onPause() }
    override fun onDestroyView() {
        vm.stop();nativeFeed?.destroy();nativeFeed=null;_binding?.findList?.adapter=null;_binding=null;super.onDestroyView()
    }
    companion object { fun collections()=FindPetFragment().apply { arguments=Bundle().apply { putBoolean("collections",true) } } }
}
