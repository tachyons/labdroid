package `in`.aboobacker.labdroid.ui.viewmodel

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Branch
import `in`.aboobacker.labdroid.data.model.Commit
import `in`.aboobacker.labdroid.data.model.Issue
import `in`.aboobacker.labdroid.data.model.MergeRequest
import `in`.aboobacker.labdroid.data.model.Project
import `in`.aboobacker.labdroid.data.model.RepositoryItem
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import `in`.aboobacker.labdroid.data.repository.ProjectRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RepositoryViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val api: GitLabApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<RepositoryUiState>(RepositoryUiState.Loading)
    val uiState: StateFlow<RepositoryUiState> = _uiState.asStateFlow()

    private var currentProjectId: Long? = null
    private var currentPageIssues = 1
    private var currentPageMRs = 1
    private var isIssuesEnd = false
    private var isMRsEnd = false

    private var currentIssueScope: String? = "all"
    private var currentIssueState: String = "opened"
    private var currentIssueSearch: String? = null
    private var currentMRScope: String? = "all"
    private var currentMRState: String = "opened"
    private var currentMRSearch: String? = null

    private var issueSearchJob: Job? = null
    private var mrSearchJob: Job? = null

    private var repositoryDataJob: Job? = null

    fun loadRepositoryData(projectId: Long, path: String? = null, ref: String? = null) {
        if (currentProjectId != projectId) {
            // Project changed, reset everything
            currentPageIssues = 1
            currentPageMRs = 1
            isIssuesEnd = false
            isMRsEnd = false
            currentIssueScope = "all"
            currentIssueState = "opened"
            currentIssueSearch = null
            currentMRScope = "all"
            currentMRState = "opened"
            currentMRSearch = null
            _uiState.value = RepositoryUiState.Loading

            // Cancel any running jobs for the previous project
            repositoryDataJob?.cancel()
            issueSearchJob?.cancel()
            mrSearchJob?.cancel()
        }
        currentProjectId = projectId

        repositoryDataJob?.cancel()
        repositoryDataJob = viewModelScope.launch {
            // Only show global loading if we are not already in Success state
            val currentState = _uiState.value as? RepositoryUiState.Success
            if (currentState == null || currentState.project.id != projectId) {
                _uiState.value = RepositoryUiState.Loading
            }

            try {
                coroutineScope {
                    val projectDeferred = async { repository.getProject(projectId) }
                    val itemsDeferred = async { repository.getRepositoryTree(projectId, path, ref) }
                    val lastCommitDeferred = async { repository.getLastCommit(projectId, ref) }

                    // Re-read currentState as it might have been set to Loading above
                    val activeState = _uiState.value as? RepositoryUiState.Success

                    val issuesDeferred = if (activeState == null || activeState.issues.isEmpty()) {
                        async {
                            api.getProjectIssues(
                                projectId,
                                page = 1,
                                scope = currentIssueScope,
                                state = currentIssueState,
                                search = currentIssueSearch
                            )
                        }
                    } else null

                    val mrsDeferred =
                        if (activeState == null || activeState.mergeRequests.isEmpty()) {
                            async {
                                api.getProjectMergeRequests(
                                    projectId,
                                    page = 1,
                                    state = currentMRState,
                                    scope = currentMRScope,
                                    search = currentMRSearch
                                )
                            }
                        } else null

                    val branchesDeferred =
                        if (activeState == null || activeState.branches.isEmpty()) {
                            async { api.getBranches(projectId) }
                        } else null

                    val items = itemsDeferred.await()
                    val readmeItem = items.find { it.name.lowercase() == "readme.md" }

                    val readmeContent = if (readmeItem != null) {
                        try {
                            val file =
                                api.getRepositoryFile(projectId, readmeItem.path, ref ?: "HEAD")
                            String(Base64.decode(file.content, Base64.DEFAULT))
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }

                    val issues = issuesDeferred?.await() ?: currentState?.issues ?: emptyList()
                    val mrs = mrsDeferred?.await() ?: currentState?.mergeRequests ?: emptyList()

                    if (issuesDeferred != null) {
                        currentPageIssues = 1
                        isIssuesEnd = issues.size < 20
                    }
                    if (mrsDeferred != null) {
                        currentPageMRs = 1
                        isMRsEnd = mrs.size < 20
                    }

                    _uiState.value = RepositoryUiState.Success(
                        project = projectDeferred.await(),
                        items = items,
                        lastCommit = lastCommitDeferred.await(),
                        currentPath = path,
                        issues = issues,
                        mergeRequests = mrs,
                        branches = branchesDeferred?.await() ?: currentState?.branches
                        ?: emptyList(),
                        currentRef = ref ?: projectDeferred.await().defaultBranch ?: "main",
                        readmeContent = readmeContent,
                        isIssuesEnd = issues.size < 20,
                        isMRsEnd = isMRsEnd,
                        issueScope = currentIssueScope ?: "all",
                        issueState = currentIssueState,
                        issueSearch = currentIssueSearch ?: "",
                        mrScope = currentMRScope ?: "all",
                        mrState = currentMRState,
                        mrSearch = currentMRSearch ?: ""
                    )
                }
            } catch (e: Exception) {
                _uiState.value = RepositoryUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun searchIssues(query: String) {
        val currentState = _uiState.value as? RepositoryUiState.Success ?: return
        _uiState.value = currentState.copy(issueSearch = query)
        currentIssueSearch = query.ifBlank { null }

        issueSearchJob?.cancel()
        issueSearchJob = viewModelScope.launch {
            delay(500)
            refreshIssues()
        }
    }

    fun filterIssues(scope: String? = null, state: String? = null) {
        val currentState = _uiState.value as? RepositoryUiState.Success ?: return

        scope?.let {
            val newScope = when (it) {
                "Assigned to me" -> "assigned_to_me"
                "Created by me" -> "created_by_me"
                "All Items" -> "all"
                else -> it
            }
            currentIssueScope = newScope
        }

        state?.let {
            currentIssueState = it.lowercase()
        }

        _uiState.value = currentState.copy(
            issueScope = currentIssueScope ?: "all",
            issueState = currentIssueState
        )
        refreshIssues()
    }

    private fun refreshIssues() {
        val projectId = currentProjectId ?: return
        val currentState = _uiState.value as? RepositoryUiState.Success ?: return

        viewModelScope.launch {
            _uiState.value = currentState.copy(issues = emptyList(), isLoadingMoreIssues = true)
            try {
                currentPageIssues = 1
                val issues = api.getProjectIssues(
                    projectId,
                    page = currentPageIssues,
                    scope = currentIssueScope,
                    state = currentIssueState,
                    search = currentIssueSearch
                )
                isIssuesEnd = issues.size < 20
                val updatedState = _uiState.value as? RepositoryUiState.Success ?: currentState
                _uiState.value = updatedState.copy(
                    issues = issues,
                    isLoadingMoreIssues = false,
                    isIssuesEnd = isIssuesEnd
                )
            } catch (e: Exception) {
                val updatedState = _uiState.value as? RepositoryUiState.Success ?: currentState
                _uiState.value = updatedState.copy(isLoadingMoreIssues = false)
            }
        }
    }

    fun searchMRs(query: String) {
        val currentState = _uiState.value as? RepositoryUiState.Success ?: return
        _uiState.value = currentState.copy(mrSearch = query)
        currentMRSearch = query.ifBlank { null }

        mrSearchJob?.cancel()
        mrSearchJob = viewModelScope.launch {
            delay(500)
            refreshMRs()
        }
    }

    fun filterMRs(state: String? = null, scope: String? = null) {
        val currentState = _uiState.value as? RepositoryUiState.Success ?: return

        state?.let {
            val newState = when (it.lowercase()) {
                "open" -> "opened"
                "closed" -> "closed"
                else -> it.lowercase()
            }
            currentMRState = newState
        }

        scope?.let {
            val newScope = when (it) {
                "Assigned to me" -> "assigned_to_me"
                "Created by me" -> "created_by_me"
                "All Items" -> "all"
                else -> it
            }
            currentMRScope = newScope
        }

        _uiState.value = currentState.copy(
            mrState = currentMRState,
            mrScope = currentMRScope ?: "all"
        )
        refreshMRs()
    }

    private fun refreshMRs() {
        val projectId = currentProjectId ?: return
        val currentState = _uiState.value as? RepositoryUiState.Success ?: return

        viewModelScope.launch {
            _uiState.value = currentState.copy(mergeRequests = emptyList(), isLoadingMoreMRs = true)
            try {
                currentPageMRs = 1
                val mrs = api.getProjectMergeRequests(
                    projectId,
                    page = currentPageMRs,
                    state = currentMRState,
                    scope = currentMRScope,
                    search = currentMRSearch
                )
                isMRsEnd = mrs.size < 20
                val updatedState = _uiState.value as? RepositoryUiState.Success ?: currentState
                _uiState.value = updatedState.copy(
                    mergeRequests = mrs,
                    isLoadingMoreMRs = false,
                    isMRsEnd = isMRsEnd
                )
            } catch (e: Exception) {
                val updatedState = _uiState.value as? RepositoryUiState.Success ?: currentState
                _uiState.value = updatedState.copy(isLoadingMoreMRs = false)
            }
        }
    }

    fun loadMoreIssues() {
        val currentState = _uiState.value as? RepositoryUiState.Success ?: return
        if (isIssuesEnd || currentProjectId == null || currentState.isLoadingMoreIssues) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoadingMoreIssues = true)
            try {
                currentPageIssues++
                val moreIssues = api.getProjectIssues(
                    currentProjectId!!,
                    page = currentPageIssues,
                    scope = currentIssueScope,
                    state = currentIssueState,
                    search = currentIssueSearch
                )
                isIssuesEnd = moreIssues.size < 20
                val updatedState = _uiState.value as? RepositoryUiState.Success ?: currentState
                _uiState.value = updatedState.copy(
                    issues = updatedState.issues + moreIssues,
                    isLoadingMoreIssues = false,
                    isIssuesEnd = isIssuesEnd
                )
            } catch (e: Exception) {
                val updatedState = _uiState.value as? RepositoryUiState.Success ?: currentState
                _uiState.value = updatedState.copy(isLoadingMoreIssues = false)
            }
        }
    }

    fun loadMoreMRs() {
        val currentState = _uiState.value as? RepositoryUiState.Success ?: return
        if (isMRsEnd || currentProjectId == null || currentState.isLoadingMoreMRs) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoadingMoreMRs = true)
            try {
                currentPageMRs++
                val moreMRs = api.getProjectMergeRequests(
                    currentProjectId!!,
                    page = currentPageMRs,
                    state = currentMRState,
                    scope = currentMRScope,
                    search = currentMRSearch
                )
                isMRsEnd = moreMRs.size < 20
                val updatedState = _uiState.value as? RepositoryUiState.Success ?: currentState
                _uiState.value = updatedState.copy(
                    mergeRequests = updatedState.mergeRequests + moreMRs,
                    isLoadingMoreMRs = false,
                    isMRsEnd = isMRsEnd
                )
            } catch (e: Exception) {
                val updatedState = _uiState.value as? RepositoryUiState.Success ?: currentState
                _uiState.value = updatedState.copy(isLoadingMoreMRs = false)
            }
        }
    }
}

sealed interface RepositoryUiState {
    object Loading : RepositoryUiState
    data class Success(
        val project: Project,
        val items: List<RepositoryItem>,
        val lastCommit: Commit?,
        val currentPath: String?,
        val issues: List<Issue> = emptyList(),
        val mergeRequests: List<MergeRequest> = emptyList(),
        val branches: List<Branch> = emptyList(),
        val currentRef: String = "main",
        val readmeContent: String? = null,
        val isLoadingMoreIssues: Boolean = false,
        val isLoadingMoreMRs: Boolean = false,
        val isIssuesEnd: Boolean = false,
        val isMRsEnd: Boolean = false,
        val issueScope: String = "all",
        val issueState: String = "opened",
        val issueSearch: String = "",
        val mrScope: String = "all",
        val mrState: String = "opened",
        val mrSearch: String = ""
    ) : RepositoryUiState

    data class Error(val message: String) : RepositoryUiState
}
