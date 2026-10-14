package com.newrecorder.app.gmail

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.newrecorder.app.data.preferences.EncryptedTokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenResponse
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * مصادقة Gmail عبر OAuth 2.0 (توصية Google) باستخدام Authorization Code + PKCE.
 * لا تُطلب كلمة مرور المستخدم مطلقاً، ويُخزَّن الرمز مشفّراً على الجهاز.
 *
 * ملاحظة: يجب على المستخدم إنشاء OAuth Client ID من Google Cloud Console
 * (نوع Android) ووضع القيمة في OAUTH_CLIENT_ID قبل البناء.
 */
object OAuthConfig {
    const val OAUTH_CLIENT_ID = "752869896295-ijv4q5k05t9a5j9aik0m1iiofc8hpflc.apps.googleusercontent.com"

    /**
     * عميل نوع Android في جوجل لا يسمح بمخطط (scheme) مخصّص عشوائي،
     * بل بالمخطط المشتق من معكوس Client ID فقط:
     *   com.googleusercontent.apps.<CLIENT_ID_DIGITS>:/oauth2redirect
     * مهم: يجب أن يطابق android:scheme في AndroidManifest.xml هذه القيمة أيضاً.
     */
    val REDIRECT_URI: String =
        "com.googleusercontent.apps." +
            OAUTH_CLIENT_ID.removeSuffix(".apps.googleusercontent.com") +
            ":/oauth2redirect"
    const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
    const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
    const val USERINFO_ENDPOINT = "https://openidconnect.googleapis.com/v1/userinfo"
    const val SCOPE = "https://mail.google.com/ openid email"
}

class GmailOAuthManager(
    private val context: Context,
    private val tokenStore: EncryptedTokenStore
) {

    private val serviceConfig: AuthorizationServiceConfiguration =
        AuthorizationServiceConfiguration(
            Uri.parse(OAuthConfig.AUTH_ENDPOINT),
            Uri.parse(OAuthConfig.TOKEN_ENDPOINT)
        )

    fun authRequestBuilder(): AuthorizationRequest.Builder =
        AuthorizationRequest.Builder(
            serviceConfig,
            OAuthConfig.OAUTH_CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(OAuthConfig.REDIRECT_URI)
        ).setScope(OAuthConfig.SCOPE)
            .setPrompt("consent")

    fun authIntent(builder: AuthorizationRequest.Builder): Intent {
        val request = builder.build()
        val service = AuthorizationService(context)
        return service.getAuthorizationRequestIntent(request)
    }

    /** يستخرج رمز الوصول من استجابة جوجل بعد موافقة المستخدم. */
    suspend fun handleAuthResponse(data: Intent): String? = withContext(Dispatchers.IO) {
        val response = AuthorizationResponse.fromIntent(data)
        if (response == null) {
            AuthorizationException.fromIntent(data)?.let { return@withContext null }
            return@withContext null
        }
        val service = AuthorizationService(context)
        try {
            // تبادل رمز التفويض برمز وصول فعلي.
            val tokenResponse: TokenResponse = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                service.performTokenRequest(response.createTokenExchangeRequest()) { token, ex ->
                    if (token != null) cont.resumeWith(Result.success(token))
                    else cont.resumeWith(Result.failure(ex ?: RuntimeException("token error")))
                }
            }
            val access = tokenResponse.accessToken ?: return@withContext null
            val email = fetchEmail(access) ?: return@withContext null
            val expiry = tokenResponse.accessTokenExpirationTime ?: (System.currentTimeMillis() + 3600_000)
            tokenStore.saveAuth(email, access, tokenResponse.refreshToken, expiry)
            email
        } finally {
            service.dispose()
        }
    }

    /** يجدّد رمز الوصول باستخدام refresh token دون إزعاج المستخدم. */
    suspend fun refreshIfNeeded(email: String): String? = withContext(Dispatchers.IO) {
        val current = tokenStore.accessToken(email)
        val expiry = tokenStore.expiry(email)
        if (current != null && System.currentTimeMillis() < expiry - 60_000) return@withContext current

        val refresh = tokenStore.refreshToken(email) ?: return@withContext current
        val body = "client_id=" + enc(OAuthConfig.OAUTH_CLIENT_ID) +
            "&grant_type=refresh_token" +
            "&refresh_token=" + enc(refresh)

        val conn = (URL(OAuthConfig.TOKEN_ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        OutputStreamWriter(conn.outputStream).use { it.write(body) }
        if (conn.responseCode !in 200..299) return@withContext current
        val json = conn.inputStream.bufferedReader().use(BufferedReader::readText)
        val obj = JSONObject(json)
        val access = obj.optString("access_token")
        val expiresIn = obj.optLong("expires_in", 3600)
        if (access.isBlank()) return@withContext current
        tokenStore.saveAuth(email, access, refresh, System.currentTimeMillis() + expiresIn * 1000)
        access
    }

    private fun fetchEmail(accessToken: String): String? {
        val conn = (URL(OAuthConfig.USERINFO_ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        if (conn.responseCode !in 200..299) return null
        val json = conn.inputStream.bufferedReader().use(BufferedReader::readText)
        return JSONObject(json).optString("email").ifBlank { null }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}
