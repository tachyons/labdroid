package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.MergeRequest
import `in`.aboobacker.labdroid.data.paging.GitLabPagingSource
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

data class MergeRequestListParams(
    val scope: String = "assigned_to_me",
    val state: String = "opened",
    val search: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MergeRequestListViewModel @Inject constructor(
    private val api: GitLabApi
) : ViewModel() {

    private val paramsFlow = MutableStateFlow(MergeRequestListParams())

    val mergeRequests: Flow<PagingData<MergeRequest>> = paramsFlow.flatMapLatest { params ->
        Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                GitLabPagingSource { page, perPage ->
                    // For "all" scope, we might still want to restrict to avoid timeouts on gitlab.com,
                    // but since we're using Paging 3, we'll let the user specify.
                    // The previous implementation added current user's ID as assignee if scope was "all".
                    // We can do similar if needed.
                    api.getMergeRequests(
                        scope = params.scope,
                        state = params.state,
                        search = params.search,
                        page = page,
                        perPage = perPage
                    )
                }
            }
        ).flow
    }.cachedIn(viewModelScope)

    fun fetchMergeRequests(
        scope: String = "assigned_to_me",
        state: String = "opened",
        search: String? = null
    ) {
        paramsFlow.value = MergeRequestListParams(scope, state, search)
    }

    fun search(query: String) {
        paramsFlow.value = paramsFlow.value.copy(search = query.ifBlank { null })
    }
}
