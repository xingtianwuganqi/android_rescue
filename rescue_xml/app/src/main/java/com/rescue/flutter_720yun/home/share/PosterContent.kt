package com.rescue.flutter_720yun.home.share

import java.net.URI
import java.util.regex.Pattern

/** Public-only poster input. No HomeListModel/profile/contact travels into the renderer. */
data class PosterContent(val topicId: Int, val description: String, val photoUrl: String,
    val workflow: String?, val legacyComplete: Boolean, val includeQr: Boolean) {
    init { require(topicId>0 && photoUrl.isNotBlank()) }
    val statusText: String? get()=status(workflow,legacyComplete)
    companion object {
        fun status(workflow: String?,complete: Boolean): String?=when(workflow) {
            "open" -> "送养中"
            "adopted" -> "送养已结束 · 已完成送养"
            "closed" -> "送养已结束 · 已关闭送养"
            else -> if(complete) "送养已结束" else null
        }
        fun introduction(tags: List<String>,body: String?): String =
            listOf(tags.map(String::trim).filter(String::isNotEmpty).distinct().joinToString(" · "),body.orEmpty().trim())
                .filter(String::isNotEmpty).joinToString("\n\n")
        fun imageUrl(images: List<String>?): String? = images.orEmpty().asSequence().map(String::trim)
            .filter(String::isNotEmpty).mapNotNull { value ->
                val url=if(value.startsWith("http://") || value.startsWith("https://")) value
                    else if(!value.contains(":") && !value.startsWith("//")) "http://img.rxswift.cn/"+value.trimStart('/') else return@mapNotNull null
                runCatching { URI(url) }.getOrNull()?.takeIf { it.scheme in listOf("http","https") && !it.host.isNullOrBlank() }?.toASCIIString()
            }.firstOrNull()
        fun shareLink(base: String,id: Int): String {
            require(id>0)
            val root=URI(base.trimEnd('/')+"/")
            require(root.scheme in listOf("http","https") && !root.host.isNullOrBlank())
            return root.resolve("api/topic/info/$id/").toASCIIString()
        }
    }
}

object PosterText {
    data class Preview(val text: String,val truncated: Boolean)
    private val cluster=Pattern.compile("\\X")
    /** Search only whole extended grapheme clusters, including ZWJ emoji and flag pairs. */
    fun preview(source: String,fits: (String)->Boolean): Preview {
        val text=source.trim()
        if(text.isEmpty()) return Preview("",false)
        val matcher=cluster.matcher(text)
        val boundaries=mutableListOf(0)
        while(matcher.find() && boundaries.size<=4096) boundaries.add(matcher.end())
        val complete=boundaries.last()==text.length
        if(complete && fits(text)) return Preview(text,false)
        var lo=0;var hi=boundaries.lastIndex
        while(lo<hi) {
            val mid=(lo+hi+1)/2
            if(fits(text.substring(0,boundaries[mid]).trimEnd()+"…")) lo=mid else hi=mid-1
        }
        return Preview(text.substring(0,boundaries[lo]).trimEnd()+"…",true)
    }
}
