package com.rescue.flutter_720yun.home.share

import android.content.*
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object ShareFiles {
    fun directory(context: Context) = File(context.cacheDir,"share-exports").apply { mkdirs() }
    fun newFile(context: Context, extension: String) = File(directory(context),"topic-${UUID.randomUUID()}.$extension")
    fun prune(context: Context) {
        val deadline = System.currentTimeMillis() - 24*60*60*1000L
        directory(context).listFiles()?.filter { it.lastModified() < deadline }?.forEach { it.delete() }
        context.cacheDir.listFiles()?.filter { it.name.startsWith("video-work-") && it.lastModified() < deadline }?.forEach { it.deleteRecursively() }
    }
    fun intent(context: Context, file: File, mime: String, text: String? = null): Intent {
        val uri = FileProvider.getUriForFile(context,context.packageName+".sharefiles",file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime; putExtra(Intent.EXTRA_STREAM,uri)
            text?.let { putExtra(Intent.EXTRA_TEXT,it) }
            clipData = ClipData.newUri(context.contentResolver,"分享文件",uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send,if (mime == "video/mp4") "分享视频" else "分享链接")
    }
    fun send(context: Context, file: File, mime: String, text: String? = null) { context.startActivity(intent(context,file,mime,text)) }
}
