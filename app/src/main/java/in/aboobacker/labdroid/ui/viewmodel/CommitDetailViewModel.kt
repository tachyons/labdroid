package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Change
import `in`.aboobacker.labdroid.data.model.Commit
import `in`.aboobacker.labdroid.data.model.Discussion
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommitDetailViewModel @Inject constructor(
    private val api: GitLabApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<CommitDetailUiState>(CommitDetailUiState.Loading)
    val uiState: StateFlow<CommitDetailUiState> = _uiState.asStateFlow()

    fun fetchCommitDetails(projectId: Long, sha: String) {
        viewModelScope.launch {
            _uiState.value = CommitDetailUiState.Loading
            try {
                coroutineScope {
                    val commitDeferred = async { api.getCommit(projectId, sha) }
                    val diffDeferred = async { api.getCommitDiff(projectId, sha) }
                    val discussionsDeferred = async { api.getCommitDiscussions(projectId, sha) }

                    _uiState.value = CommitDetailUiState.Success(
                        commit = commitDeferred.await(),
                        diffs = diffDeferred.await(),
                        discussions = discussionsDeferred.await()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = CommitDetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun postComment(projectId: Long, sha: String, body: String, discussionId: String? = null) {
        viewModelScope.launch {
            try {
                if (discussionId != null) {
                    api.postCommitDiscussionNote(projectId, sha, discussionId, body)
                } else {
                    api.postCommitNote(projectId, sha, body)
                }
                fetchCommitDetails(projectId, sha)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

sealed interface CommitDetailUiState {
    object Loading : CommitDetailUiState
    data class Success(
        val commit: Commit,
        val diffs: List<Change>,
        val discussions: List<Discussion> = emptyList()
    ) : CommitDetailUiState

    data class Error(val message: String) : CommitDetailUiState
}
