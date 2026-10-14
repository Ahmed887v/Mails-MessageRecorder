package com.newrecorder.app.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.newrecorder.app.service.ImapSyncWorker
import java.util.concurrent.TimeUnit

/**
 * جدولة المزامنة الدورية عبر WorkManager.
 */
object WorkScheduler {

    fun scheduleImapSync(context: Context, intervalMinutes: Int) {
        val request = PeriodicWorkRequestBuilder<ImapSyncWorker>(
            intervalMinutes.coerceAtLeast(15).toLong(), TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ImapSyncWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelImapSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ImapSyncWorker.UNIQUE_NAME)
    }
}
