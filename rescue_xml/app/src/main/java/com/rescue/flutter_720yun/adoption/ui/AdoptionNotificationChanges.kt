package com.rescue.flutter_720yun.adoption.ui
import androidx.lifecycle.MutableLiveData

/** Only a refresh signal is shared; no notification payload or private contact is cached. */
object AdoptionNotificationChanges {
    val revision = MutableLiveData(0L)
    var readRole: String? = null
        private set
    fun changed(role: String? = null) { readRole=role; revision.value = (revision.value ?: 0L) + 1L }
}
