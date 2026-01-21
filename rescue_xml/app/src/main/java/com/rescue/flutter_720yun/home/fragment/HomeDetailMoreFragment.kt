package com.rescue.flutter_720yun.home.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.FragmentHomeDetailMoreBinding

class HomeDetailMoreFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentHomeDetailMoreBinding? = null
    private val binding get() = _binding!!
    private var status: Int? = null
    var clickCallBack: ((Int) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val value = it.getInt("status")
            status = value
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeDetailMoreBinding.inflate(inflater,container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("TAG","$status")
        if (status == 0) { // 未完成
            binding.titleText.text = resources.getString(R.string.user_complete_rescue)//"点击完成领养，即代表宠物已经被领养，他人无法获取你的联系方式，确定改成完成领养吗？"
            binding.changeText.text = resources.getString(R.string.user_complete_title)
        }else{ // 已完成
            binding.titleText.text = resources.getString(R.string.user_un_complete)
            binding.changeText.text = resources.getString(R.string.user_un_complete_title)
        }

        binding.changeLinear.setOnClickListener {
            dismiss()
            if (status == 0) { //未完成，要去完成
                clickCallBack?.let { it1 -> it1(1) }
            }else{ // 完成，改成未完成
                clickCallBack?.let { it1 -> it1(0) }
            }
        }

        binding.deleteLinear.setOnClickListener {
            dismiss()
            clickCallBack?.let { it1 -> it1(2) }
        }

        binding.cancel.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeDetailMoreFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(status: Int) =
            HomeDetailMoreFragment().apply {
                arguments = Bundle().apply {
                    putInt("status", status)
                }
            }
    }
}