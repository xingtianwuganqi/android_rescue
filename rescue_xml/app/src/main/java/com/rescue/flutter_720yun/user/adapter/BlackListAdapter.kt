package com.rescue.flutter_720yun.user.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.BlackListItemBinding
import com.rescue.flutter_720yun.user.models.BlackListModel
import com.rescue.flutter_720yun.util.maskCenter
import com.rescue.flutter_720yun.util.toImgUrl

class BlackListAdapter(
    var list: MutableList<BlackListModel>,
    private val clickListener: (BlackListModel) -> Unit,
    private val evidenceListener: (List<String>, Int) -> Unit
) : RecyclerView.Adapter<BlackListAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: BlackListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BlackListModel) {
            val context = binding.root.context
            val name = item.name?.trim().orEmpty()
            binding.nameText.text = if (name.isEmpty()) {
                ContextCompat.getString(context, R.string.black_unknown_name)
            } else {
                name.maskCenter()
            }

            val contact = item.contact?.trim().orEmpty()
            binding.phoneText.text = if (contact.isEmpty()) {
                ContextCompat.getString(context, R.string.black_unknown_contact)
            } else {
                context.getString(R.string.black_contact_value, contact.maskCenter())
            }

            binding.roleText.text = ContextCompat.getString(
                context,
                if (item.black_type == 2) R.string.user_sender else R.string.user_rescue
            )

            val desc = item.desc?.trim().orEmpty()
            binding.desc.text = if (desc.isEmpty()) {
                ContextCompat.getString(context, R.string.black_no_desc)
            } else {
                desc
            }

            val evidence = item.images.orEmpty()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { it.toImgUrl() }
            binding.evidenceSection.isVisible = evidence.isNotEmpty()
            if (evidence.isNotEmpty()) {
                binding.evidenceLabel.text = context.getString(R.string.black_evidence_count, evidence.size)
                binding.evidenceGrid.setImages(evidence) { index ->
                    evidenceListener(evidence, index)
                }
            } else {
                binding.evidenceGrid.setImages(emptyList()) { }
            }

            binding.backLayout.setOnClickListener {
                clickListener(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = BlackListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    fun refreshItem(items: List<BlackListModel>) {
        list.clear()
        list.addAll(items)
        notifyDataSetChanged()
    }

    fun addItems(newList: List<BlackListModel>) {
        val startPosition = list.size
        list.addAll(newList)
        notifyItemRangeInserted(startPosition, newList.size)
    }
}
