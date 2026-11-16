package com.rescue.flutter_720yun.adoption.models

import com.rescue.flutter_720yun.R
import com.rescue.flutter_720yun.util.localized

/** Client entry policy only; do not reinterpret historical server profile completeness. */
object ProfileInputPolicy {
    val AGE_TITLE get() = localized(R.string.age_title,"年龄（周岁，10–100）")
    val AGE_ERROR get() = localized(R.string.age_error,"请输入10–100的整数年龄")
    fun age(value: String): Int? = value.trim().takeIf { it.matches(Regex("[0-9]+")) }
        ?.toIntOrNull()?.takeIf { it in 10..100 }
}
