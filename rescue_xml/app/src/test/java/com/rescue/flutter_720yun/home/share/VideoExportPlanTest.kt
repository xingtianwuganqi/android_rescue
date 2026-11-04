package com.rescue.flutter_720yun.home.share

import org.junit.Assert.*
import org.junit.Test

class VideoExportPlanTest {
    @Test fun everyPhotoKeepsOriginalOrderAndIosDurations() {
        val slides = VideoExportPlan.slides(3,123)
        assertEquals(listOf(0,1,2),slides.mapNotNull { it.photoIndex })
        assertEquals(listOf("photo","photo","photo","introduction","search","download"),slides.map { it.kind })
        assertEquals(listOf(90,90,90,150,60,90),slides.map { it.frames })
        assertEquals(19 * 30,slides.sumOf { it.frames })
        assertEquals(720,VideoExportPlan.WIDTH); assertEquals(1280,VideoExportPlan.HEIGHT)
    }
    @Test fun absentTopicUsesFiveSecondDownloadWithoutFakeSearchId() {
        val slides = VideoExportPlan.slides(1,0)
        assertEquals(listOf("photo","introduction","download"),slides.map { it.kind })
        assertEquals(13 * 30,slides.sumOf { it.frames })
    }
    @Test fun noPhotoCannotProduceMisleadingEmptyVideo() {
        try { VideoExportPlan.slides(0,123); fail("Expected invalid input") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun snapshotChangesWithPhotoOrderStatusOrPublicContent() {
        val poster = PosterContent(123,"猫\n\n介绍","https://example.com/a.jpg","open",false,true)
        val content = VideoContent(poster,listOf(poster.photoUrl,"https://example.com/b.jpg"),listOf("猫"),"上海")
        assertEquals("介绍",content.bodyDescription)
        assertEquals(content.fingerprint,content.copy().fingerprint)
        assertNotEquals(content.fingerprint,content.copy(photos=content.photos.reversed()).fingerprint)
        assertNotEquals(content.fingerprint,content.copy(location="北京").fingerprint)
        assertNotEquals(content.fingerprint,content.copy(poster=poster.copy(workflow="adopted")).fingerprint)
        assertNotEquals(content.fingerprint,content.copy(tags=listOf("猫","已绝育")).fingerprint)
        assertEquals(64,content.fingerprint.length)
    }
    @Test fun soundtrackHasIosGainAndFadeEnvelope() {
        assertEquals(0.0,VideoExportPlan.musicGain(0.0,13.0),0.00001)
        assertEquals(0.225,VideoExportPlan.musicGain(0.35,13.0),0.00001)
        assertEquals(0.45,VideoExportPlan.musicGain(1.0,13.0),0.00001)
        assertEquals(0.225,VideoExportPlan.musicGain(12.4,13.0),0.00001)
        assertEquals(0.0,VideoExportPlan.musicGain(13.0,13.0),0.00001)
        for (i in -10..150) assertTrue(VideoExportPlan.musicGain(i/10.0,13.0) in 0.0..0.45)
    }
}
