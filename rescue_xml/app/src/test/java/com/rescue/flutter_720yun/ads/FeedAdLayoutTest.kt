package com.rescue.flutter_720yun.ads

import org.junit.Assert.assertEquals
import org.junit.Test

class FeedAdLayoutTest {
    @Test fun homeAdAndMorePageKeepBusinessIndicesInOrder() {
        val rows = FeedAdLayout.rows(6, listOf(2, 4))
        assertEquals(listOf(FeedAdRow.Content(0), FeedAdRow.Content(1), FeedAdRow.Ad(0),
            FeedAdRow.Content(2), FeedAdRow.Content(3), FeedAdRow.Ad(1),
            FeedAdRow.Content(4), FeedAdRow.Content(5)), rows)
    }
    @Test fun adFailureOrCloseLeavesEveryBusinessRecordAccessible() {
        val rows = FeedAdLayout.rows(6, listOf(4))
        assertEquals((0..5).toList(), rows.filterIsInstance<FeedAdRow.Content>().map { it.index })
        assertEquals(4, rows.indexOf(FeedAdRow.Ad(0)))
        assertEquals((0..5).map { FeedAdRow.Content(it) }, FeedAdLayout.rows(6, emptyList()))
    }
    @Test fun shortPageAdAppearsAfterLastRecordAndClampsAfterDeletion() {
        assertEquals(listOf(FeedAdRow.Content(0), FeedAdRow.Ad(0)), FeedAdLayout.rows(1, listOf(3)))
    }
}
