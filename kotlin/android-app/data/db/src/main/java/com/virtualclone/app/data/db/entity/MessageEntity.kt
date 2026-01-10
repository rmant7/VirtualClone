package com.virtualclone.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),

    val rawMessage: String,
    val author: String,                // "user" or "model"

    /**
     * Persisted model phase:
     * THINKING | STREAMING | DONE
     */
    val phase: String,

    val timestamp: Date = Date(),
    val conversationId: String
)
