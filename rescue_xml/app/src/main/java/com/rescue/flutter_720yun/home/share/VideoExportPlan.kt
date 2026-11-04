package com.rescue.flutter_720yun.home.share

/** iOS export timing: photos, introduction, search card, download card. */
object VideoExportPlan {
    const val WIDTH = 720
    const val HEIGHT = 1280
    const val FPS = 30
    const val BIT_RATE = 2_800_000
    const val TRANSITION_SECONDS = 0.35
    data class Slide(val photoIndex: Int? = null, val kind: String, val frames: Int)
    fun slides(photoCount: Int, topic: Int): List<Slide> {
        require(photoCount > 0)
        return (0 until photoCount).map { Slide(it, "photo", 3 * FPS) } +
            listOf(Slide(kind = "introduction", frames = 5 * FPS)) +
            (if (topic > 0) listOf(Slide(kind = "search", frames = 2 * FPS)) else emptyList()) +
            listOf(Slide(kind = "download", frames = (if (topic > 0) 3 else 5) * FPS))
    }
    fun musicGain(seconds: Double, duration: Double): Double {
        require(duration > 0)
        val fadeIn = minOf(0.7, duration / 4)
        val fadeOut = minOf(1.2, duration / 4)
        return 0.45 * minOf((seconds / fadeIn).coerceIn(0.0, 1.0),
            ((duration - seconds) / fadeOut).coerceIn(0.0, 1.0))
    }
}

data class VideoContent(val poster: PosterContent, val photos: List<String>, val tags: List<String>, val location: String) {
    init { require(photos.isNotEmpty() && photos.none(String::isBlank)) }
    val bodyDescription: String get() {
        val prefix = tags.joinToString(" · ")
        return poster.description.removePrefix(prefix + "\n\n").let { if (it == prefix) "" else it }.trim()
    }
    val fingerprint: String get() {
        val fields = listOf(poster.topicId.toString(),poster.description,poster.photoUrl,poster.workflow.orEmpty(),poster.legacyComplete.toString(),photos.size.toString()) +
            photos + listOf(tags.size.toString()) + tags + location
        val canonical = fields.joinToString("") { "${it.toByteArray(Charsets.UTF_8).size}:$it" }
        return java.security.MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
