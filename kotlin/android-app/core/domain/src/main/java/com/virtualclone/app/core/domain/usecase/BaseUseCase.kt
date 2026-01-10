package com.virtualclone.app.core.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Lightweight helper base that runs blocks on provided dispatcher.
 * Subclasses receive the dispatcher (injected) and call runOnDispatcher.
 */
abstract class BaseUseCase(protected val dispatcher: CoroutineDispatcher) {
    protected suspend fun <T> runOnDispatcher(block: suspend () -> T): T =
        withContext(dispatcher) { block() }
}