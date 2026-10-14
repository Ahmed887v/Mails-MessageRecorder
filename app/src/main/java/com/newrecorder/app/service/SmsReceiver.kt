package com.newrecorder.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.newrecorder.app.NewRecorderApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * يلتقط رسائل SMS لحظة وصولها لشبكة الاتصالات (قبل عرضها في تطبيق الرسائل الأصلي),
 * ثم يؤرشفها في الصندوق الموحد.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val container = (context.applicationContext as NewRecorderApp).container
        val pending = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        // تجميع أجزاء الرسالة متعددة الأجزاء بحسب المرسل.
        val grouped = LinkedHashMap<String, StringBuilder>()
        val timestamps = HashMap<String, Long>()
        for (part in messages) {
            val address = part.displayOriginatingAddress ?: part.originatingAddress ?: "Unknown"
            grouped.getOrPut(address) { StringBuilder() }.append(part.displayMessageBody ?: part.messageBody.orEmpty())
            timestamps[address] = part.timestampMillis
        }

        pending.launch {
            grouped.forEach { (address, sb) ->
                container.repository.ingestSms(
                    address = address,
                    body = sb.toString(),
                    timestamp = timestamps[address] ?: System.currentTimeMillis()
                )
            }
        }
    }
}
