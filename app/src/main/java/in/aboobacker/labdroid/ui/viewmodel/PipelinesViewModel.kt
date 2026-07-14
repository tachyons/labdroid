package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Pipeline
import `in`.aboobacker.labdroid.data.paging.GitLabPagingSource
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PipelinesViewModel @Inject constructor(
    private val api: GitLabApi
) : ViewModel() {

    private val projectIdFlow = MutableStateFlow<Long?>(null)

    val pipelines: Flow<PagingData<Pipeline>> = projectIdFlow.flatMapLatest { projectId ->
        Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                GitLabPagingSource { page, perPage ->
                    if (projectId != null) {
                        api.getProjectPipelines(projectId, perPage = perPage, page = page)
                    } else {
                        emptyList()
                    }
                }
            }
        ).flow
    }.cachedIn(viewModelScope)

    fun fetchPipelines(projectId: Long) {
        projectIdFlow.value = projectId
    }
}
