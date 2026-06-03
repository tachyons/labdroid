package `in`.aboobacker.labdroid.data.repository

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.aboobacker.labdroid.BuildConfig
import `in`.aboobacker.labdroid.data.local.AuthPreferences
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authPreferences: AuthPreferences
) {
    private val TAG = "AuthRepository"
    private var serviceConfig: AuthorizationServiceConfiguration? = null
    private val authService = AuthorizationService(context)

    private val clientId = BuildConfig.GITLAB_CLIENT_ID
    private val redirectUri = "in.aboobacker.labdroid://oauth".toUri()

    private suspend fun getServiceConfig(): AuthorizationServiceConfiguration =
        suspendCancellableCoroutine { continuation ->
            if (serviceConfig != null) {
                continuation.resume(serviceConfig!!)
                return@suspendCancellableCoroutine
            }

            AuthorizationServiceConfiguration.fetchFromIssuer(
                "https://gitlab.com".toUri()
            ) { config, ex ->
                if (config != null) {
                    serviceConfig = config
                    continuation.resume(config)
                } else {
                    Log.w(TAG, "Failed to fetch discovery document, using fallback", ex)
                    val fallback = AuthorizationServiceConfiguration(
                        "https://gitlab.com/oauth/authorize".toUri(),
                        "https://gitlab.com/oauth/token".toUri()
                    )
                    continuation.resume(fallback)
                }
            }
        }

    fun getAuthorizationRequest(): AuthorizationRequest {
        // We use a simplified version here because this is called synchronously for the intent
        // In a real app, you might want to fetch config before showing login
        val fallbackConfig = AuthorizationServiceConfiguration(
            "https://gitlab.com/oauth/authorize".toUri(),
            "https://gitlab.com/oauth/token".toUri()
        )
        return AuthorizationRequest.Builder(
            serviceConfig ?: fallbackConfig,
            clientId,
            ResponseTypeValues.CODE,
            redirectUri
        ).setScope("api read_user openid profile email")
            .build()
    }

    suspend fun exchangeCodeForToken(response: AuthorizationResponse): Result<Boolean> =
        suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "Exchanging code for token")
            val tokenRequest = response.createTokenExchangeRequest()
            authService.performTokenRequest(tokenRequest) { tokenResponse, exception ->
                val authState = AuthState(response, tokenResponse, exception)
                if (tokenResponse != null) {
                    authPreferences.saveAuthState(authState)
                    continuation.resume(Result.success(true))
                } else {
                    continuation.resume(
                        Result.failure(
                            exception ?: Exception("Token exchange failed")
                        )
                    )
                }
            }
        }

    fun getAccessToken(): String? = authPreferences.getAccessToken()

    fun getAccessTokenFlow() = authPreferences.authStateFlow

    fun getUsername(): String? = authPreferences.getUsername()

    fun saveUsername(username: String?) = authPreferences.saveUsername(username)

    suspend fun refreshToken(): Result<String?> = suspendCancellableCoroutine { continuation ->
        val authState = authPreferences.authStateFlow.value
        if (authState == null || authState.refreshToken == null) {
            continuation.resume(Result.failure(Exception("No refresh token available")))
            return@suspendCancellableCoroutine
        }

        val tokenRequest = authState.createTokenRefreshRequest()
        authService.performTokenRequest(tokenRequest) { response, ex ->
            authState.update(response, ex)
            if (response != null) {
                authPreferences.saveAuthState(authState)
                continuation.resume(Result.success(response.accessToken))
            } else {
                continuation.resume(Result.failure(ex ?: Exception("Token refresh failed")))
            }
        }
    }

    fun logout() {
        authPreferences.clear()
    }
}
