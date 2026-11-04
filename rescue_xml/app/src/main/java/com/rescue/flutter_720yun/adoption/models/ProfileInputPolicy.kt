package com.rescue.flutter_720yun.adoption.models

/** Client entry policy only; do not reinterpret historical server profile completeness. */
object ProfileInputPolicy {
    const val AGE_TITLE = "年龄（周岁，10–100）"
    const val AGE_ERROR = "请输入10–100的整数年龄"
    fun age(value: String): Int? = value.trim().takeIf { it.matches(Regex("[0-9]+")) }
        ?.toIntOrNull()?.takeIf { it in 10..100 }
}
