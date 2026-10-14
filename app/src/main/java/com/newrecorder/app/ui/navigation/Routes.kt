package com.newrecorder.app.ui.navigation

import android.net.Uri

object Routes {
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"

    const val ACCOUNTS = "accounts/{packageName}"
    fun accounts(packageName: String): String = "accounts/${Uri.encode(packageName)}"

    const val MESSAGES = "messages/{accountId}"
    fun messages(accountId: Long): String = "messages/$accountId"
}
