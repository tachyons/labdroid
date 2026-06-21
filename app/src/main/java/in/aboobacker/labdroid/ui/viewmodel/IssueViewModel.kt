package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Issue
import `in`.aboobacker.labdroid.data.paging.GitLabPagingSource
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import `in`.aboobacker.labdroid.data.repository.AuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

data class IssueListParams(
    val scope: String = "all",
    val state: String = "opened",
    val search: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class IssueViewModel @Inject constructor(
    private val api: GitLabApi,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val paramsFlow = MutableStateFlow(IssueListParams())

    val issues: Flow<PagingData<Issue>> = paramsFlow.flatMapLatest { params ->
        Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                GitLabPagingSource { page, perPage ->
                    val username = authRepository.getUsername() ?: api.getCurrentUser().username.also {
                        authRepository.saveUsername(it)
                    }

                    api.getIssues(
                        scope = params.scope,
                        state = params.state,
                        search = params.search,
                        assigneeUsername = if (params.scope == "created_by_me") null else username,
                        page = page,
                        perPage = perPage
                    )
                }
            }
        ).flow
    }.cachedIn(viewModelScope)

    fun fetchData(scope: String = "all", state: String = "opened", search: String? = null) {
        paramsFlow.value = IssueListParams(scope, state, search)
    }

    fun search(query: String) {
        paramsFlow.value = paramsFlow.value.copy(search = query.ifBlank { null })
    }
}
