package com.rescue.flutter_720yun.analytics

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import com.rescue.flutter_720yun.BuildConfig
import com.rescue.flutter_720yun.util.UserManager

object AnalyticsManager {
    private var core: AnalyticsCore?=null
    private var transport: AnalyticsTransport?=null
    private var exposure=AnalyticsExposure()
    private val rewardSuccesses=mutableSetOf<String>()
    private fun identity()=UserManager.userInfo.let { AnalyticsIdentity(it?.id,it?.token) }
    fun initialize(app: Application) {
        if(core!=null) return
        val prefs=app.getSharedPreferences("analytics_v4",Context.MODE_PRIVATE)
        val gson=Gson()
        core=AnalyticsCore(object: AnalyticsStateStore {
            override fun read(): AnalyticsState?=runCatching {
                gson.fromJson(prefs.getString("session",null),AnalyticsState::class.java)?.takeIf {
                    java.util.UUID.fromString(it.anonymousId);java.util.UUID.fromString(it.sessionId)
                    it.sequence>=0 && it.lastInteractionAt>=0 && it.backgroundAt>=0
                }
            }.getOrNull()
            override fun write(state: AnalyticsState) { prefs.edit().putString("session",gson.toJson(state)).apply() }
        })
        transport=AnalyticsTransport(BuildConfig.API_BASE_URL)
        app.registerActivityLifecycleCallbacks(AnalyticsLifecycle())
    }
    @Synchronized fun setEnabled(enabled: Boolean) { core?.setEnabled(enabled);if(!enabled) transport?.cancelAll() }
    fun identityChanged() { core?.identityChanged(identity()) }
    fun interaction() { core?.interaction(identity()) }
    fun foreground() { if(core?.enterForeground(identity())==true) track("app_open") }
    fun background() { if(core?.leaveForeground(identity())==true) track("app_quit") }
    @Synchronized fun track(name: String,page: String="",properties: Map<String,Any> = emptyMap(),
        interactive: Boolean=false,eventId: String?=null,expectedAccount: Int?=null) {
        // Capture credentials now, never in a coroutine/interceptor after account changes.
        val who=identity()
        if(expectedAccount!=null && who.accountId!=expectedAccount) return
        runCatching { core?.create(name,page,properties,who,BuildConfig.VERSION_NAME,interactive,eventId)?.let { transport?.send(it) } }
    }
    fun showScreen(key: String,page: String?,properties: Map<String,Any> = emptyMap()) {
        val previous=exposure.lastPage
        if(!exposure.show(key,page) || page==null) return
        val props=properties.toMutableMap()
        if(previous!=null && previous!=page) props["referrer_page"]=previous
        track("page_view",page,props)
        val type=when(page) { "publish_adoption" -> "adoption";"publish_find_pet" -> "find_pet";"publish_show_pet" -> "show_pet";else -> null }
        if(type!=null) track("publish_page_view",page,buildMap {
            put("publish_type",type);if(previous!=null && previous!=page) put("referrer_page",previous)
        })
    }
    fun loginClick()=track("login_click",properties=referrer(),interactive=true)
    fun publishClick(type: String)=track("publish_click",properties=referrer()+mapOf("publish_type" to type),interactive=true)
    private fun referrer(): Map<String,Any> = exposure.lastPage?.let { mapOf("referrer_page" to it) } ?: emptyMap()
    fun rewardClick(topic: Int,scene: String)=track("click_video_ad",properties=mapOf("ad_placement" to scene,"object_id" to topic.toLong()),interactive=true)
    @Synchronized fun rewardSuccess(account: Int,topic: Int,scene: String,attemptId: String) {
        if(identity().accountId!=account || core?.enabled!=true || !rewardSuccesses.add(attemptId)) return
        track("video_ad_success",properties=mapOf("ad_placement" to scene,"object_id" to topic.toLong(),"reward_attempt_id" to attemptId),eventId=attemptId,expectedAccount=account)
    }
}
