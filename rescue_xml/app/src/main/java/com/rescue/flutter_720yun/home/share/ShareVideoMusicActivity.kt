package com.rescue.flutter_720yun.home.share

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.*
import com.rescue.flutter_720yun.BaseActivity

class ShareVideoMusicActivity : BaseActivity() {
    private var player: MediaPlayer? = null
    private var previewing: ShareVideoMusic? = null
    private lateinit var rows: LinearLayout
    override fun onCreate(state: Bundle?) {
        super.onCreate(state); setupToolbar("背景音乐管理")
        rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24,16,24,16); setBackgroundColor(Color.rgb(245,245,245)) }
        val scroll = ScrollView(this).apply { addView(rows) }
        baseBinding.contentFrame.addView(scroll)
        render()
    }
    private fun render() {
        rows.removeAllViews()
        rows.addView(TextView(this).apply { text = "视频导出配乐"; textSize = 18f; setTextColor(Color.DKGRAY); setPadding(8,8,8,16) })
        val selected = ShareVideoMusic.selected(this)
        ShareVideoMusic.values().forEach { music ->
            val row = LinearLayout(this).apply { gravity = android.view.Gravity.CENTER_VERTICAL; setPadding(8,12,8,12) }
            val label = TextView(this).apply {
                text = "${if (music == selected) "●" else "○"}  ${music.title}\n${music.detail}"; textSize = 15f; setTextColor(Color.DKGRAY)
                setPadding(8,12,8,12); setOnClickListener { ShareVideoMusic.select(this@ShareVideoMusicActivity,music); render() }
            }
            row.addView(label,LinearLayout.LayoutParams(0,-2,1f))
            if (music != ShareVideoMusic.NONE) row.addView(androidx.appcompat.widget.AppCompatButton(this).apply {
                text = if (previewing == music) "停止" else "试听"; setTextColor(Color.rgb(230,130,0))
                contentDescription = text.toString() + music.title
                setOnClickListener { if (previewing == music) stop() else play(music) }
            })
            rows.addView(row)
        }
        rows.addView(TextView(this).apply { text = "点击曲目即可选择并保存，点右侧按钮试听。所选配乐会自动添加到之后导出的视频中。三首内置配乐来自 FreePD 公共领域曲库（CC0），可免费商用。"; textSize = 13f; setTextColor(Color.GRAY); setPadding(8,24,8,16) })
    }
    private fun play(music: ShareVideoMusic) {
        stop()
        try {
            val audio = MediaPlayer(); player = audio
            assets.openFd("ShareMusic/${music.asset}.m4a").use { audio.setDataSource(it.fileDescriptor,it.startOffset,it.length) }
            audio.setOnCompletionListener { stop() }; audio.setOnErrorListener { _,_,_ -> stop(); true }
            audio.setOnPreparedListener { if (player === it) it.start() }
            audio.prepareAsync(); previewing = music; render()
        } catch (e: Exception) { stop(); Toast.makeText(this,"配乐加载失败，请重试",Toast.LENGTH_SHORT).show() }
    }
    private fun stop() { player?.release(); player = null; previewing = null; if (::rows.isInitialized) render() }
    override fun onPause() { stop(); super.onPause() }
    override fun onDestroy() { player?.release(); player = null; super.onDestroy() }
}
