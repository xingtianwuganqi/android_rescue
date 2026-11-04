package com.rescue.flutter_720yun.home.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import kotlinx.coroutines.*
import java.io.File
import java.util.UUID

class TopicShareVideoExporter(private val context: Context) {
    suspend fun export(content: VideoContent, music: ShareVideoMusic, output: File, progress: (Int) -> Unit): File = withContext(Dispatchers.Default) {
        val work = File(context.cacheDir,"video-work-${UUID.randomUUID()}").apply { check(mkdirs()) }
        var successful = false
        try {
            val repository = PosterRepository(context)
            val photos = content.photos.mapIndexed { index, url ->
                ensureActive()
                val file = File(work,"photo-$index.png")
                try {
                    repository.withImage(url,1280) { bitmap -> file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) } }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { throw PosterUnavailable("图片加载失败，请检查网络后重试") }
                file
            }
            check(repository.videoContent(content.poster.topicId) == content) { "帖子信息已变化，请重新导出" }
            val slides = VideoExportPlan.slides(photos.size,content.poster.topicId)
            val total = slides.sumOf { it.frames }; val duration = total.toDouble()/30
            val video = File(work,"silent.mp4")
            val deadline = SystemClock.elapsedRealtime() + 120_000
            val operation = currentCoroutineContext()
            val checkpoint = { operation.ensureActive(); check(SystemClock.elapsedRealtime() < deadline) { "视频生成超时，请重试" } }
            val frame = Bitmap.createBitmap(720,1280,Bitmap.Config.ARGB_8888)
            var previous: Bitmap? = null
            try {
                TopicShareVideoFrames(context,content).use { renderer ->
                    VideoSurfaceEncoder(video).use { encoder ->
                        var frameIndex = 0
                        for (slide in slides) {
                            checkpoint()
                            val photo = slide.photoIndex?.let { BitmapFactory.decodeFile(photos[it].path) ?: error("图片加载失败") }
                            var base: Bitmap? = null
                            try {
                                base = renderer.base(slide,photo)
                                repeat(slide.frames) { local ->
                                    checkpoint()
                                    renderer.drawFrame(frame,base,previous,photo,slide.photoIndex ?: 0,photos.size,local,slide.frames)
                                    encoder.frame(frame,frameIndex,checkpoint); frameIndex++
                                    if (frameIndex % 30 == 0) progress((frameIndex.toDouble()/total * if (music == ShareVideoMusic.NONE) 99 else 80).toInt())
                                }
                                previous?.recycle(); previous = frame.copy(Bitmap.Config.ARGB_8888,false)
                            } finally { base?.recycle(); photo?.recycle() }
                        }
                        encoder.finish(checkpoint)
                    }
                }
            } finally { previous?.recycle(); frame.recycle() }
            if (music == ShareVideoMusic.NONE) check(video.renameTo(output))
            else {
                val audioDeadline = SystemClock.elapsedRealtime() + 120_000
                val audioCheck = { operation.ensureActive(); check(SystemClock.elapsedRealtime() < audioDeadline) { "视频配乐超时，请重试" } }
                progress(85)
                val audio = ShareVideoAudio.create(context,music,work,duration,audioCheck)
                progress(95)
                ShareVideoAudio.mux(video,audio,output,(duration*1_000_000).toLong(),audioCheck)
            }
            ensureActive()
            check(repository.videoContent(content.poster.topicId) == content) { "帖子信息已变化，请重新导出" }
            successful = true; progress(100); output
        } finally { work.deleteRecursively(); if (!successful) output.delete() }
    }
}
