package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Discussion
import `in`.aboobacker.labdroid.data.model.Epic
import `in`.aboobacker.labdroid.data.model.Issue
import `in`.aboobacker.labdroid.data.repository.EpicRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EpicDetailViewModel @Inject constructor(
    private val repository: EpicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EpicDetailUiState>(EpicDetailUiState.Loading)
    val uiState: StateFlow<EpicDetailUiState> = _uiState.asStateFlow()

    fun loadEpicDetail(groupId: Long, epicIid: Long) {
        viewModelScope.launch {
            _uiState.value = EpicDetailUiState.Loading
            try {
                coroutineScope {
                    val epicDeferred = async { repository.getEpic(groupId, epicIid) }
                    val issuesDeferred = async { repository.getEpicIssues(groupId, epicIid) }
                    val discussionsDeferred =
                        async { repository.getEpicDiscussions(groupId, epicIid) }

                    _uiState.value = EpicDetailUiState.Success(
                        epicDeferred.await(),
                        issuesDeferred.await(),
                        discussionsDeferred.await()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = EpicDetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface EpicDetailUiState {
    object Loading : EpicDetailUiState
    data class Success(
        val epic: Epic,
        val issues: List<Issue>,
        val discussions: List<Discussion> = emptyList()
    ) : EpicDetailUiState

    data class Error(val message: String) : EpicDetailUiState
}
