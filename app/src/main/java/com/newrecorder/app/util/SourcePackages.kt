package com.newrecorder.app.util

import com.newrecorder.app.data.local.entity.SourceCategory

/**
 * خرائط معروفة بين أسماء حزم التطبيقات الشائعة والتصنيف الداخلي.
 */
object SourcePackages {

    const val SMS = "com.android.providers.telephony.sms"
    const val GMAIL = "com.google.android.gm"

    private val map: Map<String, SourceCategory> = mapOf(
        GMAIL to SourceCategory.GMAIL,
        "com.google.android.apps.gm" to SourceCategory.GMAIL,
        SMS to SourceCategory.SMS,
        "com.google.android.apps.messaging" to SourceCategory.SMS,
        "com.whatsapp" to SourceCategory.WHATSAPP,
        "com.whatsapp.w4b" to SourceCategory.WHATSAPP,
        "com.facebook.orca" to SourceCategory.MESSENGER,
        "com.facebook.katana" to SourceCategory.MESSENGER,
        "com.instagram.android" to SourceCategory.INSTAGRAM,
        "com.snapchat.android" to SourceCategory.SNAPCHAT
    )

    private val displayNames: Map<String, String> = mapOf(
        GMAIL to "Gmail",
        SMS to "SMS",
        "com.google.android.apps.messaging" to "SMS",
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp Business",
        "com.facebook.orca" to "Messenger",
        "com.facebook.katana" to "Facebook",
        "com.instagram.android" to "Instagram",
        "com.snapchat.android" to "Snapchat"
    )

    fun categoryOf(packageName: String): SourceCategory =
        map[packageName] ?: SourceCategory.OTHER

    /** يقترح الاسم الافتراضي قبل قراءة اسم التطبيق الحقيقي من PackageManager. */
    fun displayNameOf(packageName: String): String =
        displayNames[packageName] ?: packageName.substringAfterLast('.')
}
