package com.rescue.flutter_720yun.home.share

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.rescue.flutter_720yun.R

/** Four equal-width options, 60dp white icon tiles and a separate 50dp cancel row. */
object TopicShareSheet {
    fun show(context: Context, selected: (Int) -> Unit): BottomSheetDialog {
        fun dp(value: Int) = (context.resources.displayMetrics.density * value).toInt()
        val dialog = BottomSheetDialog(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(ContextCompat.getColor(context,R.color.color_defIcon)); setPadding(0,dp(10),0,0)
        }
        val row = LinearLayout(context).apply { setPadding(dp(15),dp(10),dp(15),dp(10)) }
        val labels = listOf("导出视频","生成海报","分享链接","复制链接")
        val icons = listOf(R.drawable.ic_share_video,R.drawable.icon_share_hb,R.drawable.icon_share_share,R.drawable.icon_share_url)
        labels.forEachIndexed { index, title ->
            val option = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; isFocusable = true; contentDescription = title }
            val tile = FrameLayout(context).apply {
                background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(6).toFloat(); setStroke(dp(1),ContextCompat.getColor(context,R.color.color_tableBack)) }
            }
            tile.addView(ImageView(context).apply { setImageResource(icons[index]); importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO },FrameLayout.LayoutParams(dp(30),dp(30),Gravity.CENTER))
            option.addView(tile,LinearLayout.LayoutParams(dp(60),dp(60)))
            option.addView(TextView(context).apply { text = title; textSize = 12f; gravity = Gravity.CENTER; setTextColor(ContextCompat.getColor(context,R.color.color_content)) },LinearLayout.LayoutParams(-1,-2).apply { topMargin = dp(5) })
            option.setOnClickListener { dialog.dismiss(); selected(index) }
            row.addView(option,LinearLayout.LayoutParams(0,dp(95),1f))
        }
        root.addView(row)
        root.addView(TextView(context).apply { text = "取消"; textSize = 16f; gravity = Gravity.CENTER; setTextColor(Color.BLACK); setBackgroundColor(Color.WHITE); setOnClickListener { dialog.dismiss() } },LinearLayout.LayoutParams(-1,dp(50)))
        dialog.setContentView(root); dialog.show(); return dialog
    }
}
