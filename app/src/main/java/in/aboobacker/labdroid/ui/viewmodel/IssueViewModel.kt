package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Epic
import `in`.aboobacker.labdroid.data.model.Issue
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import `in`.aboobacker.labdroid.data.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IssueViewModel @Inject constructor(
    private val api: GitLabApi,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<IssueUiState>(IssueUiState.Loading)
    val uiState: StateFlow<IssueUiState> = _uiState.asStateFlow()

    init {
        fetchData(scope = "all")
    }

    private var currentScope = "all"
    private var currentState = "opened"
    private var currentSearch: String? = null
    private var searchJob: Job? = null

    fun fetchData(scope: String = "all", state: String = "opened", search: String? = null) {
        currentScope = scope
        currentState = state
        currentSearch = search

        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is IssueUiState.Success) {
                _uiState.value = currentState.copy(isLoading = true)
            } else {
                _uiState.value = IssueUiState.Loading
            }

            try {
                coroutineScope {
                    val user = api.getCurrentUser()
                    val username = user.username
                    authRepository.saveUsername(username)

                    val issues = api.getIssues(
                        scope = scope,
                        state = state,
                        search = search,
                        assigneeUsername = if (scope == "created_by_me") null else username
                    )

                    _uiState.value = IssueUiState.Success(
                        user = user,
                        issues = issues,
                        epics = emptyList(),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = IssueUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isNotBlank()) {
                delay(500)
            }
            fetchData(scope = currentScope, state = currentState, search = query.ifBlank { null })
        }
    }
}

sealed interface IssueUiState {
    data object Loading : IssueUiState
    data class Success(
        val user: User,
        val issues: List<Issue>,
        val epics: List<Epic> = emptyList(),
        val isLoading: Boolean = false
    ) : IssueUiState

    data class Error(val message: String) : IssueUiState
}
