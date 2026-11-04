package com.rescue.flutter_720yun.home.share

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Device-only Canvas/StaticLayout validation; does not write to the user's photo library. */
@RunWith(AndroidJUnit4::class)
class TopicSharePosterRendererTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun standalonePosterKeepsPhotoAspectAndDropsTheEntireQrFooter() {
        val photo=Bitmap.createBitmap(200,100,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        val content=PosterContent(123,"","https://example.test/pet.jpg",null,false,true)
        val renderer=TopicSharePosterRenderer(context)
        val coded=renderer.render(content,photo)
        val plain=renderer.render(content.copy(includeQr=false),photo)
        try {
            assertEquals(1170,coded.width);assertEquals(1170,plain.width)
            assertEquals(654,coded.height-plain.height)
            // 350 logical width and original 2:1 aspect: full 175 logical-height photo.
            assertEquals(Color.BLUE,plain.getPixel(585,350))
            assertEquals(Color.BLUE,plain.getPixel(585,850))
            assertEquals(Color.WHITE,plain.getPixel(585,880))
        } finally { coded.recycle();plain.recycle();photo.recycle() }
    }
    @Test fun tallAndLandscapePhotosRemainBoundedAndUncropped() {
        val renderer=TopicSharePosterRenderer(context)
        for((width,height) in listOf(300 to 500,500 to 300)) {
            val photo=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
            try {
                val poster=renderer.render(PosterContent(29,"正常介绍","https://example.test/pet.jpg","closed",true,true),photo)
                try { assertEquals(1170,poster.width);assertTrue(poster.height in 1..6000) } finally { poster.recycle() }
            } finally { photo.recycle() }
        }
    }
    @Test fun pathologicalAspectRatioFailsBeforeAllocatingAnUnboundedPoster() {
        val photo=Bitmap.createBitmap(1,500,Bitmap.Config.ARGB_8888)
        try {
            try { TopicSharePosterRenderer(context).render(PosterContent(123,"","https://example.test/pet.jpg","open",false,true),photo);fail("Unsafe height") }
            catch(expected: IllegalArgumentException) { }
        } finally { photo.recycle() }
    }
    @Test fun androidRegexRetainsCombinedEmojiDuringTruncation() {
        val emoji="👨‍👩‍👧‍👦"
        val preview=PosterText.preview("中$emoji".repeat(30)) { it.length<=2+emoji.length }
        assertEquals("中$emoji…",preview.text)
    }
}
