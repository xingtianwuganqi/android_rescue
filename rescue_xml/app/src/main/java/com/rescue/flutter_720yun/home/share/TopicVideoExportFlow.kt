package com.rescue.flutter_720yun.home.share

import android.content.Intent
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adoption.ui.notice
import com.rescue.flutter_720yun.util.UserManager
import kotlinx.coroutines.*
import java.io.File

class TopicVideoExportFlow(private val host: FragmentActivity, private val topic: () -> Int?,
    private val available: () -> Boolean, private val changed: () -> Unit) : DefaultLifecycleObserver {
    var busy = false
        private set
    private var job: Job? = null
    private var progress: AlertDialog? = null
    private var generation = 0
    init {
        host.lifecycle.addObserver(this)
        UserManager.sessionRevision.observe(host) { cancel() }
        ShareFiles.prune(host)
    }
    fun start(id: Int) {
        if (busy || !available() || topic() != id) return
        val operation = ++generation
        busy = true; changed()
        val label = TextView(host).apply { text = host.getString(R.string.common_video_image_loading); setPadding(24,24,24,24) }
        progress = AlertDialog.Builder(host).setView(label).setCancelable(false).show()
        job = host.lifecycleScope.launch {
            var output: File? = null
            try {
                val content = PosterRepository(host).videoContent(id)
                val destination = ShareFiles.newFile(host,"mp4"); output = destination
                TopicShareVideoExporter(host.applicationContext).export(content,ShareVideoMusic.selected(host),destination) { percent ->
                    host.runOnUiThread { if (generation == operation) label.text = host.getString(R.string.common_video_generating, percent) }
                }
                if (generation != operation || topic() != id || !available() || !host.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@launch
                host.startActivity(Intent(host,TopicVideoPreviewActivity::class.java).apply {
                    putExtra("video_file",output!!.name); putExtra("topic_id",id); putExtra("snapshot",content.fingerprint)
                })
                output = null
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (generation == operation) host.notice(when {
                    e is PosterUnavailable -> e.message ?: host.getString(R.string.common_post_unavailable_short)
                    e.message in listOf(host.getString(R.string.common_post_changed),host.getString(R.string.common_video_timeout),host.getString(R.string.common_video_music_timeout)) -> e.message!!
                    else -> host.getString(R.string.common_video_generation_failed)
                })
            }
            catch (e: OutOfMemoryError) { if (generation == operation) host.notice(host.getString(R.string.common_video_memory_failed)) }
            finally { output?.delete(); if (generation == operation) { busy = false; progress?.dismiss(); progress = null; changed() } }
        }
    }
    fun cancel() { generation++; job?.cancel(); job = null; progress?.dismiss(); progress = null; busy = false; changed() }
    override fun onPause(owner: LifecycleOwner) { cancel() }
    override fun onDestroy(owner: LifecycleOwner) { cancel() }
}
