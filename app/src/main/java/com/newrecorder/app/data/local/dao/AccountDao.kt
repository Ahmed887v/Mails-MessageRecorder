package com.newrecorder.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.newrecorder.app.data.local.entity.AccountEntity
import com.newrecorder.app.data.local.entity.AccountWithMessages
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(account: AccountEntity): Long

    @Query("SELECT * FROM accounts WHERE appPackage = :appPackage ORDER BY isCloned ASC, displayName ASC")
    fun observeByApp(appPackage: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE appPackage = :appPackage AND accountKey = :accountKey LIMIT 1")
    suspend fun find(appPackage: String, accountKey: String): AccountEntity?

    @Transaction
    @Query("SELECT * FROM accounts WHERE id = :accountId LIMIT 1")
    suspend fun getWithMessages(accountId: Long): AccountWithMessages?

    @Query("SELECT COUNT(*) FROM accounts WHERE appPackage = :appPackage")
    suspend fun countByApp(appPackage: String): Int
}
