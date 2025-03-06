package com.rescue.flutter_720yun

import android.app.Activity

object ActivityController {
    val activities: MutableList<Activity> = mutableListOf()
    fun addActivity(activity: Activity) {
        activities.add(activity)
    }

    fun removeActivity(activity: Activity) {
        activities.remove(activity)
    }

    fun finishAll() {
        for (activity in activities) {
            if (!activity.isFinishing) {
                activity.finish()
            }
        }
        activities.clear()
    }

    fun finishToLast() {
        activities.forEachIndexed({ index,value ->
            if (index != 0) {
                value.finish()
            }
        })
    }
}