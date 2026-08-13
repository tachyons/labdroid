package `in`.aboobacker.labdroid.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.openid.appauth.AuthState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences = try {
        createSharedPreferences(context)
    } catch (e: Exception) {
        // If creation fails, the KeyStore or the file might be corrupted.
        // We clear the preferences and try to create it again.
        clearSharedPreferences(context)
        try {
            createSharedPreferences(context)
        } catch (retryException: Exception) {
            // Fallback to regular SharedPreferences if encryption is completely broken on the device.
            // This is a last resort to prevent the app from crashing.
            context.getSharedPreferences("auth_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    private val _authStateFlow = MutableStateFlow<AuthState?>(loadAuthState())
    val authStateFlow = _authStateFlow.asStateFlow()

    private fun createSharedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun clearSharedPreferences(context: Context) {
        try {
            context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun saveAuthState(authState: AuthState?) {
        try {
            val json = authState?.jsonSerializeString()
            sharedPreferences.edit().putString("auth_state", json).apply()
            _authStateFlow.value = authState
        } catch (e: Exception) {
            // If saving fails due to encryption issues, we just don't crash.
        }
    }

    private fun loadAuthState(): AuthState? {
        return try {
            val json = sharedPreferences.getString("auth_state", null) ?: return null
            AuthState.jsonDeserialize(json)
        } catch (e: Exception) {
            // Decryption issues or deserialization issues
            null
        }
    }

    fun getAccessToken(): String? = _authStateFlow.value?.accessToken

    fun getRefreshToken(): String? = _authStateFlow.value?.refreshToken

    fun saveUsername(username: String?) {
        try {
            sharedPreferences.edit().putString("username", username).apply()
        } catch (e: Exception) {
            // Ignore encryption issues during save to prevent crash
        }
    }

    fun getUsername(): String? {
        return try {
            sharedPreferences.getString("username", null)
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        try {
            sharedPreferences.edit().clear().apply()
            _authStateFlow.value = null
        } catch (e: Exception) {
            // Ignore encryption issues during clear
        }
    }
}
