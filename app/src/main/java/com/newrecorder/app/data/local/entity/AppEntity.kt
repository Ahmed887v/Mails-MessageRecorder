package com.newrecorder.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * تصنيف التطبيقات المصدر داخل النظام الشجري (التطبيق -> الحساب -> الرسائل).
 */
enum class SourceCategory {
    GMAIL,
    SMS,
    WHATSAPP,
    MESSENGER,
    INSTAGRAM,
    SNAPCHAT,
    OTHER
}

/**
 * يمثل تطبيقاً مصدراً واحداً (مثال: WhatsApp، Gmail، تطبيق الرسائل النصية).
 * المفتاح الأساسي هو اسم الحزمة (packageName) لتمييزه بشكل فريد.
 */
@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
    val displayName: String,
    val category: SourceCategory,
    val iconKey: String? = null
)
