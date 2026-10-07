package com.rescue.flutter_720yun.ads

/** A late callback may release its own presentation, never a newer one. */
class FullscreenOwnership {
    private var owner: Any? = null
    val presented: Boolean @Synchronized get() = owner != null
    @Synchronized fun acquire(token: Any): Boolean {
        if(owner != null) return false
        owner = token
        return true
    }
    @Synchronized fun release(token: Any) {
        if(owner === token) owner = null
    }
}
