package com.rescue.flutter_720yun.home.adapter

import android.location.Address
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.home.models.AddressItem

class CitySelectAdapter(
    val list: List<AddressItem>,
    private val clickCallback: (AddressItem?) -> Unit
    ): RecyclerView.Adapter<CitySelectAdapter.ViewHolder>() {

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.city_content)
        val cityList: RecyclerView = view.findViewById(R.id.city_list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.city_content_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.titleText.text = item.name
        holder.cityList.layoutManager = LinearLayoutManager(BaseApplication.context, LinearLayoutManager.VERTICAL, false)
        holder.cityList.adapter = CityItemAdapter(item.children,clickCallback)
    }


}


class CityItemAdapter(
    val list: List<AddressItem>?,
    val clickItem: (AddressItem?) -> Unit
): RecyclerView.Adapter<CityItemAdapter.ViewHolder>() {

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.city_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.city_title_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list?.size ?: 0
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list?.get(position)
        holder.titleText.text = item?.name
        holder.titleText.setOnClickListener {
            clickItem(item)
        }
    }


}