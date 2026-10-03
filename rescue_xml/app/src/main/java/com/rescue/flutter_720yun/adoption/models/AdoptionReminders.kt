package com.rescue.flutter_720yun.adoption.models

object NotificationClassification {
    fun classify(items: List<AdoptionNotification>, ownApplications: Set<Int>): List<AdoptionNotification> =
        items.distinctBy { it.id }.filter { !it.read }.map {
            it.copy(role = if(it.application_id in ownApplications) "mine" else "received")
        }
    fun counts(items: List<AdoptionNotification>) = UnreadCount(items.size,
        items.count { it.role == "mine" }, items.count { it.role == "received" })
}

data class ReminderPage(val items: List<AdoptionNotification>, val page: Int, val hasMore: Boolean, val counts: UnreadCount)
