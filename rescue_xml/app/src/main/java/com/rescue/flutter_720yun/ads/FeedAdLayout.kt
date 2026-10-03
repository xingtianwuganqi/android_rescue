package com.rescue.flutter_720yun.ads

sealed class FeedAdRow {
    data class Content(val index: Int) : FeedAdRow()
    data class Ad(val slotIndex: Int) : FeedAdRow()
}

object FeedAdLayout {
    fun rows(contentCount: Int, adAnchors: List<Int>): List<FeedAdRow> {
        val anchors = adAnchors.withIndex().groupBy { it.value.coerceIn(0, contentCount) }
        return buildList {
            for (index in 0..contentCount) {
                anchors[index]?.forEach { add(FeedAdRow.Ad(it.index)) }
                if (index < contentCount) add(FeedAdRow.Content(index))
            }
        }
    }
}
