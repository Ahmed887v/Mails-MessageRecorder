package com.newrecorder.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * يمثل حساباً واحداً داخل تطبيق (مثال: بريد معين، رقم هاتف، أو نسخة مزدوجة/Cloned App).
 *
 * [accountKey] يفرّق بين الحسابات فرادةً:
 *  - Gmail:  عنوان البريد الإلكتروني.
 *  - SMS:    رقم الهاتف أو معرّف المرسل.
 *  - WhatsApp/الشات: المعرّف الفريد الذي يمنحه أندرويد لكل ملف تعريف (userId / profile serial)
 *                   لفرز الحساب الأساسي عن الحساب المزدوج.
 */
@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["packageName"],
            childColumns = ["appPackage"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["appPackage"]),
        Index(value = ["appPackage", "accountKey"], unique = true)
    ]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appPackage: String,
    val accountKey: String,
    val displayName: String,
    val isCloned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
