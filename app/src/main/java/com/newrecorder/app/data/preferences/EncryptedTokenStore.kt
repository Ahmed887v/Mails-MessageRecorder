package com.newrecorder.app.data.preferences

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * تخزين آمن ومشفّر لرموز OAuth الخاصة بـ Gmail (لا تُخزَّن كلمات المرور أبداً).
 */
class EncryptedTokenStore(context: Context) {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "newrecorder_tokens",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveAuth(
        email: String,
        accessToken: String,
        refreshToken: String?,
        expiry: Long
    ) {
        prefs.edit()
            .putString("${email}_access", accessToken)
            .putString("${email}_refresh", refreshToken)
            .putLong("${email}_expiry", expiry)
            .apply()
    }

    fun accessToken(email: String): String? = prefs.getString("${email}_access", null)

    fun refreshToken(email: String): String? = prefs.getString("${email}_refresh", null)

    fun expiry(email: String): Long = prefs.getLong("${email}_expiry", 0L)

    fun authenticatedEmails(): Set<String> =
        prefs.all.keys
            .filter { it.endsWith("_access") }
            .map { it.removeSuffix("_access") }
            .toSet()

    fun remove(email: String) {
        prefs.edit()
            .remove("${email}_access")
            .remove("${email}_refresh")
            .remove("${email}_expiry")
            .apply()
    }
}
