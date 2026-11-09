package com.rescue.flutter_720yun.adoption.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.*
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.user.activity.BlackListActivity
import com.rescue.flutter_720yun.user.activity.WebPageActivity
import com.rescue.flutter_720yun.util.AppBuildConfig

/** iOS TopicRemindView: body opens prevention guidance; the final link opens cases. */
class TopicSafetyNotice @JvmOverloads constructor(context: Context, attrs: AttributeSet?=null): LinearLayout(context,attrs) {
    private val icon=ImageView(context)
    private val label=TextView(context)
    private fun dp(value: Int)=(value*resources.displayMetrics.density).toInt()
    init {
        orientation=HORIZONTAL;gravity=Gravity.TOP
        setPadding(dp(15),dp(8),dp(15),dp(8))
        icon.setImageResource(R.drawable.icon_topic_rm);icon.importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
        addView(icon,LayoutParams(dp(20),dp(20)).apply { topMargin=dp(3);marginEnd=dp(7) })
        label.apply { textSize=14f;typeface=Typeface.create(Typeface.DEFAULT,500,false);setLineSpacing(dp(3).toFloat(),1f);movementMethod=LinkMovementMethod.getInstance();highlightColor=Color.TRANSPARENT }
        addView(label,LayoutParams(0,-2,1f));showDetail(false)
    }
    fun showDetail(owner: Boolean) {
        if(owner) {
            icon.visibility=View.GONE;setBackgroundColor(Color.WHITE)
            label.text="点击管理申请可处理领养申请。完成送养时，请选择实际接宠人；其他有效申请将结束，联系方式授权将撤销。"
            label.textSize=13f;label.typeface=Typeface.DEFAULT;label.setTextColor(Color.rgb(153,153,153));label.movementMethod=null
        } else warning("特别提醒：请勿相信以任何名义(包括运费、押金、定金等)要求的提前转帐与打款的行为。请提高警惕，以防被骗！")
    }
    fun showComment() { warning("请提高警惕！以避免不必要的财产损失！") }
    private fun warning(body: String) {
        icon.visibility=View.VISIBLE;setBackgroundColor(Color.rgb(255,245,218))
        label.textSize=14f;label.typeface=Typeface.create(Typeface.DEFAULT,500,false)
        label.setTextColor(Color.rgb(246,131,31));label.movementMethod=LinkMovementMethod.getInstance()
        val cases="查看案例->"
        label.text=SpannableString(body+cases).apply {
            fun link(start: Int,end: Int,color: Int,action: () -> Unit) {
                setSpan(object: ClickableSpan() {
                    override fun onClick(widget: View) { action() }
                    override fun updateDrawState(ds: TextPaint) { ds.color=color;ds.isUnderlineText=false }
                },start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            link(0,body.length,Color.rgb(246,131,31)) {
                context.startActivity(Intent(context,WebPageActivity::class.java).putExtra("webUrl",AppBuildConfig.BASEURL+AppBuildConfig.PREVENTION))
            }
            link(body.length,body.length+cases.length,Color.rgb(246,111,11)) { context.startActivity(Intent(context,BlackListActivity::class.java)) }
        }
    }
}
