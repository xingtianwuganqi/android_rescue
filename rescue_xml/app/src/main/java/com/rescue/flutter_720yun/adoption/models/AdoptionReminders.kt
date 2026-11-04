package com.rescue.flutter_720yun.adoption.models

object NotificationClassification {
    /** Identity always precedes visibility. Unknown kinds remain visible. */
    fun visible(item: AdoptionNotification): Boolean =
        item.role != "received" || item.kind !in setOf("communicate", "refresh")
    fun classify(items: List<AdoptionNotification>, ownApplications: Set<Int>): List<AdoptionNotification> =
        items.associateBy { it.id }.values.filter { !it.read }.map {
            it.copy(role = it.role.takeIf { role -> role == "mine" || role == "received" }
                ?: if(it.application_id in ownApplications) "mine" else "received")
        }
    fun visibleUnread(items: List<AdoptionNotification>): List<AdoptionNotification> =
        items.associateBy { it.id }.values.filter { !it.read && visible(it) }
    fun counts(items: List<AdoptionNotification>): UnreadCount {
        val visible=visibleUnread(items)
        return UnreadCount(visible.size,visible.count { it.role=="mine" },visible.count { it.role=="received" })
    }
}

data class ReminderPage(val items: List<AdoptionNotification>, val page: Int, val hasMore: Boolean, val counts: UnreadCount)
