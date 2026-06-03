package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Commit
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommitListViewModel @Inject constructor(
    private val api: GitLabApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CommitListUiState>(CommitListUiState.Loading)
    val uiState: StateFlow<CommitListUiState> = _uiState.asStateFlow()

    fun fetchCommits(projectId: Long, ref: String? = null) {
        viewModelScope.launch {
            _uiState.value = CommitListUiState.Loading
            try {
                val commits = api.getCommits(projectId, ref)
                _uiState.value = CommitListUiState.Success(commits)
            } catch (e: Exception) {
                _uiState.value = CommitListUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface CommitListUiState {
    object Loading : CommitListUiState
    data class Success(val commits: List<Commit>) : CommitListUiState
    data class Error(val message: String) : CommitListUiState
}
