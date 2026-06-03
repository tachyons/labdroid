package `in`.aboobacker.labdroid.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Event
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import `in`.aboobacker.labdroid.data.repository.ProjectRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val api: GitLabApi,
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        fetchDashboardData()
    }

    fun fetchDashboardData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                Log.d("HomeViewModel", "Fetching dashboard data...")
                val data = try {
                    projectRepository.getDashboardData()
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Exception in getDashboardData", e)
                    throw e
                }
                Log.d("HomeViewModel", "Dashboard data received: $data")
                val currentUser = data?.currentUser

                if (currentUser == null) {
                    Log.e("HomeViewModel", "Current user is null in dashboard data")
                    _uiState.value = HomeUiState.Error("User data not found")
                    return@launch
                }

                val userIdString = currentUser.id.toString()
                val userId = userIdString.substringAfterLast("/").toLong()

                coroutineScope {
                    val eventsDeferred = async {
                        try {
                            api.getUserEvents(userId)
                        } catch (e: Exception) {
                            Log.e("HomeViewModel", "Error fetching events", e)
                            emptyList()
                        }
                    }

                    val events = eventsDeferred.await()

                    _uiState.value = HomeUiState.Success(
                        user = User(
                            id = userId,
                            name = currentUser.name,
                            username = currentUser.username,
                            avatarUrl = if (currentUser.avatarUrl?.startsWith("/") == true) {
                                "https://gitlab.com${currentUser.avatarUrl}"
                            } else {
                                currentUser.avatarUrl
                            }
                        ),
                        events = events,
                        pendingReviews = currentUser.todos?.count ?: 0
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val user: User,
        val events: List<Event>,
        val pendingReviews: Int
    ) : HomeUiState

    data class Error(val message: String) : HomeUiState
}
