package com.newrecorder.app.data.repository

import android.content.Context
import com.newrecorder.app.data.local.NewRecorderDatabase
import com.newrecorder.app.data.local.entity.AccountEntity
import com.newrecorder.app.data.local.entity.AppEntity
import com.newrecorder.app.data.local.entity.AccountWithMessages
import com.newrecorder.app.data.local.entity.AppWithAccounts
import com.newrecorder.app.data.local.entity.MessageEntity
import com.newrecorder.app.data.local.entity.SourceCategory
import com.newrecorder.app.util.SourcePackages
import kotlinx.coroutines.flow.Flow

/**
 * المستودع الوحيد الذي تمرّ عبره كل عمليات القراءة/الكتابة.
 * يوحد مصادر الالتقاط الثلاثة (الإشعارات، SMS، IMAP/Gmail) في شجرة واحدة.
 */
class InboxRepository(private val context: Context) {

    private val db = NewRecorderDatabase.get(context)
    private val appDao = db.appDao()
    private val accountDao = db.accountDao()
    private val messageDao = db.messageDao()

    // ---------- القراءة (Flows للواجهة) ----------

    fun observeTree(): Flow<List<AppWithAccounts>> = appDao.observeAppsWithAccounts()

    fun observeAccounts(appPackage: String): Flow<List<AccountEntity>> =
        accountDao.observeByApp(appPackage)

    fun observeMessages(accountId: Long): Flow<List<MessageEntity>> =
        messageDao.observeByAccount(accountId)

    fun observeUnreadMessages(accountId: Long): Flow<List<MessageEntity>> =
        messageDao.observeUnreadByAccount(accountId)

    fun observeUnreadCount(accountId: Long): Flow<Int> =
        messageDao.observeUnreadCount(accountId)

    fun observeUnreadCountByApp(appPackage: String): Flow<Int> =
        messageDao.observeUnreadCountByApp(appPackage)

    fun observeUnreadCounts(): Flow<List<com.newrecorder.app.data.local.entity.AccountUnreadCount>> =
        messageDao.observeUnreadCounts()

    suspend fun accountWithMessages(accountId: Long): AccountWithMessages? =
        accountDao.getWithMessages(accountId)

    // ---------- إنشاء التطبيقات والحسابات ----------

    suspend fun ensureApp(packageName: String, displayName: String): AppEntity {
        appDao.findByPackage(packageName)?.let { return it }
        val app = AppEntity(
            packageName = packageName,
            displayName = displayName,
            category = SourcePackages.categoryOf(packageName)
        )
        appDao.insert(app)
        return appDao.findByPackage(packageName) ?: app
    }

    suspend fun ensureAccount(
        appPackage: String,
        accountKey: String,
        displayName: String,
        isCloned: Boolean = false
    ): AccountEntity {
        accountDao.find(appPackage, accountKey)?.let { return it }
        val account = AccountEntity(
            appPackage = appPackage,
            accountKey = accountKey,
            displayName = displayName,
            isCloned = isCloned
        )
        accountDao.insert(account)
        return accountDao.find(appPackage, accountKey) ?: account
    }

    suspend fun registerGmailAccount(email: String): AccountEntity =
        ensureAccount(
            appPackage = SourcePackages.GMAIL,
            accountKey = email,
            displayName = email
        )

    // ---------- الالتقاط ----------

    /** تنبيه من NotificationListenerService. */
    suspend fun ingestNotification(
        packageName: String,
        appDisplayName: String,
        accountKey: String,
        accountDisplayName: String,
        sender: String,
        title: String?,
        body: String,
        timestamp: Long,
        isCloned: Boolean = false
    ) {
        if (body.isBlank() && title.isNullOrBlank()) return
        ensureApp(packageName, appDisplayName)
        val account = ensureAccount(packageName, accountKey, accountDisplayName, isCloned)
        val text = body.ifBlank { title.orEmpty() }
        if (isDuplicate(account.id, sender, text, timestamp)) return
        messageDao.insert(
            MessageEntity(
                accountId = account.id,
                sender = sender.ifBlank { accountDisplayName },
                title = title,
                body = text,
                timestamp = timestamp,
                sourceType = "notification"
            )
        )
    }

    /** رسالة SMS عبر البث المباشر. */
    suspend fun ingestSms(address: String, body: String, timestamp: Long) {
        if (body.isBlank()) return
        ensureApp(SourcePackages.SMS, "SMS")
        val account = ensureAccount(SourcePackages.SMS, address, address)
        if (isDuplicate(account.id, address, body, timestamp)) return
        messageDao.insert(
            MessageEntity(
                accountId = account.id,
                sender = address,
                title = null,
                body = body,
                timestamp = timestamp,
                sourceType = "sms"
            )
        )
    }

    /** رسالة بريد مجلوبة عبر IMAP. */
    suspend fun ingestMail(
        email: String,
        sender: String,
        subject: String?,
        body: String,
        timestamp: Long
    ) {
        ensureApp(SourcePackages.GMAIL, "Gmail")
        val account = ensureAccount(SourcePackages.GMAIL, email, email)
        if (isDuplicate(account.id, sender, body, timestamp)) return
        messageDao.insert(
            MessageEntity(
                accountId = account.id,
                sender = sender,
                title = subject,
                body = body,
                timestamp = timestamp,
                sourceType = "imap"
            )
        )
    }

    // ---------- تعديل الحالة ----------

    suspend fun markAccountRead(accountId: Long) = messageDao.markAccountRead(accountId)

    suspend fun markMessageRead(messageId: Long) = messageDao.markRead(messageId)

    suspend fun clearAccount(accountId: Long) = messageDao.clearAccount(accountId)

    // ---------- تهيئة أولية ----------

    /** إنشاء صفوف التطبيقات المعروفة مسبقاً لتظهر التبويبات حتى قبل وصول أي رسالة. */
    suspend fun seedDefaultApps() {
        ensureApp(SourcePackages.GMAIL, "Gmail")
        ensureApp(SourcePackages.SMS, "SMS")
        ensureApp("com.whatsapp", "WhatsApp")
        ensureApp("com.facebook.orca", "Messenger")
        ensureApp("com.instagram.android", "Instagram")
        ensureApp("com.snapchat.android", "Snapchat")
    }

    suspend fun appByCategory(category: SourceCategory): AppEntity? = appDao.findByCategory(category)

    private suspend fun isDuplicate(
        accountId: Long,
        sender: String,
        body: String,
        timestamp: Long
    ): Boolean {
        val latest = messageDao.latestForAccount(accountId) ?: return false
        val sameContent = latest.body == body && latest.sender == sender
        val withinWindow = kotlin.math.abs(latest.timestamp - timestamp) < 3_000L
        return sameContent && withinWindow
    }
}
