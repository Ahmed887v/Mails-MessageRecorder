package com.newrecorder.app.gmail

import java.util.Properties
import javax.mail.Address
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Session
import javax.mail.Store
import javax.mail.internet.InternetAddress
import javax.mail.search.FlagTerm

/**
 * عميل IMAP يتصل بـ Gmail مباشرة عبر XOAUTH2 ويقرأ الرسائل غير المقروءة.
 * هذا هو جسر الحل لمشكلة تأخّر إشعارات Gmail الأصلية: الجلب يتم من التطبيق
 * قبل وصول التنبيه الأصلي، وعبر الخدمة الأمامية يبقى الاتصال حيّاً.
 */
class ImapClient {

    data class Mail(
        val from: String,
        val subject: String,
        val body: String,
        val timestamp: Long
    )

    /**
     * يجلب آخر [limit] رسالة غير مقروءة من صندوق الوارد.
     * كلمة المرور هي رمز الوصول OAuth 2.0 (وليس كلمة السر).
     */
    fun fetchUnread(email: String, accessToken: String, limit: Int = 25): List<Mail> {
        val props = Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.imaps.host", "imap.gmail.com")
            put("mail.imaps.port", "993")
            put("mail.imaps.ssl.enable", "true")
            put("mail.imaps.auth.mechanisms", "XOAUTH2")
            put("mail.imaps.connectiontimeout", "15000")
            put("mail.imaps.timeout", "20000")
        }

        val session = Session.getInstance(props)
        var store: Store? = null
        var folder: Folder? = null
        val result = ArrayList<Mail>()

        try {
            store = session.getStore("imaps")
            store.connect("imap.gmail.com", 993, email, accessToken)

            folder = store.getFolder("INBOX")
            folder.open(Folder.READ_ONLY)

            val unseenTerm = FlagTerm(Flags(Flags.Flag.SEEN), false)
            val unseen = folder.search(unseenTerm)
            val start = maxOf(0, unseen.size - limit)
            val slice = unseen.copyOfRange(start, unseen.size)

            for (msg in slice) {
                val from = addressOf(msg.from, msg.getHeader("From"))
                val subject = msg.subject ?: "(بدون عنوان)"
                val body = extractText(msg)
                val ts = msg.receivedDate?.time ?: msg.sentDate?.time ?: System.currentTimeMillis()
                result.add(Mail(from, subject, body, ts))
            }
        } finally {
            runCatching { folder?.close(false) }
            runCatching { store?.close() }
        }
        return result
    }

    private fun addressOf(addresses: Array<Address>?, header: Array<String>?): String {
        addresses?.firstOrNull()?.let {
            return (it as? InternetAddress)?.address ?: it.toString()
        }
        return header?.firstOrNull() ?: "Unknown"
    }

    private fun extractText(message: Message): String {
        return try {
            val content = message.content
            when (content) {
                is String -> content
                is Multipart -> partText(content)
                else -> content?.toString().orEmpty()
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun partText(multipart: Multipart): String {
        val sb = StringBuilder()
        for (i in 0 until multipart.count) {
            val part = multipart.getBodyPart(i)
            val disposition = part.disposition
            when {
                part.content is String && disposition == null -> sb.append(part.content)
                part.content is Multipart -> sb.append(partText(part.content as Multipart))
                part.contentType?.startsWith("text/plain") == true -> sb.append(part.content.toString())
            }
        }
        val text = sb.toString().trim()
        return if (text.isBlank()) "[رسالة بدون نص]" else text
    }
}
