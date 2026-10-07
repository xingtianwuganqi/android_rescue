@file:OptIn(kotlinx.coroutines.InternalCoroutinesApi::class)

package com.rescue.flutter_720yun.adoption.qa

import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.internal.MainDispatcherFactory
import kotlin.coroutines.CoroutineContext

/** JVM-only dispatcher; no Android Looper or new dependency is needed for this audit. */
class QaMainDispatcherFactory : MainDispatcherFactory {
    override val loadPriority = Int.MAX_VALUE
    override fun hintOnError() = "Only enable this dispatcher for the JVM adoption audit."
    override fun createDispatcher(allFactories: List<MainDispatcherFactory>): MainCoroutineDispatcher =
        object : MainCoroutineDispatcher() {
            override val immediate: MainCoroutineDispatcher get() = this
            override fun isDispatchNeeded(context: CoroutineContext) = false
            override fun dispatch(context: CoroutineContext, block: Runnable) = block.run()
        }
}
