package com.rescue.flutter_720yun.home.share

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.rescue.flutter_720yun.R

/** 390 logical points, matching iOS cards. Owns only the current photo and two slide bitmaps. */
class TopicShareVideoFrames(context: Context, private val content: VideoContent) : AutoCloseable {
    private val resources = context.resources
    private val icon = BitmapFactory.decodeResource(resources, R.drawable.poster_app_icon) ?: error("视频素材加载失败")
    private val qr = BitmapFactory.decodeResource(resources, R.drawable.poster_download_qr) ?: error("视频素材加载失败")
    private val height = 1280f * 390 / 720
    private val brandY = height - 112
    private val area = RectF(20f, 20f, 370f, brandY - 20)
    private val ink = Color.rgb(59, 51, 41)
    private val orange = Color.rgb(255, 158, 0)
    private fun textPaint(size: Float, color: Int = ink, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; this.color = color; typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }
    private fun layout(text: String, size: Float, width: Int, color: Int = ink, center: Boolean = false, bold: Boolean = false) =
        StaticLayout.Builder.obtain(text, 0, text.length, textPaint(size, color, bold), width)
            .setIncludePad(false).setLineSpacing(7f, 1f)
            .setAlignment(if (center) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL).build()
    private fun text(canvas: Canvas, value: String, x: Float, y: Float, width: Int, size: Float, color: Int = ink, center: Boolean = false, bold: Boolean = false) {
        val save = canvas.save(); canvas.translate(x, y); layout(value, size, width, color, center, bold).draw(canvas); canvas.restoreToCount(save)
    }
    private fun rounded(canvas: Canvas, image: Bitmap, rect: RectF, radius: Float = 12f, filter: Boolean = true) {
        val save = canvas.save(); canvas.clipPath(Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) })
        canvas.drawBitmap(image, null, rect, Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = filter }); canvas.restoreToCount(save)
    }
    private fun brand(canvas: Canvas) {
        canvas.drawLine(20f, brandY, 370f, brandY, Paint().apply { color = Color.rgb(232, 232, 232); strokeWidth = .5f })
        rounded(canvas, icon, RectF(20f, brandY + 24, 76f, brandY + 80))
        text(canvas, "帮宠物找个家", 90f, brandY + 27, 280, 22f, Color.BLACK, bold = true)
        text(canvas, "真命天喵 App", 90f, brandY + 61, 280, 13f, Color.GRAY)
    }
    private fun card(canvas: Canvas, color: Int) {
        canvas.drawRoundRect(area, 20f, 20f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        canvas.drawRoundRect(area, 20f, 20f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.rgb(237, 222, 196); style = Paint.Style.STROKE; strokeWidth = 1f })
    }
    fun base(slide: VideoExportPlan.Slide, photo: Bitmap?): Bitmap {
        val bitmap = Bitmap.createBitmap(720, 1280, Bitmap.Config.ARGB_8888)
        try {
            val c = Canvas(bitmap); c.drawColor(Color.WHITE); c.scale(720f / 390, 720f / 390)
            when (slide.kind) {
                "photo" -> {
                    requireNotNull(photo)
                    // Downsample before enlargement to make a soft background without retaining a full blur per photo.
                    val tiny = Bitmap.createScaledBitmap(photo, maxOf(1, photo.width / 48), maxOf(1, photo.height / 48), true)
                    try {
                        val scale = maxOf(area.width() / photo.width, area.height() / photo.height)
                        val w = photo.width * scale; val h = photo.height * scale
                        c.save()
                        c.clipPath(Path().apply { addRoundRect(area,12f,12f,Path.Direction.CW) })
                        c.drawBitmap(tiny,null,RectF(area.centerX() - w/2, area.centerY() - h/2, area.centerX() + w/2, area.centerY() + h/2),Paint(Paint.FILTER_BITMAP_FLAG))
                        c.drawColor(0x2effffff)
                        c.restore()
                    } finally { if (tiny !== photo) tiny.recycle() }
                }
                "introduction" -> introduction(c)
                "search" -> {
                    card(c, Color.rgb(255, 252, 247))
                    text(c, "想了解它？", 20f, 139f, 350, 26f, center = true, bold = true)
                    text(c, "打开真命天喵 App，搜索", 20f, 204f, 350, 18f, center = true)
                    val number = content.poster.topicId.toString()
                    val size = minOf(64f, 64f * 330 / maxOf(330f, textPaint(64f, bold = true).measureText(number)))
                    text(c, number, 20f, 250f, 350, size, orange, true, true)
                    text(c, "查看详情与最新送养状态", 20f, 344f, 350, 15f, Color.GRAY, true)
                }
                "download" -> {
                    card(c, Color.rgb(255, 252, 247))
                    text(c, "扫码下载真命天喵 App", 20f, 100f, 350, 22f, center = true, bold = true)
                    text(c, "最新状态以 App 内为准", 20f, 137f, 350, 13f, Color.GRAY, true)
                    rounded(c, qr, RectF(99f, 186f, 291f, 378f), 0f, false)
                    text(c, "打开真命天喵 App，搜索", 20f, 400f, 350, 15f, Color.DKGRAY, true)
                    val number = "「${content.poster.topicId}」"
                    val size = minOf(38f, 38f * 330 / maxOf(330f, textPaint(38f, bold = true).measureText(number)))
                    text(c, number, 20f, 432f, 350, size, orange, true, true)
                    text(c, "查看详情", 20f, 486f, 350, 13f, Color.GRAY, true)
                }
            }
            brand(c); return bitmap
        } catch (e: Throwable) { bitmap.recycle(); throw e }
    }
    private fun introduction(c: Canvas) {
        card(c, Color.rgb(255, 250, 240))
        val tags = content.tags.filter(String::isNotBlank)
        val body = content.bodyDescription.ifEmpty { "更多宠物信息，请在 App 内查看。" }
        val preview = PosterText.preview(body) { layout(it, 16f, 302).lineCount <= 8 }
        val pills = mutableListOf<Pair<String, RectF>>()
        var x = 0f; var y = 0f; var omitted = false
        val maxRowsHeight = if (content.location.isEmpty()) 102 else 68
        for (tag in tags) {
            val natural = textPaint(12f, bold = true).measureText(tag) + 20
            val w = minOf(302f, natural); if (natural > 302) omitted = true
            if (x > 0 && x + w > 302) { x = 0f; y += 34 }
            if (y >= maxRowsHeight) { omitted = true; break }
            pills += tag to RectF(x, y, x + w, y + 26); x += w + 8
        }
        val status = content.poster.statusText
        val statusHeight = if (status == null) 0f else 42f
        val tagsHeight = if (pills.isEmpty()) 0f else y + 44
        val descriptionHeight = layout(preview.text, 16f, 302).height
        val locationHeight = if (content.location.isBlank()) 0f else 34f
        val guide = preview.truncated || omitted
        val total = statusHeight + tagsHeight + descriptionHeight + locationHeight + if (guide) 40 else 0
        var top = area.top + maxOf(24f, (area.height() - total) / 2)
        if (status != null) {
            val p = textPaint(12f, if (content.poster.workflow == "open") Color.rgb(51, 122, 74) else Color.DKGRAY, true)
            c.drawRoundRect(RectF(44f, top, 66f + p.measureText(status), top + 28), 14f, 14f, Paint().apply { color = if (content.poster.workflow == "open") Color.rgb(224, 242, 227) else Color.rgb(235, 235, 235) })
            c.drawText(status, 55f, top + 19, p); top += statusHeight
        }
        for ((tag, rect) in pills) {
            val r = RectF(rect).apply { offset(44f, top) }
            c.drawRoundRect(r, 8f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
            val label = android.text.TextUtils.ellipsize(tag, textPaint(12f), r.width() - 20, android.text.TextUtils.TruncateAt.END).toString()
            text(c, label, r.left + 10, r.top + 5, (r.width() - 20).toInt(), 12f)
        }
        top += tagsHeight
        text(c, preview.text, 44f, top, 302, 16f)
        if (content.location.isNotBlank()) {
            val address = android.text.TextUtils.ellipsize("⌖  ${content.location}", textPaint(13f), 302f, android.text.TextUtils.TruncateAt.END).toString()
            text(c, address, 44f, top + descriptionHeight + 14, 302, 13f, Color.rgb(120, 110, 94))
        }
        if (guide) text(c, "下载 App 查看完整介绍", 44f, top + descriptionHeight + locationHeight + 18, 302, 13f, orange, true)
    }
    fun drawFrame(target: Bitmap, base: Bitmap, previous: Bitmap?, photo: Bitmap?, index: Int, count: Int, frame: Int, frames: Int) {
        val c = Canvas(target); c.drawBitmap(base, 0f, 0f, null)
        if (photo != null) {
            c.save(); c.scale(720f / 390, 720f / 390)
            c.clipPath(Path().apply { addRoundRect(area, 12f, 12f, Path.Direction.CW) })
            val scale = minOf(area.width()/photo.width, area.height()/photo.height) / 1.04f * (1 + .04f * frame / maxOf(1, frames - 1))
            val w = photo.width*scale; val h = photo.height*scale
            c.drawBitmap(photo, null, RectF(area.centerX()-w/2, area.centerY()-h/2, area.centerX()+w/2, area.centerY()+h/2), Paint(Paint.FILTER_BITMAP_FLAG))
            if (count > 1) {
                val visible = minOf(count, 7); val start = (index - visible/2).coerceIn(0, count-visible)
                val width = (visible*5 + (visible-1)*7 + 7).toFloat(); val cy = area.bottom - 18
                c.drawRoundRect(RectF(area.centerX()-width/2-9, cy-10, area.centerX()+width/2+9, cy+10), 10f, 10f, Paint().apply { color = 0x3d000000 })
                var x = area.centerX()-width/2
                for (i in start until start+visible) {
                    val dot = if (i == index) 12f else 5f
                    c.drawRoundRect(RectF(x, cy-2.5f, x+dot, cy+2.5f), 2.5f, 2.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (i == index) Color.WHITE else 0x80ffffff.toInt() }); x += dot+7
                }
            }
            c.restore()
        }
        if (previous != null && frame.toDouble() / 30 < VideoExportPlan.TRANSITION_SECONDS) {
            c.save(); c.clipRect(0f, 0f, 720f, brandY * 720 / 390)
            c.drawBitmap(previous, 0f, 0f, Paint().apply { alpha = (255 * (1 - frame / (30 * VideoExportPlan.TRANSITION_SECONDS))).toInt().coerceIn(0,255) }); c.restore()
        }
    }
    override fun close() { icon.recycle(); qr.recycle() }
}
