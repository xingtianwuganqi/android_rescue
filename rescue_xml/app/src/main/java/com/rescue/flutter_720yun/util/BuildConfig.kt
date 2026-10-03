package com.rescue.flutter_720yun.util

object AppBuildConfig {
    const val DEBUG: Boolean = false
    val BASEURL: String get() = com.rescue.flutter_720yun.BuildConfig.API_BASE_URL.trimEnd('/')
    const val PRAVICY_URL = "/api/pravicy/"
    const val USERAGREEN_URL = "/api/useragreen/"
    const val ABOUTUS = "/api/aboutus/"
    const val INSTRUCTION = "/api/instruction/"
    const val PREVENTION = "/api/prevention/"
}
