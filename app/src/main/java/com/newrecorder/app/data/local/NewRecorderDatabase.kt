package com.newrecorder.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.newrecorder.app.data.local.dao.AccountDao
import com.newrecorder.app.data.local.dao.AppDao
import com.newrecorder.app.data.local.dao.MessageDao
import com.newrecorder.app.data.local.entity.AccountEntity
import com.newrecorder.app.data.local.entity.AppEntity
import com.newrecorder.app.data.local.entity.MessageEntity

/**
 * قاعدة البيانات المحلية (Room فوق SQLite) — كل البيانات تبقى على الجهاز.
 */
@Database(
    entities = [AppEntity::class, AccountEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NewRecorderDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao
    abstract fun accountDao(): AccountDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: NewRecorderDatabase? = null

        fun get(context: Context): NewRecorderDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NewRecorderDatabase::class.java,
                    "newrecorder.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
