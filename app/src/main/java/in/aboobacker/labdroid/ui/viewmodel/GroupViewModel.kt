package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Group
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val repository: `in`.aboobacker.labdroid.data.repository.GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupUiState>(GroupUiState.Loading)
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    init {
        fetchGroups()
    }

    fun fetchGroups(
        search: String? = null,
        ownedOnly: Boolean? = null,
        allAvailable: Boolean? = false
    ) {
        viewModelScope.launch {
            _uiState.value = GroupUiState.Loading
            try {
                val groups = repository.getGroups(search, ownedOnly, allAvailable)
                _uiState.value = GroupUiState.Success(groups)
            } catch (e: Exception) {
                _uiState.value = GroupUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface GroupUiState {
    object Loading : GroupUiState
    data class Success(val groups: List<Group>) : GroupUiState
    data class Error(val message: String) : GroupUiState
}
