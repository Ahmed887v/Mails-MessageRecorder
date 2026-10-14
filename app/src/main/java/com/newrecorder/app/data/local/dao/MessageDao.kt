package com.newrecorder.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.newrecorder.app.data.local.entity.AccountUnreadCount
import com.newrecorder.app.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE accountId = :accountId ORDER BY timestamp DESC")
    fun observeByAccount(accountId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE accountId = :accountId AND isRead = 0 ORDER BY timestamp DESC")
    fun observeUnreadByAccount(accountId: Long): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages WHERE accountId = :accountId AND isRead = 0")
    fun observeUnreadCount(accountId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE accountId IN (SELECT id FROM accounts WHERE appPackage = :appPackage) AND isRead = 0")
    fun observeUnreadCountByApp(appPackage: String): Flow<Int>

    @Query("SELECT accountId, COUNT(*) AS count FROM messages WHERE isRead = 0 GROUP BY accountId")
    fun observeUnreadCounts(): Flow<List<AccountUnreadCount>>

    @Query("UPDATE messages SET isRead = 1 WHERE accountId = :accountId")
    suspend fun markAccountRead(accountId: Long)

    @Query("UPDATE messages SET isRead = 1 WHERE id = :messageId")
    suspend fun markRead(messageId: Long)

    @Query("DELETE FROM messages WHERE accountId = :accountId")
    suspend fun clearAccount(accountId: Long)

    @Query("SELECT * FROM messages WHERE accountId = :accountId ORDER BY timestamp DESC LIMIT 1")
    suspend fun latestForAccount(accountId: Long): MessageEntity?
}
