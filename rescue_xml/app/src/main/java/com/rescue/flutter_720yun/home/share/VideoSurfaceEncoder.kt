package com.rescue.flutter_720yun.home.share

import android.graphics.Bitmap
import android.media.*
import android.opengl.*
import android.view.Surface
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** EGL input surface avoids device-specific YUV layouts and preserves Canvas text/QR pixels. */
internal class VideoSurfaceEncoder(file: File) : AutoCloseable {
    private val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
    private val muxer = MediaMuxer(file.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var context: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var input: Surface? = null
    private var track = -1
    private var muxerStarted = false
    private var program = 0
    private var texture = 0
    private val vertices = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(floatArrayOf(-1f,-1f,0f,1f, 1f,-1f,1f,1f, -1f,1f,0f,0f, 1f,1f,1f,0f)); position(0)
    }
    init {
        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 720, 1280).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, VideoExportPlan.BIT_RATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30); setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            input = codec.createInputSurface(); codec.start()
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(EGL14.eglInitialize(display, IntArray(2), 0, IntArray(2), 0))
            val configs = arrayOfNulls<EGLConfig>(1)
            val attributes = intArrayOf(EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,
                EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,0x3142,1,EGL14.EGL_NONE)
            val count = IntArray(1)
            check(EGL14.eglChooseConfig(display, attributes,0,configs,0,1,count,0) && count[0] > 0)
            context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE),0)
            eglSurface = EGL14.eglCreateWindowSurface(display, configs[0], input, intArrayOf(EGL14.EGL_NONE),0)
            check(EGL14.eglMakeCurrent(display, eglSurface, eglSurface, context))
            fun shader(type: Int, source: String): Int {
                val id = GLES20.glCreateShader(type); GLES20.glShaderSource(id,source); GLES20.glCompileShader(id)
                val status = IntArray(1); GLES20.glGetShaderiv(id,GLES20.GL_COMPILE_STATUS,status,0)
                if (status[0] == 0) { GLES20.glDeleteShader(id); error("Shader compilation failed") }; return id
            }
            val vertex = shader(GLES20.GL_VERTEX_SHADER,"attribute vec2 p; attribute vec2 t; varying vec2 uv; void main(){gl_Position=vec4(p,0.0,1.0);uv=t;}")
            val fragment = shader(GLES20.GL_FRAGMENT_SHADER,"precision mediump float; varying vec2 uv; uniform sampler2D image; void main(){gl_FragColor=texture2D(image,uv);}")
            program = GLES20.glCreateProgram(); GLES20.glAttachShader(program,vertex); GLES20.glAttachShader(program,fragment); GLES20.glLinkProgram(program)
            GLES20.glDeleteShader(vertex); GLES20.glDeleteShader(fragment)
            val status = IntArray(1); GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,status,0); check(status[0] != 0)
            val ids = IntArray(1); GLES20.glGenTextures(1,ids,0); texture = ids[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE)
        } catch (e: Throwable) { close(); throw e }
    }
    fun frame(bitmap: Bitmap, index: Int, checkpoint: () -> Unit) {
        drain(false,checkpoint)
        GLES20.glViewport(0,0,720,1280); GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0); GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bitmap,0)
        val p = GLES20.glGetAttribLocation(program,"p"); val t = GLES20.glGetAttribLocation(program,"t")
        vertices.position(0); GLES20.glVertexAttribPointer(p,2,GLES20.GL_FLOAT,false,16,vertices); GLES20.glEnableVertexAttribArray(p)
        vertices.position(2); GLES20.glVertexAttribPointer(t,2,GLES20.GL_FLOAT,false,16,vertices); GLES20.glEnableVertexAttribArray(t)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"image"),0); GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4)
        check(GLES20.glGetError() == GLES20.GL_NO_ERROR)
        check(EGLExt.eglPresentationTimeANDROID(display,eglSurface,index * 1_000_000_000L / 30))
        check(EGL14.eglSwapBuffers(display,eglSurface))
    }
    fun finish(checkpoint: () -> Unit) { codec.signalEndOfInputStream(); drain(true,checkpoint); check(muxerStarted); muxer.stop(); muxerStarted = false }
    private fun drain(end: Boolean, checkpoint: () -> Unit) {
        val info = MediaCodec.BufferInfo()
        while (true) {
            checkpoint()
            when (val index = codec.dequeueOutputBuffer(info, if (end) 10_000 else 0)) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> if (!end) return
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    check(!muxerStarted); track = muxer.addTrack(codec.outputFormat); muxer.start(); muxerStarted = true
                }
                else -> if (index >= 0) {
                    try {
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.size > 0) {
                            check(muxerStarted)
                            val buffer = codec.getOutputBuffer(index)!!; buffer.position(info.offset); buffer.limit(info.offset + info.size)
                            muxer.writeSampleData(track,buffer,info)
                        }
                    } finally { codec.releaseOutputBuffer(index,false) }
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
        }
    }
    override fun close() {
        if (display != EGL14.EGL_NO_DISPLAY) {
            if (program != 0) GLES20.glDeleteProgram(program)
            if (texture != 0) GLES20.glDeleteTextures(1,intArrayOf(texture),0)
            EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT)
            if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display,eglSurface)
            if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display,context)
            EGL14.eglReleaseThread(); EGL14.eglTerminate(display); display = EGL14.EGL_NO_DISPLAY
        }
        input?.release(); input = null
        runCatching { codec.stop() }; runCatching { codec.release() }
        if (muxerStarted) runCatching { muxer.stop() }
        runCatching { muxer.release() }
    }
}
