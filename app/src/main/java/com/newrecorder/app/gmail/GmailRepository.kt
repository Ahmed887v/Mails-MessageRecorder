package com.newrecorder.app.gmail

import com.newrecorder.app.data.preferences.EncryptedTokenStore
import com.newrecorder.app.data.repository.InboxRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * يربط المصادقة (OAuth) بالجلب (IMAP) وبالمستودع المحلي.
 */
class GmailRepository(
    private val repository: InboxRepository,
    private val tokenStore: EncryptedTokenStore
) {

    private val imap = ImapClient()

    fun authenticatedEmails(): Set<String> = tokenStore.authenticatedEmails()

    /** يهيّئ صف الحساب في قاعدة البيانات فور الربط ليظهر مباشرة في التبويب. */
    suspend fun ensureGmailAccount(email: String) {
        repository.registerGmailAccount(email)
    }

    /** يجلب كل الحسابات المرتبطة دفعة واحدة (يُستخدم بعد الربط وعند بدء المزامنة). */
    suspend fun syncAll(oauth: GmailOAuthManager): Int {
        var total = 0
        for (email in authenticatedEmails()) {
            val token = oauth.refreshIfNeeded(email) ?: continue
            total += runCatching { syncAccount(email, token) }.getOrDefault(0)
        }
        return total
    }

    suspend fun storeToken(email: String, accessToken: String, refreshToken: String?, expiry: Long) {
        tokenStore.saveAuth(email, accessToken, refreshToken, expiry)
        repository.registerGmailAccount(email)
    }

    /** جلب البريد غير المقروء لحساب واحد ثم أرشفته محلياً. */
    suspend fun syncAccount(email: String, accessToken: String): Int =
        withContext(Dispatchers.IO) {
            val mails = imap.fetchUnread(email, accessToken)
            mails.forEach { mail ->
                repository.ingestMail(
                    email = email,
                    sender = mail.from,
                    subject = mail.subject,
                    body = mail.body,
                    timestamp = mail.timestamp
                )
            }
            mails.size
        }

    suspend fun removeAccount(email: String) {
        tokenStore.remove(email)
    }
}
