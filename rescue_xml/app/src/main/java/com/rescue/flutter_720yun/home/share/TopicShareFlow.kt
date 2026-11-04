package com.rescue.flutter_720yun.home.share

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.bumptech.glide.Glide
import com.rescue.flutter_720yun.BuildConfig
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
    private var preview: AlertDialog?=null
    private var menu: AlertDialog?=null
    private var output: File?=null
    private var bound: PosterContent?=null
    private var permissionPending=false
    private var saveOnResume=false
    var busy=false
        private set
    private val permission=host.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionPending=false;setBusy(false)
        preview?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled=true
        if(granted && output!=null) {
            saveOnResume=true
            if(host.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) resumeSave()
        } else host.notice("保存失败")
    }
    init {
        host.lifecycle.addObserver(this)
        UserManager.sessionRevision.observe(host) { cancel() }
    }
    private fun setBusy(value: Boolean) { busy=value;busyChanged() }
    fun showMenu() {
        val id=topic()?.takeIf { it>0 } ?: run { host.notice("帖子参数无效");return }
        if(busy || !available()) { host.notice("帖子已删除或暂不可用");return }
        val choices=mutableListOf("生成海报","分享链接","复制链接")
        if(BuildConfig.DEBUG) choices.add("生成无二维码海报（调试）")
        menu=AlertDialog.Builder(host).setTitle("分享帖子").setItems(choices.toTypedArray()) { dialog,index ->
            dialog.dismiss();menu=null
            if(topic()!=id || !available()) return@setItems
            when(index) {
                0 -> {
                    com.rescue.flutter_720yun.analytics.AnalyticsManager.track("export_image","topic_detail",mapOf("publish_type" to "adoption","object_id" to id.toLong()),interactive=true)
                    generate(id,true)
                }
                1 -> {
                    val url=PosterContent.shareLink(BuildConfig.API_BASE_URL,id)
                    host.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type="text/plain";putExtra(Intent.EXTRA_TEXT,url)
                    },"分享链接"))
                }
                2 -> {
                    val url=PosterContent.shareLink(BuildConfig.API_BASE_URL,id)
                    (host.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("帖子链接",url))
                    host.notice("已复制链接")
                }
                else -> generate(id,false)
            }
        }.setNegativeButton("取消",null).show()
    }
    private fun current(operation: Long,id: Int)=generation==operation && topic()==id && available() &&
        host.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    private fun showProgress(message: String) {
        val row=LinearLayout(host).apply { orientation=LinearLayout.HORIZONTAL;setPadding(32,24,32,24)
            addView(ProgressBar(host));addView(TextView(host).apply { text=message;setPadding(24,0,0,0) }) }
        progress=AlertDialog.Builder(host).setView(row).setCancelable(false).show()
    }
    private fun generate(id: Int,includeQr: Boolean) {
        if(busy) return
        cancel();val operation=++generation;setBusy(true);showProgress("正在生成图片…")
        job=host.lifecycleScope.launch {
            var file: File?=null
            try {
                val initial=repository.content(id,includeQr)
                val generated=File(host.cacheDir,"topic-poster-${java.util.UUID.randomUUID()}.png")
                file=generated
                val latest=repository.withImage(initial.photoUrl) { photo ->
                    // Recheck availability/status after the download. Never reuse a changed image.
                    val verified=repository.content(id,includeQr)
                    if(verified.photoUrl!=initial.photoUrl) throw PosterUnavailable("图片生成失败")
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
                progress?.dismiss();progress=null;setBusy(false);showPreview()
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { if(current(operation,id)) host.notice((e as? PosterUnavailable)?.message ?: "图片生成失败") }
            catch(e: OutOfMemoryError) { if(current(operation,id)) host.notice("图片生成失败") }
            finally {
                file?.delete()
                if(operation==generation) { progress?.dismiss();progress=null;setBusy(false) }
            }
        }
    }
    private fun showPreview() {
        val image=ImageView(host).apply { adjustViewBounds=true;scaleType=ImageView.ScaleType.FIT_CENTER }
        val scroll=ScrollView(host).apply { addView(image,ViewGroup.LayoutParams(-1,-2)) }
        Glide.with(image).load(output).into(image)
        preview=AlertDialog.Builder(host).setTitle("分享海报").setView(scroll).setNegativeButton("取消",null)
            .setPositiveButton("保存到相册",null).create().apply {
                setOnDismissListener {
                    generation++;permissionPending=false;saveOnResume=false;job?.cancel();job=null;setBusy(false)
                    Glide.with(image).clear(image);output?.delete();output=null;bound=null
                }
                show()
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    if(busy || permissionPending) return@setOnClickListener
                    if(Build.VERSION.SDK_INT<29 && ContextCompat.checkSelfPermission(host,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED) {
                        permissionPending=true;setBusy(true);getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=false
                        try { permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) }
                        catch(e: Exception) { permissionPending=false;setBusy(false);getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=true;host.notice("保存失败") }
                    } else save()
                }
            }
    }
    private fun resumeSave() {
        if(!saveOnResume) return
        saveOnResume=false
        if(output!=null && available()) save() else host.notice("保存失败")
    }
    private fun save() {
        val file=output ?: return
        val content=bound ?: return
        if(busy) return
        val operation=generation;setBusy(true)
        preview?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled=false
        job=host.lifecycleScope.launch {
            try {
                val latest=repository.content(content.topicId,content.includeQr)
                if(latest!=content || !current(operation,content.topicId)) throw PosterUnavailable("图片生成失败")
                PosterGallery.save(host.applicationContext,file,content.topicId)
                if(current(operation,content.topicId)) { host.notice("成功保存到相册");preview?.dismiss();preview=null }
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { if(current(operation,content.topicId)) host.notice("保存失败") }
            finally { if(operation==generation) { setBusy(false);preview?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled=true } }
        }
    }
    fun cancel() {
        generation++;permissionPending=false;saveOnResume=false;job?.cancel();job=null
        progress?.dismiss();progress=null;preview?.dismiss();preview=null;menu?.dismiss();menu=null
        output?.delete();output=null;bound=null;setBusy(false)
    }
    override fun onResume(owner: LifecycleOwner) { resumeSave() }
    override fun onPause(owner: LifecycleOwner) { if(!permissionPending) cancel() }
    // A permission overlay may pause the Activity; a real background transition must discard it.
    override fun onStop(owner: LifecycleOwner) { if(permissionPending) cancel() }
    override fun onDestroy(owner: LifecycleOwner) { cancel() }
}
