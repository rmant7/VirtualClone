package com.virtualclone.app.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Provides coroutine dispatchers for different use cases in the app.
 */
object AppDispatchers {
    val Main: CoroutineDispatcher = Dispatchers.Main
    val IO: CoroutineDispatcher = Dispatchers.IO
    val Default: CoroutineDispatcher = Dispatchers.Default
    val Unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
