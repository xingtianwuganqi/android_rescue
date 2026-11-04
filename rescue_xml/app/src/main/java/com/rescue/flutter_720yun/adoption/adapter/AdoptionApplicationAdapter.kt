package com.rescue.flutter_720yun.adoption.adapter

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButton
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adoption.models.*
import com.rescue.flutter_720yun.databinding.AdoptionApplicationItemBinding
import com.rescue.flutter_720yun.util.toImgUrl

class AdoptionApplicationAdapter(
    private val detail: (AdoptionApplication) -> Unit,
    private val topic: (AdoptionApplication) -> Unit,
    private val copyContact: ((AdoptionApplication) -> Unit)? = null,
    private val action: (AdoptionApplication, String) -> Unit = { _, _ -> },
    private val busy: () -> Boolean = { false },
    private val previews: () -> Map<Int, String> = { emptyMap() },
    private val missingPreview: (Int) -> Unit = {}
) : RecyclerView.Adapter<AdoptionApplicationAdapter.Holder>() {
    private var items = emptyList<AdoptionApplication>()
    private val mine get() = copyContact != null
    class Holder(val binding: AdoptionApplicationItemBinding) : RecyclerView.ViewHolder(binding.root) {
        var topicId: Int? = null
    }
    override fun getItemCount() = items.size
    fun submit(value: List<AdoptionApplication>) { items = value; notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, type: Int) = Holder(
        AdoptionApplicationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val app = items[position]
        val b = holder.binding
        val context = b.root.context
        val disabled = busy()
        val time = AdoptionTime.display(app.created_at)
        val fullStatus = AdoptionOperations.status(app)
        val badge = when (app.status) {
            "applying" -> "申请中"
            "communicating" -> "沟通中"
            "ended" -> if (fullStatus.endsWith("被拒绝")) "被拒绝" else "已结束"
            else -> "状态暂不可用"
        }
        b.applicationMineHeader.isVisible = mine
        b.applicationReceivedContent.isVisible = !mine
        b.applicationMineTime.text = time
        b.applicationReceivedTime.text = time
        listOf(b.applicationMineStatus, b.applicationReceivedStatus).forEach { status ->
            status.text = badge
            val (background, foreground) = when (app.status) {
                "applying" -> R.color.adoption_applying_background to R.color.color_system
                "communicating" -> R.color.adoption_communicating_background to R.color.adoption_communicating_text
                else -> R.color.adoption_status_background to R.color.adoption_text_secondary
            }
            status.background = GradientDrawable().apply {
                cornerRadius = status.dp(8).toFloat()
                setColor(ContextCompat.getColor(context, background))
            }
            status.setTextColor(ContextCompat.getColor(context, foreground))
        }
        b.applicationApplicant.text = app.applicant?.nickname?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.adoption_applicant_unavailable)
        b.applicationResult.text = when {
            app.status == "ended" && app.result == "completed" -> "已结束 · 完成送养（送养人报告）"
            else -> fullStatus
        }
        b.applicationResult.isVisible = app.status == "ended"
        Glide.with(b.applicationAvatar).clear(b.applicationAvatar)
        b.applicationAvatar.setImageResource(R.drawable.icon_eee)
        if (!mine) Glide.with(b.applicationAvatar).load(app.applicant?.avatar?.toImgUrl())
            .placeholder(R.drawable.icon_eee).error(R.drawable.icon_eee).into(b.applicationAvatar)
        val snapshot = app.profile_snapshot.takeUnless { app.topic?.unavailable == true || app.topic?.is_delete == 0 }
        b.applicationProfileSummary.text = AdoptionLabels.summary(snapshot)
        b.applicationProfile.isEnabled = !disabled
        b.applicationProfile.setOnClickListener { detail(app) }

        val available = app.topic?.let { !it.unavailable && it.is_delete != 0 } == true
        val title = if (available) app.topic?.content_excerpt?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.adoption_topic_fallback, app.topic!!.topic_id)
        else context.getString(R.string.adoption_topic_unavailable)
        b.applicationTopicTitle.text = title
        b.applicationTopicTitle.setTextColor(ContextCompat.getColor(context,
            if (available) R.color.black else R.color.adoption_text_secondary))
        b.applicationTopicArrow.isVisible = available
        b.applicationTopic.isEnabled = available
        b.applicationTopic.setOnClickListener(if (available) View.OnClickListener { topic(app) } else null)
        b.applicationTopic.isClickable = available
        b.applicationTopic.contentDescription = if (available) context.getString(R.string.adoption_view_topic, title) else title
        bindPhoto(holder, app.topic.takeIf { available })

        // Contact authorization remains independent of a single application's terminal state.
        val contact = if (mine) AdoptionContact.visible(app.topic) else null
        b.applicationContact.isVisible = mine
        b.applicationContactValue.text = contact ?: context.getString(R.string.adoption_no_contact)
        b.applicationContactValue.setTextColor(ContextCompat.getColor(context,
            if (contact == null) R.color.adoption_text_secondary else R.color.color_content))
        b.applicationActions.removeAllViews()
        if (contact != null && app.status == "communicating") {
            addAction(b, "复制联系方式", false, disabled) { copyContact?.invoke(app) }
        }
        AdoptionOperations.actions(app, mine).forEach { code ->
            addAction(b, AdoptionOperations.title(code), code in listOf("communicate", "complete"), disabled) { action(app, code) }
        }
        b.applicationActions.isVisible = b.applicationActions.childCount > 0
    }

    private fun bindPhoto(holder: Holder, topic: TopicSummary?) {
        val photo = holder.binding.applicationTopicPhoto
        holder.topicId = topic?.topic_id
        Glide.with(photo).clear(photo)
        photo.setImageResource(R.drawable.icon_eee)
        if (topic == null) return
        val image = previews()[topic.topic_id] ?: topic.preview_img
        if (image.isNullOrBlank()) missingPreview(topic.topic_id)
        Glide.with(photo).load(image?.toImgUrl())
            .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(e: com.bumptech.glide.load.engine.GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>, first: Boolean): Boolean {
                    photo.post { if (holder.topicId == topic.topic_id) missingPreview(topic.topic_id) }
                    return false
                }
                override fun onResourceReady(resource: android.graphics.drawable.Drawable, model: Any, target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?, source: com.bumptech.glide.load.DataSource, first: Boolean) = false
            })
            .placeholder(R.drawable.icon_eee).error(R.drawable.icon_eee).into(photo)
    }

    private fun addAction(b: AdoptionApplicationItemBinding, title: String, primary: Boolean, disabled: Boolean, click: () -> Unit) {
        val button = MaterialButton(b.root.context).apply {
            text = title; textSize = 13f; isAllCaps = false
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            minWidth = 0; minimumWidth = 0; minHeight = dp(36); minimumHeight = dp(36)
            insetTop = 0; insetBottom = 0; cornerRadius = dp(9)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, if (primary) R.color.color_system else R.color.white))
            setTextColor(ContextCompat.getColor(context, if (primary) R.color.white else R.color.color_content))
            strokeWidth = if (primary) 0 else dp(1)
            strokeColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.adoption_card_border))
            isEnabled = !disabled; alpha = if (disabled) .45f else 1f
            setOnClickListener { click() }
        }
        b.applicationActions.addView(button, FlexboxLayout.LayoutParams(-2, -2).apply {
            marginStart = button.dp(8); topMargin = button.dp(4)
        })
    }
    override fun onViewRecycled(holder: Holder) {
        holder.topicId = null
        Glide.with(holder.binding.applicationTopicPhoto).clear(holder.binding.applicationTopicPhoto)
        Glide.with(holder.binding.applicationAvatar).clear(holder.binding.applicationAvatar)
        super.onViewRecycled(holder)
    }
    private fun View.dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
