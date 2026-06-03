package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val api: GitLabApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var currentUser: User? = null

    init {
        viewModelScope.launch {
            try {
                currentUser = api.getCurrentUser()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun search(query: String, scope: String = "Global") {
        if (query.isEmpty()) {
            _uiState.value = SearchUiState.Idle
            return
        }
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                coroutineScope {
                    val userDeferred =
                        if (currentUser == null) async { api.getCurrentUser() } else null
                    val resultsDeferred = async {
                        val gitlabScope = when (scope.lowercase()) {
                            "users" -> "users"
                            "projects" -> "projects"
                            "issues" -> "issues"
                            "mrs", "merge requests" -> "merge_requests"
                            else -> "projects"
                        }
                        api.search(gitlabScope, query)
                    }

                    val user = userDeferred?.await() ?: currentUser!!
                    currentUser = user
                    val results = resultsDeferred.await()

                    _uiState.value = SearchUiState.Success(user, results)
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val user: User, val results: List<Any>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}
