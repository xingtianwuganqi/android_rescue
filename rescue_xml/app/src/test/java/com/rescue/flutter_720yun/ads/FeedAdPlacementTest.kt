package com.rescue.flutter_720yun.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeedAdPlacementTest {
    @Test fun homeUsesThirdRowAndNextPageStart() {
        assertEquals(2, FeedAdPlacement.HOME.insertionIndex(10, true))
        assertEquals(0, FeedAdPlacement.HOME.insertionIndex(10, false))
        assertEquals(1, FeedAdPlacement.HOME.insertionIndex(1, true))
    }
    @Test fun fourthRowListsAppendAfterShortPages() {
        for (placement in listOf(FeedAdPlacement.LOCAL, FeedAdPlacement.FIND_PET, FeedAdPlacement.SHOW)) {
            assertEquals(3, placement.insertionIndex(10, true))
            assertEquals(3, placement.insertionIndex(10, false))
            assertEquals(2, placement.insertionIndex(2, true))
            assertEquals(2, placement.insertionIndex(2, false))
        }
    }
    @Test fun searchSkipsShortInitialResultsButAllowsShortMorePage() {
        assertNull(FeedAdPlacement.SEARCH.insertionIndex(3, true))
        assertEquals(3, FeedAdPlacement.SEARCH.insertionIndex(4, true))
        assertEquals(2, FeedAdPlacement.SEARCH.insertionIndex(2, false))
    }
    @Test fun emptyResultsNeverShowAnAdOnlyPage() {
        for (placement in FeedAdPlacement.values()) {
            assertNull(placement.insertionIndex(0, true))
            assertNull(placement.insertionIndex(0, false))
        }
    }
}
