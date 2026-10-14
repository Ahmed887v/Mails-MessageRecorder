package com.newrecorder.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * رسالة/تنبيه واحد تم جمعه من أي مصدر.
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["isRead"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val sender: String,
    val title: String? = null,
    val body: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    /** مصدر الالتقاط: notification / sms / imap */
    val sourceType: String = "notification"
)
