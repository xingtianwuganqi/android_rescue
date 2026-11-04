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
    @Test fun threePagesSurviveFailedSameRangeRefreshAndDedupeOnFourthPage() {
        val batches=FeedBatches()
        repeat(3) { index -> batches.current.accept(meta(index+1,true),listOf(topic(index+1))) }
        val old=batches.current
        val refresh=batches.candidate(true)
        assertNull(refresh.snapshot);assertEquals(1,refresh.nextPage)
        // Simulate transport failure: the disposable candidate never commits.
        assertSame(old,batches.current);assertEquals("batch",old.snapshot);assertEquals(4,old.nextPage)
        assertEquals(listOf(9),old.accept(meta(4,false),listOf(topic(1),topic(9))).map { it.topic_id })
    }
    @Test fun successfulEmptyRefreshAtomicallyReplacesOldSeenAndHasMore() {
        val batches=FeedBatches();batches.current.accept(meta(1,false),listOf(topic(9)))
        val fresh=batches.candidate(true);fresh.accept(meta(1,true,"new"),emptyList());batches.commit(fresh)
        assertEquals("new",batches.current.snapshot);assertTrue(batches.current.hasMore)
        assertEquals(listOf(9),batches.current.accept(meta(2,false,"new"),listOf(topic(9))).map { it.topic_id })
    }
    @Test fun rangeChangeAndSnapshotInvalidationCannotRestorePreviousBatch() {
        val batches=FeedBatches();batches.current.accept(meta(1,true),listOf(topic(9)))
        batches.invalidate()
        assertNull(batches.current.snapshot);assertEquals(1,batches.current.nextPage)
        assertNotSame(batches.current,batches.candidate(true))
    }
}
