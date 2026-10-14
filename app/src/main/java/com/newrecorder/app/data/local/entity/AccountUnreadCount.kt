package com.newrecorder.app.data.local.entity

/**
 * مشروع عرض لعدد الرسائل غير المقروءة لكل حساب (يُستخدم لشارات الواجهة).
 */
data class AccountUnreadCount(
    val accountId: Long,
    val count: Int
)
