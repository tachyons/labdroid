package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Group
import `in`.aboobacker.labdroid.data.model.GroupDetail
import `in`.aboobacker.labdroid.data.model.GroupMember
import `in`.aboobacker.labdroid.data.model.Issue
import `in`.aboobacker.labdroid.data.model.MergeRequest
import `in`.aboobacker.labdroid.data.model.Project
import `in`.aboobacker.labdroid.data.paging.GitLabPagingSource
import `in`.aboobacker.labdroid.data.repository.GroupRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val repository: GroupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupDetailUiState>(GroupDetailUiState.Loading)
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    private val groupIdFlow = MutableStateFlow<Long?>(null)

    val subgroups: Flow<PagingData<Group>> = groupIdFlow.flatMapLatest { groupId ->
        Pager(
            config = PagingConfig(pageSize = 10),
            pagingSourceFactory = {
                GitLabPagingSource { page, perPage ->
                    if (groupId != null) {
                        repository.getSubgroups(groupId, page = page)
                    } else {
                        emptyList()
                    }
                }
            }
        ).flow
    }.cachedIn(viewModelScope)

    val members: Flow<PagingData<GroupMember>> = groupIdFlow.flatMapLatest { groupId ->
        Pager(
            config = PagingConfig(pageSize = 10),
            pagingSourceFactory = {
                GitLabPagingSource { page, perPage ->
                    if (groupId != null) {
                        repository.getGroupMembers(groupId, page = page)
                    } else {
                        emptyList()
                    }
                }
            }
        ).flow
    }.cachedIn(viewModelScope)

    fun loadGroupDetail(groupId: Long) {
        if (groupIdFlow.value == groupId) return
        groupIdFlow.value = groupId

        viewModelScope.launch {
            _uiState.value = GroupDetailUiState.Loading
            try {
                val group = repository.getGroup(groupId)
                _uiState.value = GroupDetailUiState.Success(
                    group = group,
                    isLoadingProjects = true,
                    isLoadingIssues = true,
                    isLoadingMergeRequests = true,
                )

                launch { fetchProjects(groupId) }
                launch { fetchIssues(groupId) }
                launch { fetchMergeRequests(groupId) }

            } catch (e: Exception) {
                _uiState.value = GroupDetailUiState.Error(e.message ?: "Unknown error")
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
}

sealed interface GroupDetailUiState {
    object Loading : GroupDetailUiState
    data class Success(
        val group: GroupDetail,
        val projects: List<Project> = emptyList(),
        val issues: List<Issue> = emptyList(),
        val mergeRequests: List<MergeRequest> = emptyList(),
        val isLoadingProjects: Boolean = false,
        val isLoadingIssues: Boolean = false,
        val isLoadingMergeRequests: Boolean = false,
    ) : GroupDetailUiState

    data class Error(val message: String) : GroupDetailUiState
}
