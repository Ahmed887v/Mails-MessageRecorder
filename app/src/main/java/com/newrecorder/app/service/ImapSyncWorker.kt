package com.newrecorder.app.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.newrecorder.app.NewRecorderApp
import com.newrecorder.app.gmail.GmailOAuthManager

/**
 * مهمة دورية (WorkManager) غير لحظية كمكمّل للخدمة الأمامية،
 * تضمن مزامنة البريد حتى لو أوقف النظام الخدمة مؤقتاً.
 */
class ImapSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as NewRecorderApp
        val gmail = app.container.gmailRepository
        val oauth = GmailOAuthManager(applicationContext, app.container.tokenStore)
        var synced = 0
        for (email in gmail.authenticatedEmails()) {
            val token = oauth.refreshIfNeeded(email) ?: continue
            synced += runCatching { gmail.syncAccount(email, token) }.getOrDefault(0)
        }
        return Result.success(androidx.work.workDataOf("synced" to synced))
    }

    companion object {
        const val UNIQUE_NAME = "imap_periodic_sync"
    }
}
