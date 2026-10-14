package com.newrecorder.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "newrecorder_settings")

/** تشخيص حيّ لخدمة التقاط الإشعارات (تظهر قيمه في شاشة الإعدادات). */
data class CaptureDiagnostics(
    val lastSeenPackage: String = "",
    val lastSeenAt: Long = 0L,
    val capturedCount: Int = 0
)

/**
 * تخزين بسيط لإعدادات التطبيق عبر DataStore.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val KEEP_ALIVE = booleanPreferencesKey("keep_alive_service")
        val SYNC_INTERVAL = intPreferencesKey("imap_sync_interval_min")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LAST_SYNC = longPreferencesKey("last_imap_sync")
        val NOTIFY_UNREAD = booleanPreferencesKey("notify_unread_summary")
        val LAST_SEEN_PKG = stringPreferencesKey("capture_last_seen_pkg")
        val LAST_SEEN_AT = longPreferencesKey("capture_last_seen_at")
        val CAPTURED_COUNT = intPreferencesKey("capture_count")
    }

    val keepAliveService: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.KEEP_ALIVE] ?: true }

    val syncIntervalMinutes: Flow<Int> =
        context.dataStore.data.map { it[Keys.SYNC_INTERVAL] ?: 15 }

    val onboardingDone: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    val lastImapSync: Flow<Long> =
        context.dataStore.data.map { it[Keys.LAST_SYNC] ?: 0L }

    val notifyUnreadSummary: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.NOTIFY_UNREAD] ?: false }

    suspend fun setKeepAliveService(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_ALIVE] = enabled }
    }

    suspend fun setSyncIntervalMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.SYNC_INTERVAL] = minutes.coerceIn(5, 120) }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun markSynced(now: Long = System.currentTimeMillis()) {
        context.dataStore.edit { it[Keys.LAST_SYNC] = now }
    }

    suspend fun setNotifyUnreadSummary(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_UNREAD] = enabled }
    }

    // ---------- تشخيص خدمة التقاط الإشعارات ----------

    val captureDiagnostics: Flow<CaptureDiagnostics> =
        context.dataStore.data.map { prefs ->
            CaptureDiagnostics(
                lastSeenPackage = prefs[Keys.LAST_SEEN_PKG] ?: "",
                lastSeenAt = prefs[Keys.LAST_SEEN_AT] ?: 0L,
                capturedCount = prefs[Keys.CAPTURED_COUNT] ?: 0
            )
        }

    /** يُسجّل أن خدمة الإشعارات استلمت إشعاراً من تطبيق ما (حتى لو رُفض لاحقاً). */
    suspend fun recordNotificationSeen(packageName: String, now: Long = System.currentTimeMillis()) {
        context.dataStore.edit {
            it[Keys.LAST_SEEN_PKG] = packageName
            it[Keys.LAST_SEEN_AT] = now
        }
    }

    /** يُسجّل أن إشعاراً حُوِّل فعلياً إلى رسالة في المستودع. */
    suspend fun recordNotificationCaptured() {
        context.dataStore.edit {
            it[Keys.CAPTURED_COUNT] = (it[Keys.CAPTURED_COUNT] ?: 0) + 1
        }
    }

    suspend fun resetCaptureDiagnostics() {
        context.dataStore.edit {
            it.remove(Keys.LAST_SEEN_PKG)
            it.remove(Keys.LAST_SEEN_AT)
            it.remove(Keys.CAPTURED_COUNT)
        }
    }
}
