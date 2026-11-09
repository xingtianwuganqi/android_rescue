package com.rescue.flutter_720yun.safety

object StartupSafetyPolicy {
    enum class Display { NONE, GUIDE, DAILY }
    fun display(enabled: Boolean, hidden: Boolean, lastDay: String?, today: String): Display = when {
        !enabled -> Display.NONE
        !hidden -> Display.GUIDE
        lastDay != today -> Display.DAILY
        else -> Display.NONE
    }
    fun canOptOut(completions: Int)=completions>0
}
