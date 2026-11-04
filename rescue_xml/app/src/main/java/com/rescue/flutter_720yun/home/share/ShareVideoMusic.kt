package com.rescue.flutter_720yun.home.share

import android.content.Context
import android.media.*
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Same bundled CC0 choices and persisted default as iOS. */
enum class ShareVideoMusic(val asset: String?, val title: String, val detail: String) {
    HAPPY("happy_whistling_ukulele", "快乐口哨", "Happy Whistling Ukulele · 口哨与尤克里里"),
    CITY("city_sunshine", "阳光漫步", "City Sunshine · 明亮轻快"),
    UKULELE("ukulele_song", "轻快尤克里里", "Ukulele Song · 轻松温暖"),
    NONE(null, "无音乐", "导出不含声音的视频");
    companion object {
        fun selected(context: Context) = values().firstOrNull { it.name == context.getSharedPreferences("share_video",0).getString("music",null) } ?: HAPPY
        fun select(context: Context, music: ShareVideoMusic) { context.getSharedPreferences("share_video",0).edit().putString("music",music.name).apply() }
    }
}

internal object ShareVideoAudio {
    /** Decode to disk, loop PCM samples, apply 45% gain and .7s/1.2s ramps, then encode AAC. */
    fun create(context: Context, music: ShareVideoMusic, directory: File, duration: Double, checkpoint: () -> Unit): File {
        val source = File(directory,"music.m4a")
        context.assets.open("ShareMusic/${music.asset}.m4a").use { input -> source.outputStream().use { input.copyTo(it) } }
        val pcm = File(directory,"music.pcm")
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var sampleRate = 0; var channels = 0
        try {
            extractor.setDataSource(source.path)
            val track = (0 until extractor.trackCount).first { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
            extractor.selectTrack(track); val format = extractor.getTrackFormat(track)
            val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!); decoder = codec
            codec.configure(format,null,null,0); codec.start()
            var inputEnded = false; var outputEnded = false; val info = MediaCodec.BufferInfo()
            pcm.outputStream().use { out ->
                while (!outputEnded) {
                    checkpoint()
                    if (!inputEnded) {
                        val index = codec.dequeueInputBuffer(10_000)
                        if (index >= 0) {
                            val buffer = codec.getInputBuffer(index)!!; buffer.clear()
                            val size = extractor.readSampleData(buffer,0)
                            if (size < 0) { codec.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputEnded = true }
                            else { codec.queueInputBuffer(index,0,size,extractor.sampleTime,0); extractor.advance() }
                        }
                    }
                    when (val index = codec.dequeueOutputBuffer(info,10_000)) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val output = codec.outputFormat
                            sampleRate = output.getInteger(MediaFormat.KEY_SAMPLE_RATE); channels = output.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            check(!output.containsKey(MediaFormat.KEY_PCM_ENCODING) || output.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_16BIT)
                        }
                        else -> if (index >= 0) {
                            try {
                                if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                                    val buffer = codec.getOutputBuffer(index)!!; buffer.position(info.offset); buffer.limit(info.offset+info.size)
                                    val bytes = ByteArray(info.size); buffer.get(bytes); out.write(bytes)
                                }
                                outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            } finally { codec.releaseOutputBuffer(index,false) }
                        }
                    }
                }
            }
        } finally { extractor.release(); decoder?.let { runCatching { it.stop() }; it.release() } }
        check(sampleRate > 0 && channels in 1..2 && pcm.length() > 0 && pcm.length() % (channels * 2) == 0L)
        val output = File(directory,"audio.m4a")
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val muxer = MediaMuxer(output.path,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var started = false
        try {
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,sampleRate,channels).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE,MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE,128_000); setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,16384)
            }
            encoder.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE); encoder.start()
            val total = (duration * sampleRate).toLong(); var frame = 0L; var inputEnded = false; var outputEnded = false
            var audioTrack = -1; val info = MediaCodec.BufferInfo()
            RandomAccessFile(pcm,"r").use { input ->
                while (!outputEnded) {
                    checkpoint()
                    if (!inputEnded) {
                        val index = encoder.dequeueInputBuffer(10_000)
                        if (index >= 0) {
                            val buffer = encoder.getInputBuffer(index)!!.order(ByteOrder.LITTLE_ENDIAN); buffer.clear()
                            val frames = minOf(buffer.remaining()/(channels*2),1024,(total-frame).toInt())
                            if (frames == 0) { encoder.queueInputBuffer(index,0,0,frame*1_000_000/sampleRate,MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputEnded = true }
                            else {
                                val bytes = ByteArray(frames * channels * 2)
                                var filled = 0
                                while (filled < bytes.size) {
                                    if (input.filePointer == input.length()) input.seek(0)
                                    val count = minOf((input.length() - input.filePointer).toInt(), bytes.size - filled)
                                    check(count > 0); input.readFully(bytes,filled,count); filled += count
                                }
                                val samples = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                                repeat(frames) { offset ->
                                    val gain = VideoExportPlan.musicGain((frame+offset).toDouble()/sampleRate,duration)
                                    repeat(channels) { buffer.putShort((samples.short * gain).toInt().coerceIn(-32768,32767).toShort()) }
                                }
                                encoder.queueInputBuffer(index,0,buffer.position(),frame*1_000_000/sampleRate,0); frame += frames
                            }
                        }
                    }
                    when (val index = encoder.dequeueOutputBuffer(info,10_000)) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { check(!started); audioTrack = muxer.addTrack(encoder.outputFormat); muxer.start(); started = true }
                        else -> if (index >= 0) {
                            try {
                                if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.presentationTimeUs >= 0) {
                                    val buffer = encoder.getOutputBuffer(index)!!; buffer.position(info.offset); buffer.limit(info.offset+info.size)
                                    muxer.writeSampleData(audioTrack,buffer,info)
                                }
                                outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            } finally { encoder.releaseOutputBuffer(index,false) }
                        }
                    }
                }
            }
            check(started); muxer.stop(); started = false
        } finally {
            runCatching { encoder.stop() }; encoder.release()
            if (started) runCatching { muxer.stop() }; muxer.release(); pcm.delete(); source.delete()
        }
        return output
    }
    fun mux(video: File, audio: File, output: File, durationUs: Long, checkpoint: () -> Unit) {
        val muxer = MediaMuxer(output.path,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val extractors = mutableListOf<MediaExtractor>(); var started = false
        try {
            val tracks = listOf(video,audio).map { source ->
                val extractor = MediaExtractor(); extractors += extractor; extractor.setDataSource(source.path)
                val format = extractor.getTrackFormat(0); extractor.selectTrack(0)
                extractor to muxer.addTrack(format)
            }
            muxer.start(); started = true
            val buffer = ByteBuffer.allocateDirect(2*1024*1024); val info = MediaCodec.BufferInfo()
            for ((extractor,track) in tracks) {
                while (true) {
                    checkpoint(); buffer.clear(); val size = extractor.readSampleData(buffer,0)
                    if (size < 0 || extractor.sampleTime >= durationUs) break
                    info.set(0,size,extractor.sampleTime,if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                    muxer.writeSampleData(track,buffer,info); extractor.advance()
                }
            }
            muxer.stop(); started = false
        } finally { extractors.forEach { it.release() }; if (started) runCatching { muxer.stop() }; muxer.release() }
    }
}
