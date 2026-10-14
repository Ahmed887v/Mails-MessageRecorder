package com.newrecorder.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * علاقة تطبيق واحد بحساباته (مستوى أول من الشجرة).
 */
data class AppWithAccounts(
    @Embedded val app: AppEntity,
    @Relation(
        parentColumn = "packageName",
        entityColumn = "appPackage"
    )
    val accounts: List<AccountEntity>
)

/**
 * علاقة حساب واحد برسائله (المستوى الثاني من الشجرة).
 */
data class AccountWithMessages(
    @Embedded val account: AccountEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "accountId"
    )
    val messages: List<MessageEntity>
)
