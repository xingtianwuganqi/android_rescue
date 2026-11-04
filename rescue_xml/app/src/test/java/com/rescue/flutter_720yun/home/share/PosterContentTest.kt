package com.rescue.flutter_720yun.home.share

import org.junit.Test
import org.junit.Assert.*

class PosterContentTest {
    @Test fun v2StatusOverridesLegacyAndUnknownOpenIsNeverInvented() {
        assertEquals("送养中",PosterContent.status("open",true))
        assertEquals("送养已结束 · 已完成送养",PosterContent.status("adopted",false))
        assertEquals("送养已结束 · 已关闭送养",PosterContent.status("closed",false))
        assertEquals("送养已结束",PosterContent.status(null,true))
        assertNull(PosterContent.status(null,false));assertNull(PosterContent.status("unexpected",false))
    }
    @Test fun descriptionOmitsEmptySectionsAndPrivateFieldsHaveNoInput() {
        assertEquals("猫 · 已绝育\n\n介绍",PosterContent.introduction(listOf("猫"," ","猫","已绝育")," 介绍 "))
        assertEquals("介绍",PosterContent.introduction(emptyList(),"介绍"))
        assertEquals("猫",PosterContent.introduction(listOf("猫"),""))
        assertEquals("",PosterContent.introduction(emptyList(),null))
        assertEquals(setOf("topicId","description","photoUrl","workflow","legacyComplete","includeQr"),
            PosterContent::class.java.declaredFields.filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) }.map { it.name }.toSet())
    }
    @Test fun originalImagesAndTrueIdLinksUseCurrentEnvironment() {
        assertEquals("http://img.rxswift.cn/cat.jpg",PosterContent.imageUrl(listOf(" ","cat.jpg")))
        assertEquals("https://img.test/a.png",PosterContent.imageUrl(listOf("https://img.test/a.png")))
        assertNull(PosterContent.imageUrl(listOf("file:///secret")));assertNull(PosterContent.imageUrl(null))
        assertEquals("https://test.example/api/topic/info/123/",PosterContent.shareLink("https://test.example/",123))
        try { PosterContent.shareLink("https://test.example",0);fail("Invalid ID") } catch(e: IllegalArgumentException) { }
    }
    @Test fun truncationNeverCutsEmojiCombiningMarksOrFlags() {
        val emojis=listOf("👨‍👩‍👧‍👦","👍🏽","🇨🇳","e\u0301","🧑🏽‍🚒")
        for(emoji in emojis) {
            val source="中$emoji".repeat(20)
            val result=PosterText.preview(source) { it.length<=2+emoji.length }
            assertTrue(result.truncated);assertEquals("中$emoji…",result.text)
        }
        assertEquals(PosterText.Preview("短文",false),PosterText.preview(" 短文 ") { true })
        assertEquals(PosterText.Preview("",false),PosterText.preview("  ") { true })
    }
}
