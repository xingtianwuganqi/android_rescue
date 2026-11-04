package com.rescue.flutter_720yun.home.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.rescue.flutter_720yun.adoption.repository.*
import com.rescue.flutter_720yun.network.*
import com.rescue.flutter_720yun.util.UserManager
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class PosterUnavailable(message: String): Exception(message)
class PosterRepository(context: Context) {
    private val context=context.applicationContext
    private val service=ServiceCreator.create<HomeService>()
    private val client=OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).callTimeout(30,TimeUnit.SECONDS).build()
    suspend fun content(topic: Int,includeQr: Boolean): PosterContent = snapshot(topic, includeQr, false).poster
    suspend fun linkContent(topic: Int): VideoContent = snapshot(topic, false, false)
    suspend fun videoContent(topic: Int): VideoContent = snapshot(topic, true, true)
    private suspend fun snapshot(topic: Int,includeQr: Boolean,allPhotos: Boolean): VideoContent {
        if(topic<=0) throw PosterUnavailable("帖子参数无效")
        val session=UserManager.sessionRevision.value
        val params=mutableMapOf<String,Any?>("topic_id" to topic)
        UserManager.token?.let { params["token"]=it }
        val response=service.topicPreview(params)
        if(session!=UserManager.sessionRevision.value) throw CancellationException("Account changed")
        val model=response.body()?.data
        if(response.code()!=200 || response.body()?.code!=200 || model?.topic_id!=topic || model.is_delete==0 || model.unavailable)
            throw PosterUnavailable("帖子已删除或暂不可用")
        val photo=PosterContent.imageUrl(model.imgs) ?: throw PosterUnavailable(if(allPhotos) "帖子暂无可导出的视频图片" else "帖子暂无可导出的图片")
        val workflow=if(UserManager.isLogin) {
            try { withContext(Dispatchers.Main.immediate) { AdoptionRepository().state(topic).workflow_status } }
            catch(e: CancellationException) { throw e }
            catch(e: AdoptionError) { if(e.status in listOf(401,403,404)) throw PosterUnavailable("帖子已删除或暂不可用") else null }
            catch(e: Exception) { null }
        } else model.workflow_status
        if(session!=UserManager.sessionRevision.value) throw CancellationException("Account changed")
        val poster = PosterContent(topic,PosterContent.introduction(model.tagInfos.orEmpty().mapNotNull { it.tag_name },model.content),
            photo,workflow,model.is_complete==true,includeQr)
        val paths = model.imgs.orEmpty().map(String::trim).filter(String::isNotEmpty)
        val photos = if (!allPhotos) listOf(photo) else paths.map { PosterContent.imageUrl(listOf(it)) ?: throw PosterUnavailable("图片加载失败，请检查网络后重试") }
        return VideoContent(poster, photos, model.tagInfos.orEmpty().mapNotNull { it.tag_name?.trim()?.takeIf(String::isNotEmpty) }, model.address_info.orEmpty().trim())
    }
    suspend fun <T> withImage(url: String, maxEdge: Int? = null, render: suspend (Bitmap)->T): T = withContext(Dispatchers.IO) {
        val temp=File.createTempFile("poster-photo-", ".img",context.cacheDir)
        val call=client.newCall(Request.Builder().url(url).build())
        val canceller=launch(Dispatchers.Default,start=CoroutineStart.UNDISPATCHED) {
            try { awaitCancellation() } finally { call.cancel() }
        }
        try {
            call.execute().use { response ->
                if(!response.isSuccessful) throw PosterUnavailable("图片生成失败")
                val body=response.body ?: throw PosterUnavailable("图片生成失败")
                if(body.contentLength()>16*1024*1024) throw PosterUnavailable("图片生成失败")
                body.byteStream().use { source -> temp.outputStream().use { out ->
                    val buffer=ByteArray(8192);var bytes=0
                    while(true) {
                        ensureActive();val count=source.read(buffer);if(count<0) break
                        bytes+=count;if(bytes>16*1024*1024) throw PosterUnavailable("图片生成失败")
                        out.write(buffer,0,count)
                    }
                } }
            }
            val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
            BitmapFactory.decodeFile(temp.path,bounds)
            if(bounds.outWidth<=0 || bounds.outHeight<=0) throw PosterUnavailable("图片生成失败")
            var sample=1
            while(bounds.outWidth/sample>2100 || bounds.outHeight/sample>6000 ||
                (bounds.outWidth.toLong()/sample)*(bounds.outHeight/sample)>4_000_000) sample*=2
            ensureActive()
            val bitmap = if (maxEdge != null) android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(temp)) { decoder, info, _ ->
                val scale = minOf(1.0, maxEdge.toDouble() / maxOf(info.size.width, info.size.height))
                decoder.setTargetSize(maxOf(1, (info.size.width * scale).toInt()), maxOf(1, (info.size.height * scale).toInt()))
                decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
            } else BitmapFactory.decodeFile(temp.path,BitmapFactory.Options().apply { inSampleSize=sample;inPreferredConfig=Bitmap.Config.ARGB_8888 })
                ?: throw PosterUnavailable("图片生成失败")
            // Keep ownership inside this scope: prompt cancellation cannot strand a Bitmap between dispatchers.
            try { ensureActive();render(bitmap) } finally { bitmap.recycle() }
        } finally { canceller.cancel();call.cancel();temp.delete() }
    }
}
