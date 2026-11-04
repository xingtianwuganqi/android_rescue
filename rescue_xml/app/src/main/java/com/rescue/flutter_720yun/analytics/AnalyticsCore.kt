package com.rescue.flutter_720yun.analytics

import com.google.gson.Gson
import java.time.Instant
import java.util.UUID

/** Only random installation/session IDs are persisted. No credentials or event queue. */
data class AnalyticsState(val anonymousId: String, val sessionId: String, val sequence: Int = 0,
    val lastInteractionAt: Long = 0, val backgroundAt: Long = 0, val accountId: Int? = null)
interface AnalyticsStateStore {
    fun read(): AnalyticsState?
    fun write(state: AnalyticsState)
}
data class AnalyticsIdentity(val accountId: Int?, val token: String?) {
    val authenticated get() = accountId != null && !token.isNullOrBlank()
}
data class AnalyticsEvent(val event_id: String, val event_name: String, val anonymous_id: String,
    val session_id: String, val sequence: Int, val occurred_at: String, val platform: String,
    val app_version: String, val page: String, val properties: Map<String, Any>)
data class AnalyticsEnvelope(val event: AnalyticsEvent, val identity: AnalyticsIdentity)

/** Serial allocation and a testable clock. Automatic completions never extend idle time. */
class AnalyticsCore(private val store: AnalyticsStateStore, private val now: () -> Long = System::currentTimeMillis,
    private val uuid: () -> String = { UUID.randomUUID().toString() }) {
    companion object { const val IDLE_MS = 30 * 60 * 1000L }
    private var state: AnalyticsState? = store.read()
    @Volatile var enabled = true; private set
    private var foreground = false
    private var cold = true
    @Synchronized fun setEnabled(value: Boolean) { enabled=value }
    private fun state(identity: AnalyticsIdentity): AnalyticsState {
        var result=state ?: AnalyticsState(uuid(),uuid(),lastInteractionAt=now(),accountId=identity.accountId)
        if(result.accountId!=identity.accountId) {
            // Anonymous -> login keeps the path; logout and switching authenticated users end it.
            result=if(result.accountId!=null) result.copy(sessionId=uuid(),sequence=0,lastInteractionAt=now(),accountId=identity.accountId)
                else result.copy(accountId=identity.accountId)
        }
        state=result
        return result
    }
    private fun save(value: AnalyticsState) { state=value;runCatching { store.write(value) } }
    @Synchronized fun identityChanged(identity: AnalyticsIdentity) { if(enabled || state!=null) save(state(identity)) }
    @Synchronized fun interaction(identity: AnalyticsIdentity) {
        if(!enabled || !foreground) return
        val time=now();var value=state(identity)
        if(time-value.lastInteractionAt>=IDLE_MS) value=value.copy(sessionId=uuid(),sequence=0)
        save(value.copy(lastInteractionAt=time))
    }
    @Synchronized fun enterForeground(identity: AnalyticsIdentity): Boolean {
        if(foreground) return false
        foreground=true
        if(!enabled) { cold=false;return false }
        val time=now();var value=state(identity)
        val longBackground=value.backgroundAt>0 && time-value.backgroundAt>=IDLE_MS
        if(longBackground || cold && time-value.lastInteractionAt>=IDLE_MS) value=value.copy(sessionId=uuid(),sequence=0)
        save(value.copy(lastInteractionAt=time,backgroundAt=0))
        val opened=cold || longBackground
        cold=false
        return opened
    }
    @Synchronized fun leaveForeground(identity: AnalyticsIdentity): Boolean {
        if(!foreground) return false
        foreground=false
        if(!enabled) return false
        save(state(identity).copy(backgroundAt=now()))
        return true
    }
    @Synchronized fun create(name: String, page: String, properties: Map<String, Any>, identity: AnalyticsIdentity,
        version: String, interactive: Boolean = false, eventId: String? = null): AnalyticsEnvelope? {
        if(!enabled || !AnalyticsContract.valid(name,page,properties,identity.authenticated)) return null
        if(interactive) interaction(identity)
        var value=state(identity)
        if(value.sequence==Int.MAX_VALUE) value=value.copy(sessionId=uuid(),sequence=0)
        value=value.copy(sequence=value.sequence+1)
        val event=AnalyticsEvent(eventId ?: uuid(),name,value.anonymousId,value.sessionId,value.sequence,
            Instant.ofEpochMilli(now()).toString(),"android",version.take(32),page,properties.toMap())
        if(!AnalyticsContract.withinSize(event)) return null
        save(value)
        return AnalyticsEnvelope(event,identity.copy())
    }
}

object AnalyticsContract {
    private val gson=Gson()
    val pages=setOf("home","login","publish_selector","publish_adoption","publish_find_pet","publish_show_pet",
        "topic_detail","messages","profile","adoption_profile","message_list")
    val lists=setOf("like","collect","comment","system","find_pet","my_applications","received_applications")
    private val types=setOf("adoption","find_pet","show_pet")
    private val methods=setOf("password","sms","email","wechat","apple","other")
    private val reasons=setOf("invalid_credentials","invalid_code","account_unavailable","cancelled","network_error","provider_error","unknown")
    private val requiredAuth=setOf("login_success","publish_success","logOff","click_video_ad","video_ad_success")
    private val fields=mapOf(
        "app_open" to setOf("referrer_page"), "app_quit" to emptySet(),
        "page_view" to setOf("object_id","referrer_page","message_list_type"),
        "login_click" to setOf("login_method","referrer_page"), "login_success" to setOf("login_method"),
        "login_failed" to setOf("login_method","reason_code"),
        "publish_click" to setOf("publish_type","referrer_page"),
        "publish_page_view" to setOf("publish_type","referrer_page"),
        "publish_success" to setOf("publish_type","object_id"),
        "logout" to emptySet(), "logOff" to emptySet(),
        "export_image" to setOf("publish_type","object_id"), "export_video" to setOf("publish_type","object_id"),
        "click_video_ad" to setOf("ad_placement","object_id"),
        "video_ad_success" to setOf("ad_placement","object_id","reward_attempt_id"))
    fun valid(name: String,page: String,p: Map<String,Any>,authenticated: Boolean): Boolean {
        val allowed=fields[name] ?: return false
        if(page.isNotEmpty() && page !in pages || p.keys.any { it !in allowed } || name in requiredAuth && !authenticated) return false
        if(name=="page_view" && page.isEmpty()) return false
        for((key,value) in p) {
            val valid=when(key) {
                "object_id" -> (value is Long && value>0) || (value is Int && value>0)
                "publish_type" -> value in types
                "login_method" -> value in methods
                "reason_code" -> value in reasons
                "referrer_page" -> value in pages
                "message_list_type" -> name=="page_view" && page=="message_list" && value in lists
                "ad_placement" -> value in setOf("my_posts","publish_success")
                "reward_attempt_id" -> value is String && runCatching { UUID.fromString(value).toString()==value.lowercase() }.getOrDefault(false)
                else -> false
            }
            if(!valid) return false
        }
        if(name=="page_view" && page=="message_list" && !p.containsKey("message_list_type")) return false
        if(name=="login_failed" && !p.containsKey("reason_code")) return false
        if(name in setOf("publish_page_view","publish_success","export_image","export_video") && !p.containsKey("publish_type")) return false
        if(name=="publish_page_view" && page!="publish_"+p["publish_type"]) return false
        if(name in setOf("publish_success","export_image","export_video","click_video_ad","video_ad_success") && !p.containsKey("object_id")) return false
        if(name in setOf("click_video_ad","video_ad_success") && !p.containsKey("ad_placement")) return false
        if(name=="video_ad_success" && !p.containsKey("reward_attempt_id")) return false
        return true
    }
    fun withinSize(event: AnalyticsEvent)=gson.toJson(event.properties).toByteArray(Charsets.UTF_8).size<=2048 &&
        gson.toJson(event).toByteArray(Charsets.UTF_8).size<=8192
}

/** Config changes, permission dialogs and repeated renders keep the same visible key. */
class AnalyticsExposure {
    private var visibleKey: String?=null
    var lastPage: String?=null; private set
    fun show(key: String,page: String?): Boolean {
        if(key==visibleKey) return false
        visibleKey=key
        lastPage=page
        return page!=null
    }
}
