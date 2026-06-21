package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Todo
import `in`.aboobacker.labdroid.data.paging.GitLabPagingSource
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodoViewModel @Inject constructor(
    private val api: GitLabApi
) : ViewModel() {

    private val filterFlow = MutableStateFlow("All")

    val todos: Flow<PagingData<Todo>> = filterFlow.flatMapLatest { filter ->
        val action = when (filter) {
            "Assigned" -> "assigned"
            "Mentions" -> "mentioned"
            "Review Requests" -> "review_requested"
            else -> null
        }
        Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                GitLabPagingSource { page, perPage ->
                    api.getTodos(action = action, page = page, perPage = perPage)
                }
            }
        ).flow
    }.cachedIn(viewModelScope)

    fun fetchTodos(filter: String = "All") {
        filterFlow.value = filter
    }

    fun markAsDone(todoId: Long) {
        viewModelScope.launch {
            try {
                api.markTodoAsDone(todoId)
                // In Paging 3, we usually refresh the entire list or use a local cache with Room to update specific items.
                // For simplicity here, we can just trigger a refresh of the current paging data if needed, 
                // but usually markAsDone is followed by list refresh or manual removal from UI.
                // Since we don't have Room, we might just want to trigger a refresh.
                fetchTodos(filterFlow.value)
            } catch (e: Exception) {
                // Optionally handle error
            }
        }
    }
}
