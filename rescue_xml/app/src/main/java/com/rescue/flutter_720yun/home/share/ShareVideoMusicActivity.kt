package com.rescue.flutter_720yun.home.share

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.rescue.flutter_720yun.BaseActivity
import com.rescue.flutter_720yun.R

class ShareVideoMusicActivity : BaseActivity() {
    private var player: MediaPlayer? = null
    private var previewing: ShareVideoMusic? = null
    private lateinit var rows: LinearLayout
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private val orange get() = ContextCompat.getColor(this,R.color.color_system)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setupToolbar("背景音乐管理")
        rows = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16),dp(24),dp(16),dp(24))
        }
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(242,242,247)); isFillViewport=true;addView(rows) }
        baseBinding.contentFrame.addView(scroll)
        render()
    }
    private fun render() {
        rows.removeAllViews()
        rows.addView(TextView(this).apply {
            text="视频导出配乐";textSize=13f;setTextColor(Color.rgb(110,110,115));setPadding(dp(16),0,dp(16),dp(8))
        })
        val group=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            background=GradientDrawable().apply { setColor(Color.WHITE);cornerRadius=dp(10).toFloat() }
            clipToOutline=true
        }
        rows.addView(group,LinearLayout.LayoutParams(-1,-2))
        val selected=ShareVideoMusic.selected(this)
        val choices=ShareVideoMusic.values()
        choices.forEachIndexed { index,music ->
            val row=LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL
                minimumHeight=dp(86);setPadding(dp(16),dp(12),dp(8),dp(12))
                isFocusable=true;isSelected=selected==music
                contentDescription="${music.title}，${music.detail}，${if(selected==music) "已选择" else "未选择"}"
                val attributes=obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
                foreground=attributes.getDrawable(0);attributes.recycle()
                setOnClickListener { stop(false);ShareVideoMusic.select(this@ShareVideoMusicActivity,music);render() }
            }
            row.addView(ImageView(this).apply {
                setImageResource(if(selected==music) R.drawable.ic_music_selected else R.drawable.ic_music_unselected)
                importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },LinearLayout.LayoutParams(dp(22),dp(22)))
            val labels=LinearLayout(this).apply {
                orientation=LinearLayout.VERTICAL;importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                addView(TextView(context).apply { text=music.title;textSize=16f;typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL);setTextColor(Color.BLACK) })
                addView(TextView(context).apply { text=music.detail;textSize=11f;setTextColor(Color.rgb(142,142,147));maxLines=2 },LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(4) })
            }
            row.addView(labels,LinearLayout.LayoutParams(0,-2,1f).apply { marginStart=dp(12);marginEnd=dp(8) })
            if(music!=ShareVideoMusic.NONE) row.addView(ImageButton(this).apply {
                setImageResource(if(previewing==music) R.drawable.ic_music_stop else R.drawable.ic_music_play)
                setBackgroundColor(Color.TRANSPARENT);setPadding(dp(10),dp(10),dp(10),dp(10))
                contentDescription=if(previewing==music) "停止试听" else "试听${music.title}"
                setOnClickListener { if(previewing==music) stop() else play(music) }
            },LinearLayout.LayoutParams(dp(44),dp(44)))
            group.addView(row,LinearLayout.LayoutParams(-1,-2))
            if(index!=choices.lastIndex) group.addView(View(this).apply { setBackgroundColor(Color.rgb(230,230,233)) },LinearLayout.LayoutParams(-1,dp(1).coerceAtLeast(1)).apply { marginStart=dp(50) })
        }
        rows.addView(TextView(this).apply {
            text="点击曲目即可选择并保存，点右侧按钮试听。所选配乐会自动添加到之后导出的视频中。三首内置配乐来自 FreePD 公共领域曲库（CC0），可免费商用。"
            textSize=13f;setTextColor(Color.rgb(110,110,115));setPadding(dp(16),dp(10),dp(16),0);setLineSpacing(dp(3).toFloat(),1f)
        })
    }
    private fun play(music: ShareVideoMusic) {
        stop(false)
        try {
            val audio=MediaPlayer();player=audio
            audio.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            audio.setVolume(0.45f,0.45f)
            assets.openFd("ShareMusic/${music.asset}.m4a").use { audio.setDataSource(it.fileDescriptor,it.startOffset,it.length) }
            audio.setOnCompletionListener { if(player===it) stop() }
            audio.setOnErrorListener { media,_,_ -> if(player===media) { stop();Toast.makeText(this,"音乐加载失败，请重试",Toast.LENGTH_SHORT).show() };true }
            audio.setOnPreparedListener { if(player===it) it.start() }
            audio.prepareAsync();previewing=music;render()
        } catch(e: Exception) { stop();Toast.makeText(this,"音乐加载失败，请重试",Toast.LENGTH_SHORT).show() }
    }
    private fun stop(refresh: Boolean=true) { player?.release();player=null;previewing=null;if(refresh && ::rows.isInitialized) render() }
    override fun onPause() { stop();super.onPause() }
    override fun onDestroy() { stop(false);super.onDestroy() }
}
