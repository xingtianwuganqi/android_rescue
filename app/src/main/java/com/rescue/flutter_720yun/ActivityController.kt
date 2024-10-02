package com.rescue.flutter_720yun

import android.app.Activity

object ActivityController {
    val list: MutableList<Activity> = mutableListOf()
    fun addActivity(activity: Activity) {
        list.add(activity)
    }

    fun removeActivity(activity: Activity) {
        list.remove(activity)
    }

    fun finishAll() {
        for (activity in list) {
            if (!activity.isFinishing) {
                activity.finish()
            }
        }
        list.clear()
    }
}