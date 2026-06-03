package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Discussion
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscussionsViewModel @Inject constructor(
    private val api: GitLabApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiscussionsUiState>(DiscussionsUiState.Loading)
    val uiState: StateFlow<DiscussionsUiState> = _uiState.asStateFlow()

    fun fetchIssueDiscussions(projectId: Long, issueIid: Long) {
        viewModelScope.launch {
            _uiState.value = DiscussionsUiState.Loading
            try {
                val discussions = api.getIssueDiscussions(projectId, issueIid)
                _uiState.value = DiscussionsUiState.Success(discussions)
            } catch (e: Exception) {
                _uiState.value = DiscussionsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun fetchMergeRequestDiscussions(projectId: Long, mrIid: Long) {
        viewModelScope.launch {
            _uiState.value = DiscussionsUiState.Loading
            try {
                val discussions = api.getMergeRequestDiscussions(projectId, mrIid)
                _uiState.value = DiscussionsUiState.Success(discussions)
            } catch (e: Exception) {
                _uiState.value = DiscussionsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun fetchCommitDiscussions(projectId: Long, commitSha: String) {
        viewModelScope.launch {
            _uiState.value = DiscussionsUiState.Loading
            try {
                val discussions = api.getCommitDiscussions(projectId, commitSha)
                _uiState.value = DiscussionsUiState.Success(discussions)
            } catch (e: Exception) {
                _uiState.value = DiscussionsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface DiscussionsUiState {
    object Loading : DiscussionsUiState
    data class Success(val discussions: List<Discussion>) : DiscussionsUiState
    data class Error(val message: String) : DiscussionsUiState
}
