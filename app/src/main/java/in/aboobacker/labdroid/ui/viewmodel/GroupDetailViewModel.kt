package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Group
import `in`.aboobacker.labdroid.data.model.GroupDetail
import `in`.aboobacker.labdroid.data.model.GroupMember
import `in`.aboobacker.labdroid.data.model.Issue
import `in`.aboobacker.labdroid.data.model.MergeRequest
import `in`.aboobacker.labdroid.data.model.Project
import `in`.aboobacker.labdroid.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val repository: GroupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupDetailUiState>(GroupDetailUiState.Loading)
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    private var currentGroupId: Long? = null
    private var subgroupPage = 1
    private var memberPage = 1

    fun loadGroupDetail(groupId: Long) {
        if (currentGroupId == groupId) return
        currentGroupId = groupId
        subgroupPage = 1
        memberPage = 1

        viewModelScope.launch {
            _uiState.value = GroupDetailUiState.Loading
            try {
                val group = repository.getGroup(groupId)
                _uiState.value = GroupDetailUiState.Success(
                    group = group,
                    isLoadingSubgroups = true,
                    isLoadingProjects = true,
                    isLoadingMembers = true,
                    isLoadingIssues = true,
                    isLoadingMergeRequests = true,
                )

                launch { fetchSubgroups(groupId) }
                launch { fetchProjects(groupId) }
                launch { fetchMembers(groupId) }
                launch { fetchIssues(groupId) }
                launch { fetchMergeRequests(groupId) }

            } catch (e: Exception) {
                _uiState.value = GroupDetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun fetchSubgroups(groupId: Long) {
        try {
            val subgroups = repository.getSubgroups(groupId, subgroupPage)
            _uiState.update { state ->
                (state as? GroupDetailUiState.Success)?.copy(
                    subgroups = subgroups,
                    isLoadingSubgroups = false,
                    hasMoreSubgroups = subgroups.size >= 10,
                ) ?: state
            }
        } catch (_: Exception) {
            _uiState.update { state ->
                (state as? GroupDetailUiState.Success)?.copy(isLoadingSubgroups = false) ?: state
            }
        }
    }

    private suspend fun fetchProjects(groupId: Long) {
        try {
            val projects = repository.getGroupProjects(groupId)
            _uiState.update { state ->
                (state as? GroupDetailUiState.Success)?.copy(
                    projects = projects,
                    isLoadingProjects = false,
                ) ?: state
            }
        } catch (_: Exception) {
            _uiState.update { state ->
                (state as? GroupDetailUiState.Success)?.copy(isLoadingProjects = false) ?: state
            }
        }
    }

    private suspend fun fetchMembers(groupId: Long) {
        try {
            val members = repository.getGroupMembers(groupId, memberPage)
            _uiState.update { state ->
                (state as? GroupDetailUiState.Success)?.copy(
                    members = members,
                    isLoadingMembers = false,
                    hasMoreMembers = members.size >= 10,
                ) ?: state
            }
        } catch (_: Exception) {
            _uiState.update { state ->
                (state as? GroupDetailUiState.Success)?.copy(isLoadingMembers = false) ?: state
            }
        }
    }

    private suspend fun fetchIssues(groupId: Long) {
        try {
            val issues = repository.getGroupIssues(groupId)
            _uiState.update { state ->
                (state as? GroupDetailUiState.Success)?.copy(
                    issues = issues,
                    isLoadingIssues = false,
                ) ?: state
            }
        } catch (_: Exception) {
            _uiState.update { state ->
                (state as? GroupDetailUiState.Success)?.copy(isLoadingIssues = false) ?: state
            }
        }
    }

    private suspend fun fetchMergeRequests(groupId: Long) {
        try {
            val mergeRequests = repository.getGroupMergeRequests(groupId)
            _uiState.update { state ->
                (state as? GroupDetailUiState.Success)?.copy(
                    mergeRequests = mergeRequests,
                    isLoadingMergeRequests = false,
                ) ?: state
            }
        } catch (_: Exception) {
            _uiState.update { state ->
                (state as? GroupDetailUiState.Success)?.copy(isLoadingMergeRequests = false)
                    ?: state
            }
        }
    }

    fun loadMoreSubgroups() {
        val currentState = _uiState.value
        if ((currentState !is GroupDetailUiState.Success) || !currentState.hasMoreSubgroups) return
        val groupId = currentGroupId ?: return

        viewModelScope.launch {
            try {
                subgroupPage++
                val newSubgroups = repository.getSubgroups(groupId, subgroupPage)
                _uiState.update { state ->
                    (state as? GroupDetailUiState.Success)?.copy(
                        subgroups = state.subgroups + newSubgroups,
                        hasMoreSubgroups = newSubgroups.size >= 10,
                    ) ?: state
                }
            } catch (_: Exception) {
                // Handle error
            }
        }
    }

    fun loadMoreMembers() {
        val currentState = _uiState.value
        if ((currentState !is GroupDetailUiState.Success) || !currentState.hasMoreMembers) return
        val groupId = currentGroupId ?: return

        viewModelScope.launch {
            try {
                memberPage++
                val newMembers = repository.getGroupMembers(groupId, memberPage)
                _uiState.update { state ->
                    (state as? GroupDetailUiState.Success)?.copy(
                        members = state.members + newMembers,
                        hasMoreMembers = newMembers.size >= 10,
                    ) ?: state
                }
            } catch (_: Exception) {
                // Handle error
            }
        }
    }
}

sealed interface GroupDetailUiState {
    object Loading : GroupDetailUiState
    data class Success(
        val group: GroupDetail,
        val subgroups: List<Group> = emptyList(),
        val projects: List<Project> = emptyList(),
        val members: List<GroupMember> = emptyList(),
        val issues: List<Issue> = emptyList(),
        val mergeRequests: List<MergeRequest> = emptyList(),
        val isLoadingSubgroups: Boolean = false,
        val isLoadingProjects: Boolean = false,
        val isLoadingMembers: Boolean = false,
        val isLoadingIssues: Boolean = false,
        val isLoadingMergeRequests: Boolean = false,
        val hasMoreSubgroups: Boolean = false,
        val hasMoreMembers: Boolean = false,
    ) : GroupDetailUiState

    data class Error(val message: String) : GroupDetailUiState
}
