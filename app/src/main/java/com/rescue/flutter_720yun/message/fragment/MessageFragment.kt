package com.rescue.flutter_720yun.message.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.message.activity.MessageSystemListActivity
import com.rescue.flutter_720yun.message.adapter.MessageListAdapter
import com.rescue.flutter_720yun.message.adapter.MessageListItemClickListener
import com.rescue.flutter_720yun.databinding.FragmentMessageBinding
import com.rescue.flutter_720yun.message.activity.MessageSingleActivity
import com.rescue.flutter_720yun.message.viewmodels.MessageViewModel

class MessageFragment : Fragment(), MessageListItemClickListener {
    private var rootView : View? = null
    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!


    var messageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val resultData = data?.getStringExtra("message_result")
            // 更新result
            if (resultData == "1") {

            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val messageViewModel =
            ViewModelProvider(this)[MessageViewModel::class.java]
        _binding = FragmentMessageBinding.inflate(inflater, container, false)
        if (rootView == null) {
            rootView = binding.root
        }
        messageViewModel.messageList.observe(viewLifecycleOwner) {
            val adapter = MessageListAdapter(it)
            binding.messageList.adapter = adapter
            binding.messageList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter.setClickListener(this)
        }
        Log.d("TAG","Message Fragment onCreateView networking")
        return rootView
    }

    override fun itemClick(position: Int) {
        if (position == 0) { // 系统消息
            val intent = Intent(activity, MessageSystemListActivity::class.java)
            startActivity(intent)
        }else {
            val intent = Intent(activity, MessageSingleActivity::class.java)
            intent.putExtra("messageType", position)
            messageLauncher.launch(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}