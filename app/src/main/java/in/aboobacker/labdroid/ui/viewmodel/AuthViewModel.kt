package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.UserStatus
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import `in`.aboobacker.labdroid.data.repository.AuthRepository
import `in`.aboobacker.labdroid.data.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationResponse
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val gitLabApi: GitLabApi,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUser = userRepository.currentUser

    init {
        viewModelScope.launch {
            authRepository.getAccessTokenFlow().collect { authState ->
                val isAuthenticated = authState?.isAuthorized == true
                _authState.value =
                    if (isAuthenticated) AuthState.Authenticated else AuthState.Unauthenticated

                if (isAuthenticated && currentUser.value == null) {
                    fetchUserProfile()
                }
            }
        }
    }

    private fun fetchUserProfile() {
        viewModelScope.launch {
            try {
                coroutineScope {
                    val userRestDeferred = async { gitLabApi.getCurrentUser() }
                    val dashboardDataDeferred = async { userRepository.getDashboardData() }

                    val user = userRestDeferred.await()
                    val dashboardData = dashboardDataDeferred.await()

                    val currentUserData = dashboardData?.currentUser
                    val updatedUser = user.copy(
                        hasDuoAccess = currentUserData?.duoClassicChatAvailable
                            ?: user.hasDuoAccess,
                        duoClassicChatAvailable = currentUserData?.duoClassicChatAvailable ?: false,
                        duoChatAvailableFeatures = currentUserData?.duoChatAvailableFeatures
                            ?: emptyList(),
                        duoDefaultNamespaceId = currentUserData?.userPreferences?.duoDefaultNamespace?.id?.substringAfterLast(
                            "/"
                        )?.toLongOrNull(),
                        status = currentUserData?.status?.let { status ->
                            UserStatus(
                                emoji = status.emoji,
                                message = status.message,
                                availability = status.availability?.rawValue,
                                clearStatusAt = status.clearStatusAt?.toString()
                            )
                        }
                    )

                    userRepository.setCurrentUser(updatedUser)
                    authRepository.saveUsername(updatedUser.username)
                }
            } catch (e: Exception) {
                // Log or handle error if needed, but don't break authentication
            }
        }
    }

    fun getAuthorizationRequest() = authRepository.getAuthorizationRequest()

    fun onAuthResponse(response: AuthorizationResponse) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.exchangeCodeForToken(response)
            if (result.isSuccess) {
                fetchUserProfile()
                _authState.value = AuthState.Authenticated
            } else {
                _authState.value =
                    AuthState.Error(result.exceptionOrNull()?.message ?: "Authentication failed")
            }
        }
    }

    fun onAuthError(exception: net.openid.appauth.AuthorizationException) {
        _authState.value =
            AuthState.Error("Authorization failed: ${exception.errorDescription ?: exception.message}")
    }

    fun logout() {
        authRepository.logout()
        userRepository.clear()
        _authState.value = AuthState.Unauthenticated
    }
}

sealed interface AuthState {
    data object Loading : AuthState
    data object Authenticated : AuthState
    data object Unauthenticated : AuthState
    data class Error(val message: String) : AuthState
}
