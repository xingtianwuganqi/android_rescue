package com.rescue.flutter_720yun.home.models

import com.google.gson.JsonElement

data class FeedMeta(val snapshot_id: String?, val expires_at: String?, val page: Int, val size: Int,
    val has_more: Boolean?, val total: Int, val effective_order: String?, val promotion_version: String?)
data class FeedResponse(val code: Int, val message: String?, val data: JsonElement?, val meta: FeedMeta?)

/** Page progress depends on server metadata, including an empty filtered page. */
class FeedCursor {
    var snapshot: String?=null
        private set
    var nextPage=1
        private set
    var hasMore=true
        private set
    private val seen=mutableSetOf<Int>()
    fun reset() { snapshot=null;nextPage=1;hasMore=true;seen.clear() }
    fun accept(meta: FeedMeta, items: List<HomeListModel>): List<HomeListModel> {
        require(meta.has_more!=null)
        require(meta.page==nextPage && meta.size==10 && !meta.snapshot_id.isNullOrBlank())
        require(snapshot==null || snapshot==meta.snapshot_id)
        snapshot=meta.snapshot_id;hasMore=meta.has_more;nextPage++
        return items.filter { it.topic_id!=null && seen.add(it.topic_id) }
    }
}
