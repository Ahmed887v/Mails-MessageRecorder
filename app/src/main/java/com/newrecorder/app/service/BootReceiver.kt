package com.newrecorder.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.newrecorder.app.NewRecorderApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * يعيد تشغيل الخدمة الدائمة بعد إقلاع الجهاز إن كان الخيار مفعّلاً.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val app = context.applicationContext as NewRecorderApp
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                if (app.container.settingsStore.keepAliveService.first()) {
                    SyncService.start(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
