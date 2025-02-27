package com.rescue.flutter_720yun.user.adapter

import android.content.Context
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannedString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.rescue.flutter_720yun.BaseApplication
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.databinding.BlackDescItemBinding
import com.rescue.flutter_720yun.databinding.BlackImagesItemBinding
import com.rescue.flutter_720yun.databinding.BlackInputItemBinding
import com.rescue.flutter_720yun.databinding.BlackSelectItemBinding
import com.rescue.flutter_720yun.home.adapter.ReleaseImageClickListener
import com.rescue.flutter_720yun.home.adapter.ReleaseImagesAdapter
import com.rescue.flutter_720yun.home.models.CoachReleasePhoto
import com.rescue.flutter_720yun.user.models.BlackDetailModel
import com.rescue.flutter_720yun.user.models.BlackListModel

class BlackDetailAdapter(val list: MutableList<BlackDetailModel>, val listener: ReleaseImageClickListener): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
               val binding = BlackImagesItemBinding.inflate(
                   LayoutInflater.from(parent.context),
                   parent,
                   false)
               return BlackDetailImageViewHolder(binding, parent.context, listener)
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

            is BlackDetailImageViewHolder -> {
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
        if (item.order == 0) {
            val spanString = SpannableString("${item.title}*") // 确保 title 不为空
            // 设置颜色和大小
            val color = ContextCompat.getColor(BaseApplication.context, R.color.color_system)
            val textColor = ContextCompat.getColor(BaseApplication.context, R.color.color_content)
            item.title?.length?.let {
                spanString.setSpan(ForegroundColorSpan(textColor), 0,
                    it, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                spanString.setSpan(ForegroundColorSpan(color), it,
                    it + 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            }
            spanString.setSpan(AbsoluteSizeSpan(16, true), 0, spanString.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)

            binding.titleText.text = spanString
        }else {
            binding.titleText.text = item.title
        }
        binding.editDesc.hint = item.placeholder

        binding.editDesc.addTextChangedListener(
            beforeTextChanged = { _, _, _, _, ->

            },
            onTextChanged = { _, _, _, _, ->

            },
            afterTextChanged = { text ->
                item.desc = text.toString()
            }
        )

//        if (item.desc != null) {
//            binding.editDesc.setText((item.desc ?: ""))
//        }else{
//            binding.editDesc.text = null
//
//        }
    }
}

//1: 领养人，2：送养人
class BlackDetailSelectViewHolder(val binding: BlackSelectItemBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(item: BlackDetailModel) {

        val spanString = SpannableString("${item.title}*") // 确保 title 不为空
        // 设置颜色和大小
        val color = ContextCompat.getColor(BaseApplication.context, R.color.color_system)
        val textColor = ContextCompat.getColor(BaseApplication.context, R.color.color_content)
        item.title?.length?.let {
            spanString.setSpan(ForegroundColorSpan(textColor), 0,
                it, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            spanString.setSpan(ForegroundColorSpan(color), it,
                it+1, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        }
        spanString.setSpan(AbsoluteSizeSpan(16, true), 0, spanString.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        binding.titleText.text = spanString

        binding.senderText.setOnClickListener {
            binding.senderText.background = AppCompatResources.getDrawable(BaseApplication.context, R.color.color_system)
            binding.senderText.setTextColor(BaseApplication.context.resources.getColor(R.color.white,null))

            binding.resqueText.background = AppCompatResources.getDrawable(BaseApplication.context, R.color.color_eee)
            binding.resqueText.setTextColor(BaseApplication.context.resources.getColor(R.color.color_node,null))
            item.desc = "2"
        }

        binding.resqueText.setOnClickListener {
            binding.resqueText.background = AppCompatResources.getDrawable(BaseApplication.context, R.color.color_system)
            binding.resqueText.setTextColor(BaseApplication.context.resources.getColor(R.color.white,null))

            binding.senderText.background = AppCompatResources.getDrawable(BaseApplication.context, R.color.color_eee)
            binding.senderText.setTextColor(BaseApplication.context.resources.getColor(R.color.color_node,null))

            item.desc = "1"
        }
    }
}

class BlackDetailDescViewHolder(val binding: BlackDescItemBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(item: BlackDetailModel) {
        val spanString = SpannableString("${item.title}*") // 确保 title 不为空
        // 设置颜色和大小
        val color = ContextCompat.getColor(BaseApplication.context, R.color.color_system)
        val textColor = ContextCompat.getColor(BaseApplication.context, R.color.color_content)
        item.title?.length?.let {
            spanString.setSpan(ForegroundColorSpan(textColor), 0,
                it, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            spanString.setSpan(ForegroundColorSpan(color), it,
                it+1, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        }
        binding.titleText.text = spanString

        binding.editDesc.hint = item.placeholder

        binding.editDesc.addTextChangedListener(
            beforeTextChanged = { _, _, _, _, ->

            },
            onTextChanged = { _, _, _, _, ->

            },
            afterTextChanged = { text ->
                item.desc = text.toString()
            }
        )
        if (item.desc != null) {
            binding.editDesc.setText((item.desc ?: ""))
        }else{
            binding.editDesc.text = null

        }
    }
}


class BlackDetailImageViewHolder(val binding: BlackImagesItemBinding, val context: Context, val listener: ReleaseImageClickListener): RecyclerView.ViewHolder(binding.root),
    ReleaseImageClickListener {

    private val adapter = ReleaseImagesAdapter(mutableListOf())

    fun bind(item: BlackDetailModel) {
        val spanString = SpannableString("${item.title}*") // 确保 title 不为空
        // 设置颜色和大小
        val color = ContextCompat.getColor(BaseApplication.context, R.color.color_system)
        val textColor = ContextCompat.getColor(BaseApplication.context, R.color.color_content)
        item.title?.length?.let {
            spanString.setSpan(ForegroundColorSpan(textColor), 0,
                it, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            spanString.setSpan(ForegroundColorSpan(color), it,
                it+1, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        }
        spanString.setSpan(AbsoluteSizeSpan(16, true), 0, spanString.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        binding.titleText.text = spanString

        binding.recyclerview.adapter = adapter
        val gridManager = GridLayoutManager(context, 3)
        binding.recyclerview.layoutManager = gridManager
        adapter.refreshItems((item.photos ?: mutableListOf()).toList())
        adapter.setClickListener(this)
    }

    override fun addImageClick() {
        this.listener.addImageClick()
    }

    override fun deleteImageClick(item: CoachReleasePhoto) {
        this.listener.deleteImageClick(item)
    }
}


