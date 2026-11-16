package com.rescue.flutter_720yun.util

import androidx.annotation.StringRes
import com.rescue.flutter_720yun.BaseApplication

/**
 * Resolves a string resource when an Android context is available, otherwise
 * returns [fallback]. Pure Kotlin policies and repositories are also exercised
 * by JVM tests where no Application is initialized, so the Chinese source text
 * remains the deterministic fallback there.
 */
fun localized(@StringRes id: Int, fallback: String, vararg args: Any): String =
    runCatching { BaseApplication.context.getString(id, *args) }.getOrDefault(fallback)
