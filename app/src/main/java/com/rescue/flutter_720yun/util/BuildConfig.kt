package com.rescue.flutter_720yun.util

object BuildConfig {
    const val DEBUG: Boolean = true
    val BASEURL: String get() = if (DEBUG) "http://test.rxswift.cn" else "https://rescue.rxswift.cn"
    const val PRAVICY_URL = "/api/pravicy/"
    const val USERAGREEN_URL = "/api/useragreen/"
    const val ABOUTUS = "/api/aboutus/"
    const val INSTRUCTION = "/api/instruction/"
    const val PREVENTION = "/api/prevention/"
}