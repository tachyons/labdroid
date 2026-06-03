package `in`.aboobacker.labdroid.ui.viewmodel

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.RepositoryFile
import `in`.aboobacker.labdroid.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FileDetailViewModel @Inject constructor(
    private val repository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FileDetailUiState>(FileDetailUiState.Loading)
    val uiState: StateFlow<FileDetailUiState> = _uiState.asStateFlow()

    fun loadFile(projectId: Long, filePath: String, ref: String = "HEAD") {
        viewModelScope.launch {
            _uiState.value = FileDetailUiState.Loading
            try {
                val file = repository.getRepositoryFile(projectId, filePath, ref)
                val decodedContent = if (file.encoding == "base64") {
                    String(Base64.decode(file.content, Base64.DEFAULT))
                } else {
                    file.content
                }
                _uiState.value = FileDetailUiState.Success(file, decodedContent)
            } catch (e: Exception) {
                _uiState.value = FileDetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface FileDetailUiState {
    object Loading : FileDetailUiState
    data class Success(val file: RepositoryFile, val content: String) : FileDetailUiState
    data class Error(val message: String) : FileDetailUiState
}
