package com.rescue.flutter_720yun.safety

import android.content.Context
import java.time.LocalDate

class StartupSafetyStore(context: Context) {
    private val prefs=context.applicationContext.getSharedPreferences("startup_safety",0)
    val completions get()=prefs.getInt("completed",0)
    fun display(enabled: Boolean)=StartupSafetyPolicy.display(enabled,prefs.getBoolean("hidden",false),prefs.getString("daily_date",null),LocalDate.now().toString())
    fun complete(hide: Boolean)=prefs.edit().putInt("completed",(completions+1).coerceAtMost(3))
        .putBoolean("hidden",hide && StartupSafetyPolicy.canOptOut(completions)).commit()
    fun markDaily()=prefs.edit().putString("daily_date",LocalDate.now().toString()).commit()
}
