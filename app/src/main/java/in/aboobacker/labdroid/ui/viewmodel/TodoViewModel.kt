package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Todo
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
class TodoViewModel @Inject constructor(
    private val api: GitLabApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<TodoUiState>(TodoUiState.Loading)
    val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow()

    init {
        fetchTodos()
    }

    fun fetchTodos(filter: String = "All") {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is TodoUiState.Success) {
                _uiState.value = currentState.copy(isRefreshing = true)
            } else {
                _uiState.value = TodoUiState.Loading
            }

            try {
                coroutineScope {
                    val action = when (filter) {
                        "Assigned" -> "assigned"
                        "Mentions" -> "mentioned"
                        "Review Requests" -> "review_requested"
                        else -> null
                    }

                    val userDeferred = async { api.getCurrentUser() }
                    val todosDeferred = async { api.getTodos(action = action) }

                    _uiState.value = TodoUiState.Success(
                        user = userDeferred.await(),
                        todos = todosDeferred.await(),
                        isRefreshing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = TodoUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun markAsDone(todoId: Long) {
        viewModelScope.launch {
            try {
                api.markTodoAsDone(todoId)
                // Refresh todos after marking as done
                val currentState = _uiState.value
                if (currentState is TodoUiState.Success) {
                    _uiState.value = currentState.copy(
                        todos = currentState.todos.filter { it.id != todoId }
                    )
                }
            } catch (e: Exception) {
                // Optionally handle error (e.g., show snackbar)
            }
        }
    }
}

sealed interface TodoUiState {
    object Loading : TodoUiState
    data class Success(
        val user: User,
        val todos: List<Todo>,
        val isRefreshing: Boolean = false
    ) : TodoUiState

    data class Error(val message: String) : TodoUiState
}
