package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Commit
import `in`.aboobacker.labdroid.data.paging.GitLabPagingSource
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CommitListViewModel @Inject constructor(
    private val api: GitLabApi,
) : ViewModel() {

    private val paramsFlow = MutableStateFlow<Pair<Long, String?>?>(null)

    val commits: Flow<PagingData<Commit>> = paramsFlow.flatMapLatest { params ->
        Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                GitLabPagingSource { page, perPage ->
                    if (params != null) {
                        api.getCommits(params.first, params.second, page = page, perPage = perPage)
                    } else {
                        emptyList()
                    }
                }
            }
        ).flow
    }.cachedIn(viewModelScope)

    fun fetchCommits(projectId: Long, ref: String? = null) {
        paramsFlow.value = projectId to ref
    }
}
