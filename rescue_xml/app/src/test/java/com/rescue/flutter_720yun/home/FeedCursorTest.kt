package com.rescue.flutter_720yun.home

import com.google.gson.Gson
import com.rescue.flutter_720yun.home.models.*
import org.junit.Test
import org.junit.Assert.*

class FeedCursorTest {
    private fun meta(page: Int,more: Boolean,key: String="batch")=FeedMeta(key,null,page,10,more,25,null,key)
    private fun topic(id: Int)=Gson().fromJson("""{"topic_id":$id}""",HomeListModel::class.java)
    @Test fun emptyPageWithHasMoreAdvancesAndDuplicatesKeepServerOrder() {
        val cursor=FeedCursor()
        assertEquals(listOf(9,2),cursor.accept(meta(1,true),listOf(topic(9),topic(2))).map { it.topic_id })
        assertTrue(cursor.accept(meta(2,true),emptyList()).isEmpty())
        assertTrue(cursor.hasMore);assertEquals(3,cursor.nextPage)
        assertEquals(listOf(6,4),cursor.accept(meta(3,false),listOf(topic(2),topic(6),topic(4),topic(6))).map { it.topic_id })
        assertFalse(cursor.hasMore)
    }
    @Test fun mismatchCannotMixBatchesAndResetStartsFresh() {
        val cursor=FeedCursor();cursor.accept(meta(1,true),listOf(topic(9)))
        try { cursor.accept(meta(2,false,"another"),listOf(topic(2)));fail("Mixed batches") } catch(e: IllegalArgumentException) { }
        assertEquals(2,cursor.nextPage)
        cursor.reset();assertNull(cursor.snapshot)
        assertEquals(listOf(9),cursor.accept(meta(1,false,"new"),listOf(topic(9))).map { it.topic_id })
    }
}
