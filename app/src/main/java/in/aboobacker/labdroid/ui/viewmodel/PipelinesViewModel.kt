package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Pipeline
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PipelinesViewModel @Inject constructor(
    private val api: GitLabApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<PipelinesUiState>(PipelinesUiState.Loading)
    val uiState: StateFlow<PipelinesUiState> = _uiState.asStateFlow()

    private var currentProjectId: Long? = null
    private var currentPage = 1
    private var isEnd = false

    fun fetchPipelines(projectId: Long) {
        currentProjectId = projectId
        currentPage = 1
        isEnd = false
        viewModelScope.launch {
            _uiState.value = PipelinesUiState.Loading
            try {
                val pipelines = api.getProjectPipelines(projectId, perPage = 20, page = currentPage)
                isEnd = pipelines.size < 20
                _uiState.value = PipelinesUiState.Success(pipelines)
            } catch (e: Exception) {
                _uiState.value = PipelinesUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value as? PipelinesUiState.Success ?: return
        if (isEnd || currentProjectId == null || currentState.isLoadingMore) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoadingMore = true)
            try {
                currentPage++
                val morePipelines =
                    api.getProjectPipelines(currentProjectId!!, perPage = 20, page = currentPage)
                isEnd = morePipelines.size < 20
                val updatedState = _uiState.value as? PipelinesUiState.Success ?: currentState
                _uiState.value = updatedState.copy(
                    pipelines = updatedState.pipelines + morePipelines,
                    isLoadingMore = false
                )
            } catch (e: Exception) {
                val updatedState = _uiState.value as? PipelinesUiState.Success ?: currentState
                _uiState.value = updatedState.copy(isLoadingMore = false)
            }
        }
    }
}

sealed interface PipelinesUiState {
    object Loading : PipelinesUiState
    data class Success(
        val pipelines: List<Pipeline>,
        val isLoadingMore: Boolean = false
    ) : PipelinesUiState

    data class Error(val message: String) : PipelinesUiState
}
