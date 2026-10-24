package com.example.c001apk.compose.logic.state

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Keep already-open feed lists consistent after a server-confirmed deletion. */
object FeedDeletionEvents {
    private val deletions = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val events = deletions.asSharedFlow()

    suspend fun deleted(id: String) {
        deletions.emit(id)
    }
}
