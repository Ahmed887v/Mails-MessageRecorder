package com.newrecorder.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.newrecorder.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NewRecorderApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        createNotificationChannels()

        appScope.launch {
            container.repository.seedDefaultApps()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val persistent = NotificationChannel(
            CHANNEL_PERSISTENT,
            getString(R.string.channel_persistent_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_persistent_desc)
            setShowBadge(false)
        }

        val alerts = NotificationChannel(
            CHANNEL_ALERTS,
            getString(R.string.channel_alerts_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.channel_alerts_desc)
        }

        nm.createNotificationChannel(persistent)
        nm.createNotificationChannel(alerts)
    }

    companion object {
        const val CHANNEL_PERSISTENT = "newrecorder_service"
        const val CHANNEL_ALERTS = "newrecorder_alerts"
    }
}
