package com.rescue.flutter_720yun.home.share

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.rescue.flutter_720yun.R
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.io.File

/** Private full-screen preview. A separate retained copy grants other apps read access. */
class TopicVideoPreviewActivity : AppCompatActivity() {
    private lateinit var file: File
    private lateinit var player: VideoView
    private lateinit var status: TextView
    private lateinit var saveButton: Button
    private lateinit var shareButton: Button
    private lateinit var closeButton: Button
    private var busy = false
    private var resumePosition = 0
    private val topic get() = intent.getIntExtra("topic_id",0)
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) save() else { status.text = getString(R.string.common_album_permission_save); update(false) }
    }
    private val share = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { update(false) }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val name = intent.getStringExtra("video_file") ?: run { finish(); return }
        file = File(ShareFiles.directory(this),name)
        if (name != file.name || !name.endsWith(".mp4") || !file.isFile || topic <= 0) { finish(); return }
        resumePosition = state?.getInt("position") ?: 0
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        closeButton = button(getString(R.string.common_close)) { if (!busy) finish() }
        top.addView(closeButton); top.addView(TextView(this).apply { text = getString(R.string.share_preview_video); textSize = 20f; setTextColor(Color.BLACK) })
        root.addView(top)
        val area = FrameLayout(this)
        player = VideoView(this)
        area.addView(player,FrameLayout.LayoutParams(-1,-1,Gravity.CENTER))
        root.addView(area,LinearLayout.LayoutParams(-1,0,1f))
        status = TextView(this).apply { text = getString(R.string.share_preview_hint); gravity = Gravity.CENTER; setTextColor(Color.GRAY); setPadding(20,20,20,20) }
        root.addView(status)
        val controls = LinearLayout(this).apply { setPadding(16,0,16,24) }
        saveButton = button(getString(R.string.common_save_to_gallery)) {
            if (!busy) {
                if (Build.VERSION.SDK_INT < 29 && ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    update(true)
                    permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else save()
            }
        }
        shareButton = button(getString(R.string.share_video)) { shareVideo() }
        saveButton.setTextColor(Color.WHITE)
        saveButton.background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.rgb(255,165,0)); cornerRadius = 12 * resources.displayMetrics.density }
        shareButton.background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.rgb(240,240,240)); cornerRadius = 12 * resources.displayMetrics.density }
        controls.addView(saveButton,LinearLayout.LayoutParams(0,(50*resources.displayMetrics.density).toInt(),1f).apply { marginEnd = (12*resources.displayMetrics.density).toInt() }); controls.addView(shareButton,LinearLayout.LayoutParams(0,(50*resources.displayMetrics.density).toInt(),1f)); root.addView(controls)
        setContentView(root)
        val revision = com.rescue.flutter_720yun.util.UserManager.sessionRevision.value
        com.rescue.flutter_720yun.util.UserManager.sessionRevision.observe(this) { if (it != revision) finish() }
        val media = MediaController(this); media.setAnchorView(player); player.setMediaController(media)
        player.setVideoPath(file.path)
        player.setOnPreparedListener { player.seekTo(resumePosition); player.start() }
        player.setOnErrorListener { _,_,_ -> status.text = getString(R.string.share_video_play_failed); true }
        onBackPressedDispatcher.addCallback(this,object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if (!busy) finish() }
        })
    }
    private fun button(title: String, action: () -> Unit) = androidx.appcompat.widget.AppCompatButton(this).apply {
        text = title; setTextColor(Color.rgb(230,130,0)); setOnClickListener { action() }
    }
    private fun update(value: Boolean) {
        busy = value
        if (::saveButton.isInitialized) { saveButton.isEnabled = !value; shareButton.isEnabled = !value; closeButton.isEnabled = !value }
    }
    private suspend fun verify() {
        val latest = PosterRepository(this).videoContent(topic)
        check(latest.fingerprint == intent.getStringExtra("snapshot")) { getString(R.string.common_post_changed_back) }
    }
    private fun save() {
        update(true); saveButton.text = getString(R.string.share_saving)
        lifecycleScope.launch {
            try {
                verify(); PosterGallery.save(applicationContext,file,topic,true)
                status.text = getString(R.string.share_video_saved)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { status.text = e.message?.takeIf { it.contains(getString(R.string.common_post)) } ?: getString(R.string.share_save_failed_hint) }
            finally { saveButton.text = getString(R.string.common_save_to_gallery); update(false) }
        }
    }
    private fun shareVideo() {
        if (busy) return
        update(true); player.pause()
        lifecycleScope.launch {
            try {
                verify()
                val copy = ShareFiles.newFile(this@TopicVideoPreviewActivity,"mp4")
                withContext(Dispatchers.IO) { file.copyTo(copy) }
                share.launch(ShareFiles.intent(this@TopicVideoPreviewActivity,copy,"video/mp4"))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { status.text = e.message?.takeIf { it.contains(getString(R.string.common_post)) } ?: getString(R.string.common_share_failed); update(false) }
        }
    }
    override fun onResume() { super.onResume(); if (::player.isInitialized) { player.seekTo(resumePosition); player.start() } }
    override fun onPause() { if (::player.isInitialized) { resumePosition = player.currentPosition; player.pause() }; super.onPause() }
    override fun onSaveInstanceState(out: Bundle) { out.putInt("position",if (::player.isInitialized) player.currentPosition else resumePosition); super.onSaveInstanceState(out) }
    override fun onDestroy() {
        if (::player.isInitialized) player.stopPlayback()
        if (isFinishing && ::file.isInitialized) file.delete()
        super.onDestroy()
    }
}
