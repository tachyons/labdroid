package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Group
import `in`.aboobacker.labdroid.data.paging.GitLabPagingSource
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

data class GroupListParams(
    val search: String? = null,
    val ownedOnly: Boolean? = null,
    val allAvailable: Boolean? = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GroupViewModel @Inject constructor(
    private val api: GitLabApi
) : ViewModel() {

    private val paramsFlow = MutableStateFlow(GroupListParams())

    val groups: Flow<PagingData<Group>> = paramsFlow.flatMapLatest { params ->
        Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                GitLabPagingSource { page, perPage ->
                    api.getGroups(
                        search = params.search,
                        owned = params.ownedOnly,
                        allAvailable = params.allAvailable,
                        page = page,
                        perPage = perPage
                    )
                }
            }
        ).flow
    }.cachedIn(viewModelScope)

    fun fetchGroups(
        search: String? = null,
        ownedOnly: Boolean? = null,
        allAvailable: Boolean? = false
    ) {
        paramsFlow.value = GroupListParams(search, ownedOnly, allAvailable)
    }
}
