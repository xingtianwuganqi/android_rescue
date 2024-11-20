package com.rescue.flutter_720yun.home.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.FragmentLocationListBinding
import com.rescue.flutter_720yun.home.adapter.LocationListAdapter
import com.rescue.flutter_720yun.home.models.AddressItem

class LocationListFragment : Fragment() {

    private var _binding: FragmentLocationListBinding? = null
    private val binding get() = _binding!!

    private val adapterValue by lazy {
       LocationListAdapter(mutableListOf())
    }
    var addressItems: List<AddressItem> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationListBinding.inflate(inflater,container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.localRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.localRecyclerview.adapter = adapterValue
    }

    fun setAddressItem(items: List<AddressItem>, onItemClick: (AddressItem) -> Unit) {
        addressItems = items
        adapterValue.setItems(items, onItemClick)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}