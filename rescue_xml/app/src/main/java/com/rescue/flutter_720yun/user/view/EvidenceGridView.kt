package com.rescue.flutter_720yun.user.view

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.util.toImgUrl

/**
 * 举报证据九宫格，对齐 iOS 黑名单列表的多图布局：
 * 最多展示 9 张；2 张或 4 张时两列，其余三列；单图按 72% 宽高比铺满。
 */
class EvidenceGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    companion object {
        private const val MAX_COUNT = 9
        private const val SINGLE_RATIO = 0.72f
    }

    private val gap = dp(6f)
    private val corner = dp(8f)
    private var visibleCount = 0
    private var columns = 3
    private var urls: List<String> = emptyList()
    private var onImageClick: ((Int) -> Unit)? = null

    fun setImages(images: List<String>, click: (Int) -> Unit) {
        urls = images
        onImageClick = click
        visibleCount = minOf(images.size, MAX_COUNT)
        columns = if (visibleCount == 2 || visibleCount == 4) 2 else 3
        removeAllViews()
        for (index in 0 until visibleCount) {
            val cell = FrameLayout(context)
            val image = ShapeableImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                shapeAppearanceModel = ShapeAppearanceModel.builder()
                    .setAllCorners(CornerFamily.ROUNDED, corner.toFloat())
                    .build()
                background = GradientDrawable().apply {
                    this.cornerRadius = corner.toFloat()
                    setColor(ContextCompat.getColor(context, R.color.color_defIcon))
                }
            }
            cell.addView(image, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            if (index == MAX_COUNT - 1 && urls.size > MAX_COUNT) {
                val overflow = TextView(context).apply {
                    text = "+${urls.size - MAX_COUNT}"
                    gravity = Gravity.CENTER
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                    textSize = 22f
                    background = GradientDrawable().apply {
                        cornerRadius = corner.toFloat()
                        setColor(0x66000000)
                    }
                }
                cell.addView(overflow, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            }
            cell.contentDescription = "举报证据，第 ${index + 1} 张，共 ${urls.size} 张"
            cell.setOnClickListener { onImageClick?.invoke(index) }
            Glide.with(context)
                .load(urls[index].toImgUrl())
                .placeholder(R.drawable.icon_eee)
                .error(R.drawable.icon_eee)
                .centerCrop()
                .into(image)
            addView(cell)
        }
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = when {
            visibleCount <= 0 -> 0
            visibleCount == 1 -> (width * SINGLE_RATIO).toInt()
            else -> {
                val cell = ((width - gap * (columns - 1)) / columns).coerceAtLeast(0)
                val rows = (visibleCount + columns - 1) / columns
                rows * cell + (rows - 1) * gap
            }
        }
        setMeasuredDimension(width, height)

        val cellWidth = if (visibleCount == 1) width else ((width - gap * (columns - 1)) / columns).coerceAtLeast(0)
        val cellHeight = if (visibleCount == 1) height else cellWidth
        for (index in 0 until childCount) {
            getChildAt(index).measure(
                MeasureSpec.makeMeasureSpec(cellWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(cellHeight, MeasureSpec.EXACTLY)
            )
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        if (visibleCount == 1) {
            getChildAt(0).layout(0, 0, width, b - t)
            return
        }
        val cell = ((width - gap * (columns - 1)) / columns).coerceAtLeast(0)
        for (index in 0 until childCount) {
            val row = index / columns
            val column = index % columns
            val left = column * (cell + gap)
            val top = row * (cell + gap)
            getChildAt(index).layout(left, top, left + cell, top + cell)
        }
    }

    private fun dp(value: Float): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
