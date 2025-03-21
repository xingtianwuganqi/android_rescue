package com.rescue.flutter_720yun.user.adapter

import android.content.Context
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

class BlackDetailAdapter(val blackId: Int, val list: MutableList<BlackDetailModel>, val listener: ReleaseImageClickListener): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
                holder.bind(blackId, item)
            }
            is BlackDetailSelectViewHolder -> {
                holder.bind(blackId, item)
            }

            is BlackDetailDescViewHolder -> {
                holder.bind(blackId, item)
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
    fun bind(blackId: Int, item: BlackDetailModel) {
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

        // 移除旧的 TextWatcher，防止 setText() 触发监听
        (binding.editDesc.tag as? TextWatcher)?.let {
            binding.editDesc.removeTextChangedListener(it)
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                item.desc = s?.toString()
            }
        }

        binding.editDesc.setText(item.desc)

        // 重新添加 TextWatcher
        binding.editDesc.addTextChangedListener(textWatcher)

        // 绑定 TextWatcher 到 EditText
        binding.editDesc.tag = textWatcher

        if (blackId != 0) {
            binding.editDesc.isEnabled = false
        }
    }
}

//1: 领养人，2：送养人
class BlackDetailSelectViewHolder(val binding: BlackSelectItemBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(blackId: Int, item: BlackDetailModel) {

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

            binding.rescueText.background = AppCompatResources.getDrawable(BaseApplication.context, R.color.color_eee)
            binding.rescueText.setTextColor(BaseApplication.context.resources.getColor(R.color.color_node,null))
            item.desc = "2"
        }

        binding.rescueText.setOnClickListener {
            binding.rescueText.background = AppCompatResources.getDrawable(BaseApplication.context, R.color.color_system)
            binding.rescueText.setTextColor(BaseApplication.context.resources.getColor(R.color.white,null))

            binding.senderText.background = AppCompatResources.getDrawable(BaseApplication.context, R.color.color_eee)
            binding.senderText.setTextColor(BaseApplication.context.resources.getColor(R.color.color_node,null))

            item.desc = "1"
        }

        if (blackId != 0) {
            binding.senderText.isEnabled = false
            binding.rescueText.isEnabled = false
            if (item.desc == "2") {
                binding.senderText.background =
                    AppCompatResources.getDrawable(BaseApplication.context, R.color.color_system)
                binding.senderText.setTextColor(
                    BaseApplication.context.resources.getColor(
                        R.color.white,
                        null
                    )
                )

                binding.rescueText.background =
                    AppCompatResources.getDrawable(BaseApplication.context, R.color.color_eee)
                binding.rescueText.setTextColor(
                    BaseApplication.context.resources.getColor(
                        R.color.color_node,
                        null
                    )
                )

            } else {
                binding.rescueText.background =
                    AppCompatResources.getDrawable(BaseApplication.context, R.color.color_system)
                binding.rescueText.setTextColor(
                    BaseApplication.context.resources.getColor(
                        R.color.white,
                        null
                    )
                )

                binding.senderText.background =
                    AppCompatResources.getDrawable(BaseApplication.context, R.color.color_eee)
                binding.senderText.setTextColor(
                    BaseApplication.context.resources.getColor(
                        R.color.color_node,
                        null
                    )
                )

            }
        }
    }
}

class BlackDetailDescViewHolder(val binding: BlackDescItemBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(blackId: Int, item: BlackDetailModel) {
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

        // 移除旧的 TextWatcher，防止 setText() 触发监听
        (binding.editDesc.tag as? TextWatcher)?.let {
            binding.editDesc.removeTextChangedListener(it)
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                item.desc = s?.toString()
            }
        }

        binding.editDesc.setText(item.desc)

        // 重新添加 TextWatcher
        binding.editDesc.addTextChangedListener(textWatcher)

        // 绑定 TextWatcher 到 EditText
        binding.editDesc.tag = textWatcher

        if (blackId != 0) {
            binding.editDesc.isEnabled = false
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

    override fun photoClickCallBack(position: Int) {
        this.listener.photoClickCallBack(position)
    }
}


