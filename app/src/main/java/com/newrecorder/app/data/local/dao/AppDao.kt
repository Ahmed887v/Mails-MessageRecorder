package com.newrecorder.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.newrecorder.app.data.local.entity.AppEntity
import com.newrecorder.app.data.local.entity.AppWithAccounts
import com.newrecorder.app.data.local.entity.SourceCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(app: AppEntity): Long

    @Transaction
    @Query("SELECT * FROM apps ORDER BY displayName ASC")
    fun observeAppsWithAccounts(): Flow<List<AppWithAccounts>>

    @Query("SELECT * FROM apps WHERE category = :category LIMIT 1")
    suspend fun findByCategory(category: SourceCategory): AppEntity?

    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    suspend fun findByPackage(packageName: String): AppEntity?
}
