package com.newrecorder.app.di

import android.content.Context
import com.newrecorder.app.data.preferences.EncryptedTokenStore
import com.newrecorder.app.data.preferences.SettingsStore
import com.newrecorder.app.data.repository.InboxRepository
import com.newrecorder.app.gmail.GmailOAuthManager
import com.newrecorder.app.gmail.GmailRepository

/**
 * حاوية اعتماديات بسيطة (Manual DI) — بديل خفيف عن Hilt مع نفس مبدأ فصل المسؤوليات.
 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    val repository: InboxRepository = InboxRepository(appContext)

    val settingsStore: SettingsStore = SettingsStore(appContext)

    val tokenStore: EncryptedTokenStore = EncryptedTokenStore(appContext)

    val gmailRepository: GmailRepository = GmailRepository(repository, tokenStore)

    val gmailOAuth: GmailOAuthManager = GmailOAuthManager(appContext, tokenStore)
}
