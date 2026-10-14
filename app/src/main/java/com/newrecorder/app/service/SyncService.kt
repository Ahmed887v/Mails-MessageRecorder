package com.newrecorder.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.newrecorder.app.MainActivity
import com.newrecorder.app.NewRecorderApp
import com.newrecorder.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * خدمة أمامية دائمة بإشعار مرئي.
 * الغرض: منع النظام من تنويم التطبيق (Doze) وإبقاء جلب IMAP والـ NotificationListener
 * يعملان في الخلفية بلا انقطاع — وهذه إحدى ركائز حل مشكلة تأخّر الإشعارات.
 */
class SyncService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundInternal(getString(R.string.service_running))
        startSyncLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (loopJob == null) startSyncLoop()
        return START_STICKY
    }

    private fun startSyncLoop() {
        val app = applicationContext as NewRecorderApp
        loopJob = scope.launch {
            while (true) {
                val interval = app.container.settingsStore.syncIntervalMinutes.first()
                runCatching { runSyncPass() }
                delay(interval * 60_000L)
            }
        }
    }

    private suspend fun runSyncPass() {
        val app = applicationContext as NewRecorderApp
        val gmail = app.container.gmailRepository
        val oauth = com.newrecorder.app.gmail.GmailOAuthManager(applicationContext, app.container.tokenStore)
        for (email in gmail.authenticatedEmails()) {
            val token = oauth.refreshIfNeeded(email) ?: continue
            runCatching { gmail.syncAccount(email, token) }
        }
        app.container.settingsStore.markSynced()
    }

    private fun startForegroundInternal(text: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification: Notification = NotificationCompat.Builder(this, NewRecorderApp.CHANNEL_PERSISTENT)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_inbox)
            .setContentIntent(pending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, SyncService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SyncService::class.java))
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }
}
