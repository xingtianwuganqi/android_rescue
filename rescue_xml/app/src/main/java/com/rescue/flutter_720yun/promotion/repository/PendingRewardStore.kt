package com.rescue.flutter_720yun.promotion.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rescue.flutter_720yun.promotion.models.PendingReward

/** Synchronous commit at reward observation closes the process-death gap before POST. */
class PendingRewardStore(context: Context) {
    private val prefs=context.applicationContext.getSharedPreferences("pending_topic_rewards",Context.MODE_PRIVATE)
    private val gson=Gson()
    @Synchronized fun records(): List<PendingReward> = runCatching {
        gson.fromJson<List<PendingReward>>(prefs.getString("records","[]"),object:TypeToken<List<PendingReward>>(){}.type)
    }.getOrNull().orEmpty()
    @Synchronized fun put(record: PendingReward): Boolean = prefs.edit().putString("records",gson.toJson(
        records().filter { it.reward_attempt_id!=record.reward_attempt_id }+record)).commit()
    @Synchronized fun remove(key: String): Boolean = prefs.edit().putString("records",gson.toJson(
        records().filter { it.reward_attempt_id!=key })).commit()
}
