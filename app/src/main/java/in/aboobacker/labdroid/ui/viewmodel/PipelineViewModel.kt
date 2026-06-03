package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Pipeline
import `in`.aboobacker.labdroid.data.model.PipelineJob
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PipelineViewModel @Inject constructor(
    private val api: GitLabApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PipelineUiState>(PipelineUiState.Loading)
    val uiState: StateFlow<PipelineUiState> = _uiState.asStateFlow()

    fun fetchPipeline(projectId: Long, pipelineId: Long) {
        viewModelScope.launch {
            _uiState.value = PipelineUiState.Loading
            try {
                coroutineScope {
                    val pipelineDeferred = async { api.getPipeline(projectId, pipelineId) }
                    val jobsDeferred = async { api.getPipelineJobs(projectId, pipelineId) }

                    _uiState.value = PipelineUiState.Success(
                        pipeline = pipelineDeferred.await(),
                        jobs = jobsDeferred.await()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = PipelineUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface PipelineUiState {
    object Loading : PipelineUiState
    data class Success(
        val pipeline: Pipeline,
        val jobs: List<PipelineJob>
    ) : PipelineUiState

    data class Error(val message: String) : PipelineUiState
}
