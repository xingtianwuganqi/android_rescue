package com.rescue.flutter_720yun.adoption.models

/** Acquisition is independent of application status. Only the current user's V2 response is used. */
object AdoptionContact {
    fun visible(topic: TopicSummary?): String? = topic?.takeIf {
        it.getedcontact && it.is_complete == false && it.is_delete == 1 && !it.unavailable
    }?.contact_info?.takeIf { it.isNotBlank() }
}
