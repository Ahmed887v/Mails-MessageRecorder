package com.newrecorder.app.util

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * أرشفة الرسائل النصية القديمة الموجودة على الجهاز (تتطلب صلاحية READ_SMS).
 */
object SmsArchive {

    data class ArchivedSms(val address: String, val body: String, val timestamp: Long)

    suspend fun readInbox(context: Context, limit: Int = 200): List<ArchivedSms> =
        withContext(Dispatchers.IO) {
            val result = ArrayList<ArchivedSms>()
            val projection = arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            )
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT $limit"
            )?.use { cursor ->
                val idxAddress = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val idxBody = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val idxDate = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                while (cursor.moveToNext()) {
                    result.add(
                        ArchivedSms(
                            address = cursor.getString(idxAddress) ?: "Unknown",
                            body = cursor.getString(idxBody).orEmpty(),
                            timestamp = cursor.getLong(idxDate)
                        )
                    )
                }
            }
            result
        }

    @Suppress("unused")
    val contentUri: Uri = Telephony.Sms.CONTENT_URI
}
