package com.rescue.flutter_720yun.home.share

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.rescue.flutter_720yun.BuildConfig
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adoption.ui.notice
import com.rescue.flutter_720yun.util.UserManager
import kotlinx.coroutines.*
import java.io.File

class TopicShareFlow(private val host: androidx.fragment.app.FragmentActivity,private val topic: ()->Int?,
    private val available: ()->Boolean,private val busyChanged: ()->Unit): DefaultLifecycleObserver {
    private val repository=PosterRepository(host)
    private var generation=0L
    private var job: Job?=null
    private var progress: AlertDialog?=null
    private var menu: android.app.Dialog?=null
    private var output: File?=null
    private var bound: PosterContent?=null
    private var permissionPending=false
    private var saveOnResume=false
    private var imageBusy=false
    private val video = TopicVideoExportFlow(host,topic,available,busyChanged)
    val busy get() = imageBusy || video.busy
    private val permission=host.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionPending=false;setBusy(false)
        if(output==null) return@registerForActivityResult
        if(granted && output!=null) {
            saveOnResume=true
            if(host.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) resumeSave()
        } else {
            host.notice(host.getString(R.string.common_save_failed))
            clearPoster()
        }
    }
    init {
        host.lifecycle.addObserver(this)
        UserManager.sessionRevision.observe(host) { cancel() }
    }
    private fun setBusy(value: Boolean) { imageBusy=value;busyChanged() }
    fun showMenu() {
        val id=topic()?.takeIf { it>0 } ?: run { host.notice(host.getString(R.string.common_post_parameter_invalid));return }
        if(busy || !available()) { host.notice(host.getString(R.string.common_post_unavailable));return }
        menu=TopicShareSheet.show(host) { index ->
            menu=null
            if(topic()==id && available() && !busy) when(index) {
                0 -> {
                    com.rescue.flutter_720yun.analytics.AnalyticsManager.track("export_video",properties=mapOf("publish_type" to "adoption","object_id" to id.toLong()),interactive=true)
                    video.start(id)
                }
                1 -> {
                    com.rescue.flutter_720yun.analytics.AnalyticsManager.track("export_image",properties=mapOf("publish_type" to "adoption","object_id" to id.toLong()),interactive=true)
                    generate(id)
                }
                2 -> shareLink(id)
                3 -> {
                    val url=PosterContent.shareLink(BuildConfig.API_BASE_URL,id)
                    (host.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(host.getString(R.string.common_topic_link),url))
                    host.notice(host.getString(R.string.copy_success))
                }
            }
        }
    }
    private fun shareLink(id: Int) {
        if(topic()!=id || !available()) { host.notice(host.getString(R.string.common_post_unavailable));return }
        val url=PosterContent.shareLink(BuildConfig.API_BASE_URL,id)
        try {
            host.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type="text/plain"
                putExtra(Intent.EXTRA_TEXT,url)
            },host.getString(R.string.common_share_link)))
        } catch(e: Exception) { host.notice(host.getString(R.string.common_share_failed)) }
    }

    private fun current(operation: Long,id: Int)=generation==operation && topic()==id && available() &&
        host.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    private fun showProgress(message: String) {
        val row=LinearLayout(host).apply { orientation=LinearLayout.HORIZONTAL;setPadding(32,24,32,24)
            addView(ProgressBar(host));addView(TextView(host).apply { text=message;setPadding(24,0,0,0) }) }
        progress=AlertDialog.Builder(host).setView(row).setCancelable(false).show()
    }
    private fun generate(id: Int) {
        val includeQr=true
        if(busy) return
        cancel();val operation=++generation;setBusy(true);showProgress(host.getString(R.string.common_image_generating))
        job=host.lifecycleScope.launch {
            var file: File?=null
            try {
                val initial=repository.content(id,includeQr)
                val generated=File(host.cacheDir,"topic-poster-${java.util.UUID.randomUUID()}.png")
                file=generated
                val latest=repository.withImage(initial.photoUrl) { photo ->
                    // Recheck availability/status after the download. Never reuse a changed image.
                    val verified=repository.content(id,includeQr)
                    if(verified.photoUrl!=initial.photoUrl) throw PosterUnavailable(host.getString(R.string.common_image_generation_failed))
                    withContext(Dispatchers.Default) {
                        val bitmap=TopicSharePosterRenderer(host.applicationContext).render(verified,photo)
                        try {
                            generated.outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)) }
                            ensureActive()
                        } catch(error: Throwable) { generated.delete();throw error }
                        finally { bitmap.recycle() }
                    }
                    verified
                }
                // The repository releases the decoded source before this main-thread UI handoff.
                if(!current(operation,id)) return@launch
                output=generated;file=null;bound=latest
                progress?.dismiss();progress=null
                if(Build.VERSION.SDK_INT<29 && ContextCompat.checkSelfPermission(host,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED) {
                    permissionPending=true
                    try { permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) }
                    catch(e: Exception) { permissionPending=false;clearPoster();host.notice(host.getString(R.string.common_save_failed)) }
                } else {
                    try { writePoster(generated,latest,operation) }
                    catch(e: CancellationException) { throw e }
                    catch(e: Exception) { if(current(operation,id)) host.notice(host.getString(R.string.common_save_failed)) }
                    finally { clearPoster() }
                }
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { if(current(operation,id)) host.notice((e as? PosterUnavailable)?.message ?: host.getString(R.string.common_image_generation_failed)) }
            catch(e: OutOfMemoryError) { if(current(operation,id)) host.notice(host.getString(R.string.common_image_generation_failed)) }
            finally {
                file?.delete()
                if(operation==generation) { progress?.dismiss();progress=null;if(!permissionPending) setBusy(false) }
            }
        }
    }
    private fun resumeSave() {
        if(!saveOnResume) return
        saveOnResume=false
        if(output!=null && available()) save() else { clearPoster();host.notice(host.getString(R.string.common_save_failed)) }
    }
    private fun save() {
        val file=output ?: return
        val content=bound ?: return
        if(busy) return
        val operation=generation;setBusy(true)
        job=host.lifecycleScope.launch {
            try {
                writePoster(file,content,operation)
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { if(current(operation,content.topicId)) host.notice(host.getString(R.string.common_save_failed)) }
            finally { if(operation==generation) { clearPoster();setBusy(false) } }
        }
    }
    private fun clearPoster() { output?.delete();output=null;bound=null }
    private suspend fun writePoster(file: File,content: PosterContent,operation: Long) {
        val latest=repository.content(content.topicId,content.includeQr)
        if(latest!=content || !current(operation,content.topicId)) throw PosterUnavailable(host.getString(R.string.common_image_generation_failed))
        PosterGallery.save(host.applicationContext,file,content.topicId)
        if(current(operation,content.topicId)) host.notice(host.getString(R.string.common_save_success_gallery))
    }
    fun cancel() {
        video.cancel()
        generation++;permissionPending=false;saveOnResume=false;job?.cancel();job=null
        progress?.dismiss();progress=null;menu?.dismiss();menu=null
        output?.delete();output=null;bound=null;setBusy(false)
    }
    override fun onResume(owner: LifecycleOwner) { resumeSave() }
    override fun onPause(owner: LifecycleOwner) { if(!permissionPending) cancel() }
    // A permission overlay may pause the Activity; a real background transition must discard it.
    override fun onStop(owner: LifecycleOwner) { if(permissionPending) cancel() }
    override fun onDestroy(owner: LifecycleOwner) { cancel() }
}
