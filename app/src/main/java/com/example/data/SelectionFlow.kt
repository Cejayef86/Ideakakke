package com.example.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart

/** Each selection owns its error boundary, so a denied/deleted idea cannot end navigation. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> Flow<String?>.observeSelection(
    empty: T,
    onError: (Throwable) -> Unit,
    observe: (String) -> Flow<T>
): Flow<T> = flatMapLatest { id ->
    if (id == null) flowOf(empty) else observe(id)
        .onStart { emit(empty) }
        .catch { error ->
            if (error is CancellationException) throw error
            onError(error)
            emit(empty)
        }
}
