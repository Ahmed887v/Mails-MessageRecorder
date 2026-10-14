package com.newrecorder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.newrecorder.app.service.SyncService
import com.newrecorder.app.ui.navigation.AppNavHost
import com.newrecorder.app.ui.theme.NewRecorderTheme
import com.newrecorder.app.util.WorkScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startBackgroundSync()
        setContent {
            NewRecorderTheme {
                AppNavHost()
            }
        }
    }

    /**
     * يضمن عمل المزامنة الخلفية فور فتح التطبيق (لا ينتظر إعادة تشغيل الهاتف):
     * - مهمة دورية عبر WorkManager (تبقى حتى بعد إعادة الإقلاع).
     * - الخدمة الأمامية الدائمة إن كان الخيار مفعّلاً.
     */
    private fun startBackgroundSync() {
        val app = application as NewRecorderApp
        lifecycleScope.launch {
            val container = app.container
            WorkScheduler.scheduleImapSync(
                this@MainActivity,
                container.settingsStore.syncIntervalMinutes.first()
            )
            if (container.settingsStore.keepAliveService.first()) {
                runCatching { SyncService.start(this@MainActivity) }
            }
        }
    }
}
