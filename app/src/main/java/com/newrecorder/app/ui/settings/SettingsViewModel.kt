package com.newrecorder.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.newrecorder.app.data.preferences.CaptureDiagnostics
import com.newrecorder.app.data.preferences.SettingsStore
import com.newrecorder.app.data.repository.InboxRepository
import com.newrecorder.app.gmail.GmailRepository
import com.newrecorder.app.ui.appContainer
import com.newrecorder.app.util.SmsArchive
import com.newrecorder.app.util.WorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val keepAlive: Boolean = true,
    val syncIntervalMinutes: Int = 15,
    val notifyUnread: Boolean = false,
    val gmailAccounts: List<String> = emptyList(),
    val diagnostics: CaptureDiagnostics = CaptureDiagnostics()
)

class SettingsViewModel(
    private val appContext: Context,
    private val settings: SettingsStore,
    private val gmail: GmailRepository,
    private val repository: InboxRepository
) : ViewModel() {

    private val gmailAccounts = MutableStateFlow(gmail.authenticatedEmails().toList())

    val state: StateFlow<SettingsUiState> = combine(
        settings.keepAliveService,
        settings.syncIntervalMinutes,
        settings.notifyUnreadSummary,
        gmailAccounts,
        settings.captureDiagnostics
    ) { keepAlive, interval, notify, accounts, diagnostics ->
        SettingsUiState(keepAlive, interval, notify, accounts, diagnostics)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private val _smsImported = MutableStateFlow<Int?>(null)
    val smsImported: StateFlow<Int?> = _smsImported

    fun setKeepAlive(enabled: Boolean) {
        viewModelScope.launch { settings.setKeepAliveService(enabled) }
    }

    fun setSyncInterval(minutes: Int) {
        viewModelScope.launch {
            settings.setSyncIntervalMinutes(minutes)
            WorkScheduler.scheduleImapSync(appContext, minutes)
        }
    }

    fun setNotifyUnread(enabled: Boolean) {
        viewModelScope.launch { settings.setNotifyUnreadSummary(enabled) }
    }

    fun removeGmail(email: String) {
        viewModelScope.launch {
            gmail.removeAccount(email)
            refreshGmail()
        }
    }

    fun refreshGmail() {
        gmailAccounts.value = gmail.authenticatedEmails().toList()
    }

    fun resetCaptureDiagnostics() {
        viewModelScope.launch { settings.resetCaptureDiagnostics() }
    }

    /** يستورد رسائل SMS الموجودة على الجهاز (يتطلب صلاحية READ_SMS). */
    fun importSmsArchive() {
        viewModelScope.launch {
            val items = runCatching { SmsArchive.readInbox(appContext, 500) }
                .getOrDefault(emptyList())
            items.forEach { repository.ingestSms(it.address, it.body, it.timestamp) }
            _smsImported.value = items.size
        }
    }

    fun clearSmsImported() {
        _smsImported.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer
                SettingsViewModel(
                    appContext = container.appContext,
                    settings = container.settingsStore,
                    gmail = container.gmailRepository,
                    repository = container.repository
                )
            }
        }
    }
}
