package com.newrecorder.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.newrecorder.app.data.preferences.SettingsStore
import com.newrecorder.app.gmail.GmailOAuthManager
import com.newrecorder.app.gmail.GmailRepository
import com.newrecorder.app.service.SyncService
import com.newrecorder.app.ui.appContainer
import com.newrecorder.app.util.WorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * يدير تدفق "تسجيل الدخول باستخدام جوجل" من واجهة Compose.
 * بعد نجاح الربط: يهيّئ الحساب، يجلب الرسائل فوراً، ويشغّل المزامنة الدورية.
 */
class GmailAuthViewModel(
    private val appContext: Context,
    private val oauth: GmailOAuthManager,
    private val gmail: GmailRepository,
    private val settings: SettingsStore
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun authRequestIntent() = oauth.authIntent(oauth.authRequestBuilder())

    fun handleResult(data: android.content.Intent) {
        viewModelScope.launch {
            val email = oauth.handleAuthResponse(data)
            if (email == null) {
                _message.value = "تعذّر إتمام تسجيل الدخول"
                return@launch
            }

            // 1) أظهر الحساب مباشرة في تبويب Gmail.
            gmail.ensureGmailAccount(email)
            _message.value = "تم ربط $email، جارٍ جلب الرسائل…"

            // 2) جلب فوري لكل الحسابات المرتبطة.
            val synced = runCatching { gmail.syncAll(oauth) }.getOrDefault(0)
            _message.value = "تم ربط $email (تم جلب $synced رسالة)"

            // 3) ضمان استمرار المزامنة في الخلفية (خدمة دائمة + مهمة دورية).
            runCatching { SyncService.start(appContext) }
            WorkScheduler.scheduleImapSync(appContext, settings.syncIntervalMinutes.first())
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer
                GmailAuthViewModel(
                    appContext = container.appContext,
                    oauth = container.gmailOAuth,
                    gmail = container.gmailRepository,
                    settings = container.settingsStore
                )
            }
        }
    }
}
