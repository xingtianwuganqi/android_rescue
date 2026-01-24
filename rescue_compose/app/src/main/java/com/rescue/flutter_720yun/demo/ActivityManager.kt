package com.rescue.flutter_720yun.demo

import android.app.Activity

object AppActivityManager {

    private val activityStack = mutableListOf<Activity>()

    internal fun addActivity(activity: Activity) {
        activityStack.add(activity)
    }

    internal fun removeActivity(activity: Activity) {
        activityStack.remove(activity)
    }

    fun finishActivity(activity: Activity) {
        if (!activity.isFinishing) {
            activity.finish()
        }
        activityStack.remove(activity)
    }

    fun finishTopActivity() {
        activityStack.lastOrNull()?.let {
            finishActivity(it)
        }
    }

    fun finishAll() {
        val iterator = activityStack.iterator()
        while (iterator.hasNext()) {
            val activity = iterator.next()
            if (!activity.isFinishing) {
                activity.finish()
            }
            iterator.remove()
        }
    }

    fun getTopActivity(): Activity? {
        return activityStack.lastOrNull()
    }

    fun getActivityCount(): Int = activityStack.size
}
