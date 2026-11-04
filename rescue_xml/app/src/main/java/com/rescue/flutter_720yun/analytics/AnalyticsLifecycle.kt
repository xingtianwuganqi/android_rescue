package com.rescue.flutter_720yun.analytics

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.view.WindowCallbackWrapper
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import com.rescue.flutter_720yun.MainActivity
import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.adoption.activity.*
import com.rescue.flutter_720yun.findpet.MyFindPetActivity
import com.rescue.flutter_720yun.home.activity.*
import com.rescue.flutter_720yun.message.activity.*
import com.rescue.flutter_720yun.show.activity.ShowReleaseActivity
import com.rescue.flutter_720yun.util.UserManager
import java.util.UUID
import java.util.WeakHashMap

/** Started-activity aggregation with the same 700ms grace used by process lifecycle.
 * Activity handoffs/config changes cancel background detection. Window events, not network
 * responses, update interaction time. No lifecycle-process dependency is required. */
internal class AnalyticsLifecycle : Application.ActivityLifecycleCallbacks {
    private var started=0
    private val handler=Handler(Looper.getMainLooper())
    private val stop=Runnable { if(started==0) AnalyticsManager.background() }
    override fun onActivityCreated(activity: Activity,state: Bundle?) {
        AnalyticsPages.register(activity,state?.getString("analytics.screen_instance"))
    }
    override fun onActivityStarted(activity: Activity) {
        handler.removeCallbacks(stop)
        started++
        if(started==1) AnalyticsManager.foreground()
    }
    override fun onActivityResumed(activity: Activity) {
        val callback=activity.window.callback
        if(callback!=null && callback !is InteractionCallback) activity.window.callback=InteractionCallback(callback)
        activity.window.decorView.post { AnalyticsPages.visible(activity) }
    }
    override fun onActivityStopped(activity: Activity) {
        started=(started-1).coerceAtLeast(0)
        if(started==0) handler.postDelayed(stop,700)
    }
    override fun onActivitySaveInstanceState(activity: Activity,state: Bundle) {
        state.putString("analytics.screen_instance",AnalyticsPages.key(activity))
    }
    override fun onActivityDestroyed(activity: Activity) { AnalyticsPages.remove(activity) }
    override fun onActivityPaused(activity: Activity) = Unit
    private class InteractionCallback(callback: android.view.Window.Callback) : WindowCallbackWrapper(callback) {
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            if(event.actionMasked==MotionEvent.ACTION_DOWN) AnalyticsManager.interaction()
            return super.dispatchTouchEvent(event)
        }
        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if(event.action==KeyEvent.ACTION_DOWN) AnalyticsManager.interaction()
            return super.dispatchKeyEvent(event)
        }
        override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
            if(event.actionMasked==MotionEvent.ACTION_SCROLL) AnalyticsManager.interaction()
            return super.dispatchGenericMotionEvent(event)
        }
    }
}

/** One registry for actual activities and the visible main navigation destination.
 * No fragment bind/render callbacks (including ViewPager preload) produce events. */
object AnalyticsPages {
    private val keys=WeakHashMap<Activity,String>()
    internal fun register(activity: Activity,restored: String?) { keys[activity]=restored ?: UUID.randomUUID().toString() }
    internal fun key(activity: Activity)=keys.getOrPut(activity) { UUID.randomUUID().toString() }
    internal fun remove(activity: Activity) { keys.remove(activity) }
    fun visible(activity: Activity) {
        if(activity.isFinishing || activity.isDestroyed ||
            (activity is FragmentActivity && !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))) return
        val properties=mutableMapOf<String,Any>()
        val page=when(activity) {
            is MainActivity -> when(runCatching { activity.findNavController(R.id.nav_host_fragment_activity_main).currentDestination?.id }.getOrNull()) {
                R.id.navigation_home -> "home"
                R.id.navigation_message -> "messages"
                R.id.navigation_user -> "profile"
                else -> null // No invented page code for the show-pet feed.
            }
            is LoginActivity -> if((activity.intent.getStringExtra("type") ?: "login") in setOf("login","register","registerCheckCode")) "login" else null
            is ReleaseTopicActivity -> "publish_adoption"
            is FindPetDetailActivity -> if(UserManager.isLogin) "publish_find_pet" else null
            is ShowReleaseActivity -> "publish_show_pet"
            is HomeDetailActivity -> {
                activity.intent.getIntExtra("topic_id",0).takeIf { it>0 }?.let { properties["object_id"]=it.toLong() }
                "topic_detail"
            }
            is AdoptionProfileActivity -> if(UserManager.isLogin) "adoption_profile" else null
            is MessageSingleActivity -> {
                val type=when(activity.intent.getIntExtra("messageType",0)) { 1 -> "like";2 -> "collect";3 -> "comment";else -> null }
                type?.let { properties["message_list_type"]=it }
                if(type!=null) "message_list" else null
            }
            is MessageSystemListActivity -> { properties["message_list_type"]="system";if(UserManager.isLogin) "message_list" else null }
            is MyFindPetActivity -> { properties["message_list_type"]="find_pet";if(UserManager.isLogin) "message_list" else null }
            is AdoptionMyApplicationsActivity -> { properties["message_list_type"]="my_applications";if(UserManager.isLogin) "message_list" else null }
            is AdoptionApplicationsActivity -> { properties["message_list_type"]="received_applications";if(UserManager.isLogin) "message_list" else null }
            else -> null
        }
        val route=if(activity is MainActivity) runCatching { activity.findNavController(R.id.nav_host_fragment_activity_main).currentDestination?.id }.getOrNull().toString() else ""
        AnalyticsManager.showScreen(key(activity)+":"+route+":"+page+":"+properties.toString(),page,properties)
    }
}
