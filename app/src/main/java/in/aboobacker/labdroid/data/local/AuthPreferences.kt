package `in`.aboobacker.labdroid.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.openid.appauth.AuthState
import org.json.JSONException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _authStateFlow = MutableStateFlow<AuthState?>(loadAuthState())
    val authStateFlow = _authStateFlow.asStateFlow()

    fun saveAuthState(authState: AuthState?) {
        val json = authState?.jsonSerializeString()
        sharedPreferences.edit().putString("auth_state", json).apply()
        _authStateFlow.value = authState
    }

    private fun loadAuthState(): AuthState? {
        val json = sharedPreferences.getString("auth_state", null) ?: return null
        return try {
            AuthState.jsonDeserialize(json)
        } catch (e: JSONException) {
            null
        }
    }

    fun getAccessToken(): String? = _authStateFlow.value?.accessToken

    fun getRefreshToken(): String? = _authStateFlow.value?.refreshToken

    fun saveUsername(username: String?) {
        sharedPreferences.edit().putString("username", username).apply()
    }

    fun getUsername(): String? {
        return sharedPreferences.getString("username", null)
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
        _authStateFlow.value = null
    }
}
