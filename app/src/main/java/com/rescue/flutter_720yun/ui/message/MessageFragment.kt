package com.rescue.flutter_720yun.ui.message

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rescue.flutter_720yun.message.activity.MessageSystemListActivity
import com.rescue.flutter_720yun.home.adapter.MessageListAdapter
import com.rescue.flutter_720yun.home.adapter.MessageListItemClickListener
import com.rescue.flutter_720yun.databinding.FragmentMessageBinding

class MessageFragment : Fragment(), MessageListItemClickListener {

    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val messageViewModel =
            ViewModelProvider(this)[MessageViewModel::class.java]
        _binding = FragmentMessageBinding.inflate(inflater, container, false)
        val root: View = binding.root

        messageViewModel.messageList.observe(viewLifecycleOwner) {
            val adapter = MessageListAdapter(it)
            binding.messageList.adapter = adapter
            binding.messageList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter.setClickListener(this)

        }

        return root
    }

    override fun itemClick(position: Int) {
        if (position == 0) { // 系统消息
            val intent = Intent(activity, MessageSystemListActivity::class.java)
            startActivity(intent)
        }else if (position == 1) {

        }else if (position == 2) {

        }else if (position == 3) {

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}