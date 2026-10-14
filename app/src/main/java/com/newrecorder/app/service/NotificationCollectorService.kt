package com.newrecorder.app.service

import android.app.Notification
import android.app.Person
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.newrecorder.app.NewRecorderApp
import com.newrecorder.app.util.SourcePackages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * يقرأ تدفق الإشعارات الحيّ على الشاشة (WhatsApp / Messenger / Instagram / Snapchat ...)
 * ويحوّلها إلى رسائل في المستودع الموحد.
 *
 * لتمييز الحسابات المزدوجة (Cloned Apps) نقرأ معرّف الملف الشخصي (profile/user id)
 * من مفتاح الإشعار، وهو المكوّن الأول في الصيغة: userId|packageName|id|tag|uid.
 */
class NotificationCollectorService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate: تم إنشاء خدمة التقاط الإشعارات")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "onListenerConnected: خدمة الإشعارات متصلة")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "onListenerDisconnected: انقطع اتصال خدمة الإشعارات")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val packageName = sbn.packageName ?: return

        // تجاهل إشعاراتنا واحتفظ فقط بالإشعارات القابلة للإزالة (ليست مستمرة).
        if (packageName == applicationContext.packageName) return

        val container = (applicationContext as NewRecorderApp).container

        // تسجيل أن الخدمة استلمت إشعاراً فعلياً (تشخيص) حتى قبل فلترة المحتوى.
        Log.i(TAG, "onNotificationPosted: $packageName clearable=${sbn.isClearable} ongoing=${sbn.isOngoing}")
        scope.launch { container.settingsStore.recordNotificationSeen(packageName, sbn.postTime) }

        if (sbn.isOngoing) return
        if (!sbn.isClearable) return

        val extras = sbn.notification?.extras ?: return
        val parsed = parseNotification(extras) ?: return

        val profileId = profileIdOf(sbn)
        val isCloned = profileId != "0"
        val accountKey = "profile:$profileId"
        val accountLabel = if (isCloned) {
            getString(com.newrecorder.app.R.string.account_cloned, profileId)
        } else {
            getString(com.newrecorder.app.R.string.account_primary)
        }

        val appLabel = resolveAppLabel(packageName)
        val timestamp = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()

        scope.launch {
            container.repository.ingestNotification(
                packageName = packageName,
                appDisplayName = appLabel,
                accountKey = accountKey,
                accountDisplayName = accountLabel,
                sender = parsed.sender,
                title = parsed.title,
                body = parsed.body,
                timestamp = timestamp,
                isCloned = isCloned
            )
            container.settingsStore.recordNotificationCaptured()
            Log.i(TAG, "captured from $packageName: ${parsed.sender} — ${parsed.body.take(40)}")
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private data class Parsed(val sender: String, val title: String?, val body: String)

    private fun parseNotification(extras: Bundle): Parsed? {
        var sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val conversationTitle =
            extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
        var body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        if (!bigText.isNullOrBlank()) body = bigText

        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        if (body.isBlank() && !subText.isNullOrBlank()) body = subText

        // رسائل المحادثة (MessagingStyle) مخزّنة في مصفوفة من Bundle بمفاتيح نصية ثابتة.
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (!messages.isNullOrEmpty()) {
            val last = messages.lastOrNull()
            if (last is Bundle) {
                val personSender = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    last.getParcelable(KEY_SENDER_PERSON) as? Person
                } else {
                    null
                }
                val msgSender = personSender?.name?.toString()
                    ?: last.getCharSequence(KEY_SENDER)?.toString()
                val msgText = last.getCharSequence(KEY_TEXT)?.toString()
                if (!msgSender.isNullOrBlank()) sender = msgSender
                if (!msgText.isNullOrBlank()) body = msgText
            }
        }

        // في المحادثات: العنوان هو اسم المُرسل، والنص هو المحتوى.
        val finalSender = conversationTitle ?: sender
        if (body.isBlank()) return null

        return Parsed(sender = finalSender.ifBlank { "Unknown" }, title = conversationTitle, body = body)
    }

    private fun profileIdOf(sbn: StatusBarNotification): String {
        // الصيغة: userId|packageName|id|tag|uid
        val key = sbn.key
        if (!key.isNullOrEmpty()) {
            val first = key.substringBefore('|')
            if (first.isNotEmpty() && first.all { it.isDigit() }) return first
        }
        return "0"
    }

    private fun resolveAppLabel(packageName: String): String {
        return try {
            val pm = packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            SourcePackages.displayNameOf(packageName)
        }
    }

    companion object {
        // مفاتيح رسائل MessagingStyle (قيم ثابتة في نظام أندرويد).
        private const val KEY_SENDER = "sender"
        private const val KEY_TEXT = "text"
        private const val KEY_SENDER_PERSON = "sender_person"

        const val TAG = "NewRecorder"

        fun isEnabled(context: Context): Boolean {
            val flat = android.provider.Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            ) ?: return false
            return flat.contains(context.packageName)
        }
    }
}
