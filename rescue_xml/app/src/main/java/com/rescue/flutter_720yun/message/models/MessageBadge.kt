package com.rescue.flutter_720yun.message.models
import com.rescue.flutter_720yun.home.models.MessageListModel

object MessageBadge {
    fun total(entries: List<MessageListModel>, applicationTotal: Int?): Int =
        entries.filter { it.category in listOf("system", "like", "collection", "comment", "my_find_pet") }
            .sumOf { it.unread ?: 0 } + (applicationTotal ?: 0)
}
