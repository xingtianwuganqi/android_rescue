package com.rescue.flutter_720yun.home.share

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.*
import java.io.File

object PosterGallery {
    /** API 29+ requires no storage permission. API 28 permission is requested by the page. */
    suspend fun save(context: Context,file: File,topic: Int) = withContext(Dispatchers.IO) {
        require(topic>0 && file.isFile && file.length()>0)
        val resolver=context.applicationContext.contentResolver
        val values=ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,"真命天喵_${topic}_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE,"image/png")
            if(Build.VERSION.SDK_INT>=29) {
                put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/真命天喵")
                put(MediaStore.Images.Media.IS_PENDING,1)
            }
        }
        ensureActive()
        val uri=resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values) ?: error("Cannot create media")
        try {
            resolver.openOutputStream(uri)?.use { output -> file.inputStream().use { input ->
                val bytes=ByteArray(8192)
                while(true) { ensureActive();val n=input.read(bytes);if(n<0) break;output.write(bytes,0,n) }
                output.flush()
            } } ?: error("Cannot write media")
            ensureActive()
            if(Build.VERSION.SDK_INT>=29) {
                val updated=resolver.update(uri,ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING,0) },null,null)
                check(updated>0)
            }
        } catch(error: Throwable) { runCatching { resolver.delete(uri,null,null) };throw error }
    }
}
