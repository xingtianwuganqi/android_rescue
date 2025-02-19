package com.rescue.flutter_720yun.user.adapter

import android.text.Editable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.BlackDescItemBinding
import com.rescue.flutter_720yun.databinding.BlackInputItemBinding
import com.rescue.flutter_720yun.databinding.BlackSelectItemBinding
import com.rescue.flutter_720yun.user.models.BlackDetailModel
import com.rescue.flutter_720yun.user.models.BlackListModel

class BlackDetailAdapter(val list: MutableList<BlackDetailModel>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val INPUT = 0
        const val SELECT = 1
        const val DESC = 2
        const val IMAGE = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when(position) {
            0,1,2 -> INPUT
            3 -> SELECT
            4 -> DESC
            else -> IMAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
       when (viewType) {
           INPUT -> {
               val binding = BlackInputItemBinding.inflate(
                   LayoutInflater.from(parent.context),
                   parent,
                   false)
               return BlackDetailInputViewHolder(binding)
           }
           SELECT -> {
               val binding = BlackSelectItemBinding.inflate(
                   LayoutInflater.from(parent.context),
                   parent,
                   false)
               return BlackDetailSelectViewHolder(binding)
           }
           DESC -> {
               val binding = BlackDescItemBinding.inflate(
                   LayoutInflater.from(parent.context),
                   parent,
                   false)
               return BlackDetailDescViewHolder(binding)
           }
           else -> {
               val binding = BlackDescItemBinding.inflate(
                   LayoutInflater.from(parent.context),
                   parent,
                   false)
               return BlackDetailDescViewHolder(binding)
           }
       }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list[position]
        when (holder) {
            is BlackDetailInputViewHolder -> {
                holder.bind(item)
            }
            is BlackDetailSelectViewHolder -> {
                holder.bind(item)
            }

            is BlackDetailDescViewHolder -> {
                holder.bind(item)
            }
        }
    }

    fun refreshItem(items: List<BlackDetailModel>) {
        list.clear()
        list.addAll(items)
        notifyDataSetChanged()
    }
}

class BlackDetailInputViewHolder(val binding: BlackInputItemBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(item: BlackDetailModel) {
        binding.titleText.text = item.title
        binding.editDesc.hint = item.placeholder
        if (item.desc != null) {
            binding.editDesc.setText((item.desc ?: ""))
        }
    }
}

class BlackDetailSelectViewHolder(val binding: BlackSelectItemBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(item: BlackDetailModel) {
        binding.titleText.text = item.title
        binding.switchButton.textOn = ContextCompat.getString(BaseApplication.context, R.string.user_report)
        binding.switchButton.textOff = ContextCompat.getString(BaseApplication.context, R.string.user_sender)

    }
}

class BlackDetailDescViewHolder(val binding: BlackDescItemBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(item: BlackDetailModel) {
        binding.titleText.text = item.title
        binding.editDesc.hint = item.placeholder
        if (item.desc != null) {
            binding.editDesc.setText((item.desc ?: ""))
        }
    }
}