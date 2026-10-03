package com.rescue.flutter_720yun.message.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.message.activity.MessageSystemListActivity
import com.rescue.flutter_720yun.message.adapter.MessageListAdapter
import com.rescue.flutter_720yun.message.adapter.MessageListItemClickListener
import com.rescue.flutter_720yun.databinding.FragmentMessageBinding
import com.rescue.flutter_720yun.home.models.LoginEvent
import com.rescue.flutter_720yun.message.activity.MessageSingleActivity
import com.rescue.flutter_720yun.message.viewmodels.MessageViewModel
import com.rescue.flutter_720yun.util.lazyLogin
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MessageFragment : Fragment(), MessageListItemClickListener {
    private var rootView : View? = null
    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MessageListAdapter

    private val messageViewModel by lazy {
        ViewModelProvider(requireActivity())[MessageViewModel::class.java]
    }

    private var messageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val resultData = data?.getStringExtra("message_result")
            // 更新result
            if (resultData == "1") {
                messageViewModel.unreadMessageNumberNetworking()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLoginEvent(event: LoginEvent) {
        messageViewModel.unreadMessageNumberNetworking()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = MessageListAdapter(mutableListOf())
        binding.messageList.adapter = adapter
        binding.messageList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter.setClickListener(this)

        messageViewModel.messageList.observe(viewLifecycleOwner) {
            adapter.reloadList(it)
        }
    }

    override fun itemClick(position: Int) {
        val category=messageViewModel.messageList.value?.getOrNull(position)?.category ?: return
        if(category == "system") {
            startActivity(Intent(activity, MessageSystemListActivity::class.java)); return
        }
        if(category in listOf("my_applications", "received_applications")) {
            val target=if(category=="my_applications") com.rescue.flutter_720yun.adoption.activity.AdoptionMyApplicationsActivity::class.java
                else com.rescue.flutter_720yun.adoption.activity.AdoptionApplicationsActivity::class.java
            startActivity(Intent(requireContext(),target)); return
        }
        val oldType=mapOf("like" to 1,"collection" to 2,"comment" to 3)[category] ?: return
        lazyLogin(requireActivity()) {
            messageLauncher.launch(Intent(activity, MessageSingleActivity::class.java).putExtra("messageType",oldType))
        }
    }
    override fun onResume() { super.onResume(); messageViewModel.unreadMessageNumberNetworking() }
    override fun onDestroy() { EventBus.getDefault().unregister(this); super.onDestroy() }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }
}