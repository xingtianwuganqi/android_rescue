package com.rescue.flutter_720yun.adoption.ui

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.rescue.flutter_720yun.R

/** Same four filters for both roles; selection changes only the applications query. */
class AdoptionFilterBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    var onSelectionChanged: ((String) -> Unit)? = null
    var selectedFilter: String = ""
        private set
    private val options = listOf("" to R.string.adoption_filter_all, "applying" to R.string.adoption_filter_applying,
        "communicating" to R.string.adoption_filter_communicating, "ended" to R.string.adoption_filter_ended)
    init {
        orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(2), dp(2), dp(2), dp(2))
        setBackgroundResource(R.drawable.adoption_filter_background)
        options.forEachIndexed { index, (code, label) ->
            if (index > 0) addView(View(context).apply {
                setBackgroundColor(0xffd3d3d8.toInt()); importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LayoutParams(dp(1), dp(18)))
            addView(TextView(context).apply {
                text = context.getString(label); tag = code
                textSize = 14f; gravity = Gravity.CENTER
                minHeight = dp(34); setPadding(dp(2), dp(6), dp(2), dp(6))
                setTextColor(ContextCompat.getColorStateList(context, R.color.adoption_filter_text))
                setBackgroundResource(R.drawable.adoption_filter_selected)
                accessibilityDelegate = object : View.AccessibilityDelegate() {
                    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        info.className = "android.widget.RadioButton"; info.isCheckable = true; info.isChecked = host.isSelected
                    }
                }
                setOnClickListener {
                    if (selectedFilter != code) { select(code); onSelectionChanged?.invoke(code) }
                }
            }, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        }
        select("")
    }
    fun select(code: String) {
        selectedFilter = code.takeIf { filter -> options.any { it.first == filter } }.orEmpty()
        val selectedIndex = options.indexOfFirst { it.first == selectedFilter }
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child is TextView) {
                child.isSelected = child.tag == selectedFilter
                child.setTypeface(null, if (child.isSelected) Typeface.BOLD else Typeface.NORMAL)
            } else {
                val separator = (index + 1) / 2
                child.visibility = if (selectedIndex == separator || selectedIndex == separator - 1) INVISIBLE else VISIBLE
            }
        }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
