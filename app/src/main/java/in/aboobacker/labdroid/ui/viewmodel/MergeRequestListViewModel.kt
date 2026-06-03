package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.MergeRequest
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MergeRequestListViewModel @Inject constructor(
    private val api: GitLabApi
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<MergeRequestListUiState>(MergeRequestListUiState.Loading)
    val uiState: StateFlow<MergeRequestListUiState> = _uiState.asStateFlow()

    init {
        fetchMergeRequests(scope = "assigned_to_me")
    }

    private var currentScope = "assigned_to_me"
    private var currentState = "opened"
    private var currentSearch: String? = null
    private var searchJob: Job? = null

    fun fetchMergeRequests(
        scope: String = "assigned_to_me",
        state: String = "opened",
        search: String? = null
    ) {
        currentScope = scope
        currentState = state
        currentSearch = search

        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is MergeRequestListUiState.Success) {
                _uiState.value = currentState.copy(isLoading = true)
            } else {
                _uiState.value = MergeRequestListUiState.Loading
            }

            try {
                coroutineScope {
                    val user = api.getCurrentUser()

                    // GitLab's global merge_requests API timeouts on gitlab.com if scope=all and no filters like author_id or assignee_id are provided.
                    // We ensure either scope is restrictive (assigned_to_me, created_by_me, reviews_for_me) or we explicitly provide the user's ID as assignee.
                    val mrs = if (scope == "all") {
                        api.getMergeRequests(
                            scope = "all",
                            state = state,
                            search = search,
                            assigneeId = user.id // Explicitly mention assignee to avoid global timeout
                        )
                    } else {
                        api.getMergeRequests(
                            scope = scope,
                            state = state,
                            search = search
                        )
                    }

                    _uiState.value = MergeRequestListUiState.Success(
                        user = user,
                        mergeRequests = mrs,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = MergeRequestListUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isNotBlank()) {
                delay(500)
            }
            fetchMergeRequests(
                scope = currentScope,
                state = currentState,
                search = query.ifBlank { null })
        }
    }
}

sealed interface MergeRequestListUiState {
    data object Loading : MergeRequestListUiState
    data class Success(
        val user: User,
        val mergeRequests: List<MergeRequest>,
        val isLoading: Boolean = false
    ) : MergeRequestListUiState

    data class Error(val message: String) : MergeRequestListUiState
}
