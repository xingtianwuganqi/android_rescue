package com.rescue.flutter_720yun.home.share

import android.content.Context
import android.graphics.*
import android.text.*
import androidx.core.content.ContextCompat
import com.rescue.flutter_720yun.R
import kotlin.math.ceil

/** Draws a standalone 390 logical point poster at 3x, never captures page content. */
class TopicSharePosterRenderer(context: Context) {
    private val resources=context.applicationContext.resources
    private val themeColor=ContextCompat.getColor(context,R.color.color_system)
    private fun paint(size: Float,color: Int=Color.rgb(41,41,41),bold: Boolean=false)=TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize=size;this.color=color;typeface=if(bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }
    private fun layout(text: CharSequence,paint: TextPaint,width: Int=350,center: Boolean=false): StaticLayout =
        StaticLayout.Builder.obtain(text,0,text.length,paint,width).setIncludePad(false)
            .setAlignment(if(center) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(5f,1f).build()
    fun render(content: PosterContent,petImage: Bitmap): Bitmap {
        require(petImage.width>0 && petImage.height>0)
        val icon=BitmapFactory.decodeResource(resources,R.drawable.poster_app_icon) ?: error("Brand resource missing")
        val qr=if(content.includeQr) BitmapFactory.decodeResource(resources,R.drawable.poster_download_qr) ?: error("QR resource missing") else null
        try {
            val preview=PosterText.preview(content.description) { layout(it,paint(16f)).lineCount<=8 }
            val description=layout(preview.text,paint(16f))
            val status=content.statusText
            val textY=if(status==null) 112f else 156f
            val textHeight=if(preview.text.isEmpty()) 0f else description.height.toFloat()
            val imageY=textY+textHeight+(if(preview.truncated) 28f else 0f)+(if(textHeight>0) 20f else 0f)
            val imageHeight=350f*petImage.height/petImage.width
            val instruction=SpannableString("打开真命天喵 App，搜索「${content.topicId}」查看详情").apply {
                val number=content.topicId.toString();val start=indexOf(number)
                setSpan(android.text.style.ForegroundColorSpan(themeColor),start,start+number.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(android.text.style.AbsoluteSizeSpan(20,false),start,start+number.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(android.text.style.StyleSpan(Typeface.BOLD),start,start+number.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            val search=layout(instruction,paint(16f),center=true)
            val searchY=imageY+imageHeight+24
            val footerY=searchY+search.height+24
            val height=ceil((footerY+if(content.includeQr) 218 else 0)*3).toInt()
            require(height in 1..6000) { "Image aspect ratio exceeds safe output size" }
            val bitmap=Bitmap.createBitmap(1170,height,Bitmap.Config.ARGB_8888)
            try {
                val canvas=Canvas(bitmap);canvas.drawColor(Color.WHITE);canvas.scale(3f,3f)
                fun rounded(image: Bitmap,rect: RectF,radius: Float,filter: Boolean=true) {
                    val save=canvas.save();val path=Path().apply { addRoundRect(rect,radius,radius,Path.Direction.CW) }
                    canvas.clipPath(path);canvas.drawBitmap(image,null,rect,Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap=filter });canvas.restoreToCount(save)
                }
                fun text(text: CharSequence,x: Float,y: Float,font: TextPaint,center: Boolean=false) {
                    val save=canvas.save();canvas.translate(x,y);layout(text,font,center=center).draw(canvas);canvas.restoreToCount(save)
                }
                rounded(icon,RectF(20f,24f,76f,80f),12f)
                canvas.drawText("帮宠物找个家",90f,50f,paint(22f,Color.BLACK,true))
                canvas.drawText("真命天喵 App",90f,76f,paint(13f,Color.rgb(122,122,122)))
                if(status!=null) {
                    val statusPaint=paint(13f,if(content.workflow=="open") Color.rgb(46,122,64) else Color.DKGRAY,true)
                    val rect=RectF(20f,104f,44f+statusPaint.measureText(status),134f)
                    canvas.drawRoundRect(rect,8f,8f,Paint().apply { color=if(content.workflow=="open") Color.rgb(232,247,235) else Color.rgb(240,240,240) })
                    canvas.drawText(status,32f,124f,statusPaint)
                }
                if(preview.text.isNotEmpty()) text(preview.text,20f,textY,paint(16f))
                if(preview.truncated) text("完整介绍请在 App 中查看",20f,textY+textHeight+10,paint(13f,Color.GRAY))
                rounded(petImage,RectF(20f,imageY,370f,imageY+imageHeight),12f)
                text(instruction,20f,searchY,paint(16f),true)
                if(content.includeQr) {
                    canvas.drawLine(20f,footerY,370f,footerY,Paint().apply { color=Color.rgb(232,232,232);strokeWidth=1f/3 })
                    text("扫码下载真命天喵 App",20f,footerY+20,paint(14f,Color.GRAY),true)
                    text("最新状态以 App 内为准",20f,footerY+46,paint(12f,Color.GRAY),true)
                    // No smoothing/resampling of code modules. Original PNG already contains quiet space.
                    rounded(qr!!,RectF(123f,footerY+66,267f,footerY+210),0f,false)
                }
                return bitmap
            } catch(error: Throwable) { bitmap.recycle();throw error }
        } finally { icon.recycle();qr?.recycle() }
    }
}
