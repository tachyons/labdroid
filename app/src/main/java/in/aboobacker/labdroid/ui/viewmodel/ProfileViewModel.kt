package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Event
import `in`.aboobacker.labdroid.data.model.Project
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.data.model.UserUpdate
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import `in`.aboobacker.labdroid.data.repository.ProjectRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val api: GitLabApi,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _updateEvent = MutableSharedFlow<UpdateResult>()
    val updateEvent: SharedFlow<UpdateResult> = _updateEvent.asSharedFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    init {
        // Only fetch current profile by default if we're not specifically loading another user
        // This is mainly for ProfileScreen which uses this ViewModel without parameters
    }

    fun fetchProfileData(userId: Long? = null, username: String? = null) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val (user, pinnedProjects, todoCount) = coroutineScope {
                    if (userId == null && username == null) {
                        val dashboardDataDeferred = async { projectRepository.getDashboardData() }
                        val currentUserRestDeferred = async { api.getCurrentUser() }

                        val dashboardData = dashboardDataDeferred.await()
                        val user = currentUserRestDeferred.await()

                        val currentUserData = dashboardData?.currentUser
                        val updatedUser = user.copy(
                            hasDuoAccess = currentUserData?.duoClassicChatAvailable
                                ?: user.hasDuoAccess,
                            duoClassicChatAvailable = currentUserData?.duoClassicChatAvailable
                                ?: false,
                            duoChatAvailableFeatures = currentUserData?.duoChatAvailableFeatures
                                ?: emptyList()
                        )

                        val projects =
                            currentUserData?.starredProjects?.nodes?.mapNotNull { node ->
                                node?.let {
                                    Project(
                                        id = it.id.substringAfterLast("/").toLong(),
                                        name = it.name,
                                        nameWithNamespace = it.nameWithNamespace,
                                        pathWithNamespace = it.fullPath,
                                        description = it.description,
                                        avatarUrl = it.avatarUrl,
                                        starCount = it.starCount,
                                        forksCount = it.forksCount,
                                        lastActivityAt = it.lastActivityAt?.toString(),
                                        visibility = it.visibility,
                                        webUrl = it.webUrl
                                    )
                                }
                            } ?: emptyList()
                        val count = currentUserData?.todos?.count ?: 0
                        Triple(updatedUser, projects, count)
                    } else if (userId != null) {
                        val userDeferred = async { api.getUser(userId) }
                        val projectsDeferred = async { api.getUserProjects(userId) }

                        val user = userDeferred.await()
                        val projects = projectsDeferred.await().take(5)
                        Triple(user, projects, 0)
                    } else {
                        // username != null
                        val users = api.searchUsers(username!!)
                        val user = users.find { it.username.equals(username, ignoreCase = true) }
                            ?: users.firstOrNull()
                            ?: throw Exception("User not found")

                        val projectsDeferred = async { api.getUserProjects(user.id) }
                        val projects = projectsDeferred.await().take(5)
                        Triple(user, projects, 0)
                    }
                }

                _uiState.value = ProfileUiState.Success(
                    user = user,
                    pinnedProjects = pinnedProjects,
                    todoCount = todoCount
                )

                // Fetch events asynchronously
                launch {
                    try {
                        val events = api.getUserEvents(user.id)
                        val currentState = _uiState.value
                        if (currentState is ProfileUiState.Success) {
                            _uiState.value = currentState.copy(
                                events = events,
                                isLoadingEvents = false
                            )
                        }
                    } catch (e: Exception) {
                        val currentState = _uiState.value
                        if (currentState is ProfileUiState.Success) {
                            _uiState.value = currentState.copy(isLoadingEvents = false)
                        }
                    }
                }

                // Fetch calendar asynchronously to prevent it from hampering page load
                launch {
                    try {
                        val calendar =
                            api.getContributionCalendar("https://gitlab.com/users/${user.username}/calendar.json")
                        val currentState = _uiState.value
                        if (currentState is ProfileUiState.Success) {
                            _uiState.value = currentState.copy(
                                contributionCalendar = calendar,
                                isLoadingCalendar = false
                            )
                        }
                    } catch (e: Exception) {
                        val currentState = _uiState.value
                        if (currentState is ProfileUiState.Success) {
                            _uiState.value = currentState.copy(isLoadingCalendar = false)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateProfile(userUpdate: UserUpdate) {
        viewModelScope.launch {
            _isUpdating.value = true
            try {
                api.updateCurrentUser(userUpdate)
                fetchProfileData()
                _updateEvent.emit(UpdateResult.Success)
            } catch (e: Exception) {
                _updateEvent.emit(UpdateResult.Error(e.message ?: "Failed to update profile"))
            } finally {
                _isUpdating.value = false
            }
        }
    }

    fun followUser(userId: Long) {
        viewModelScope.launch {
            try {
                api.followUser(userId)
                updateFollowState(userId, true)
            } catch (e: Exception) {
                _updateEvent.emit(UpdateResult.Error(e.message ?: "Failed to follow user"))
            }
        }
    }

    fun unfollowUser(userId: Long) {
        viewModelScope.launch {
            try {
                api.unfollowUser(userId)
                updateFollowState(userId, false)
            } catch (e: Exception) {
                _updateEvent.emit(UpdateResult.Error(e.message ?: "Failed to unfollow user"))
            }
        }
    }

    private fun updateFollowState(userId: Long, isFollowed: Boolean) {
        val currentState = _uiState.value
        if (currentState is ProfileUiState.Success && currentState.user.id == userId) {
            _uiState.value = currentState.copy(
                user = currentState.user.copy(isFollowed = isFollowed)
            )
        }
    }
}

sealed interface UpdateResult {
    object Success : UpdateResult
    data class Error(val message: String) : UpdateResult
}

sealed interface ProfileUiState {
    object Loading : ProfileUiState
    data class Success(
        val user: User,
        val pinnedProjects: List<Project>,
        val contributionCalendar: Map<String, Int> = emptyMap(),
        val events: List<Event> = emptyList(),
        val todoCount: Int = 0,
        val isLoadingEvents: Boolean = true,
        val isLoadingCalendar: Boolean = true
    ) : ProfileUiState

    data class Error(val message: String) : ProfileUiState
}
