package com.rescue.flutter_720yun.ads

/** Positions count business items, so an earlier ad never shifts the next page's anchor. */
enum class FeedAdPlacement {
    HOME, LOCAL, SEARCH, FIND_PET, SHOW;

    fun insertionIndex(pageSize: Int, refresh: Boolean): Int? {
        if (pageSize <= 0) return null
        return when (this) {
            HOME -> if (refresh) minOf(2, pageSize) else 0
            SEARCH -> if (refresh && pageSize <= 3) null else minOf(3, pageSize)
            else -> minOf(3, pageSize)
        }
    }
}
