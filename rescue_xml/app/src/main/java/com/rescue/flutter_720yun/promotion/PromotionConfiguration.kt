package com.rescue.flutter_720yun.promotion

import android.content.Context
import com.google.gson.JsonObject
import com.rescue.flutter_720yun.network.AdoptionServiceCreator
import com.rescue.flutter_720yun.network.PromotionService
import com.rescue.flutter_720yun.promotion.models.PromotionUiPolicy
import kotlinx.coroutines.CancellationException

class PromotionConfiguration(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("promotion_configuration",0)
    val seconds get() = preferences.getInt("BOOST_DURATION_SECONDS",3600).takeIf { it > 0 } ?: 3600
    val duration get() = PromotionUiPolicy.duration(seconds)
    fun update(seconds: Int) { if (seconds > 0) preferences.edit().putInt("BOOST_DURATION_SECONDS",seconds).apply() }
    suspend fun refresh() {
        try {
            val response = AdoptionServiceCreator.retrofit.create(PromotionService::class.java).configuration()
            if (response.isSuccessful && response.body()?.code == 200) parse(response.body()?.data)?.let { update(it) }
        } catch (e: CancellationException) { throw e } catch (_: Exception) { /* Use the last valid cached configuration. */ }
    }
    companion object {
        fun parse(data: JsonObject?): Int? {
            if (data?.get("key")?.takeIf { it.isJsonPrimitive }?.asString != "BOOST_DURATION_SECONDS") return null
            val value = data.get("value")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return null
            if (!value.isNumber) return null
            return runCatching { value.asBigDecimal.intValueExact().takeIf { it > 0 } }.getOrNull()
        }
    }
}
