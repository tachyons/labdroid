package `in`.aboobacker.labdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.data.model.Permissions
import `in`.aboobacker.labdroid.data.model.Pipeline
import `in`.aboobacker.labdroid.data.model.PipelineJob
import `in`.aboobacker.labdroid.data.model.Project
import `in`.aboobacker.labdroid.data.model.ProjectAccess
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.data.model.UserPermissions
import `in`.aboobacker.labdroid.data.repository.ProjectRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val repository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProjectUiState>(ProjectUiState.Loading)
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    init {
        fetchProjects()
        observeRecentProjects()
    }

    private fun observeRecentProjects() {
        viewModelScope.launch {
            repository.recentProjects.collectLatest { recent ->
                val currentState = _uiState.value
                if (currentState is ProjectUiState.Success) {
                    _uiState.value = currentState.copy(recentProjects = recent)
                }
            }
        }
    }

    fun fetchProjects() {
        viewModelScope.launch {
            _uiState.value = ProjectUiState.Loading
            try {
                coroutineScope {
                    val dashboardDataDeferred = async { repository.getDashboardData() }

                    val dashboardData = dashboardDataDeferred.await()
                    val currentUser = dashboardData?.currentUser
                    val user = User(
                        id = currentUser?.id?.toString()?.substringAfterLast("/")?.toLong() ?: 0L,
                        name = currentUser?.name ?: "User",
                        username = currentUser?.username ?: "user",
                        avatarUrl = currentUser?.avatarUrl
                    )

                    val starred = currentUser?.starredProjects?.nodes?.mapNotNull { node ->
                        node?.let {
                            Project(
                                id = it.id.substringAfterLast("/").toLong(),
                                name = it.name,
                                nameWithNamespace = it.nameWithNamespace,
                                pathWithNamespace = it.fullPath,
                                description = it.description,
                                avatarUrl = it.avatarUrl,
                                starCount = it.starCount,
                                forksCount = it.forksCount,
                                lastActivityAt = it.lastActivityAt?.toString(),
                                visibility = it.visibility,
                                webUrl = it.webUrl,
                                permissions = Permissions(
                                    maxAccessLevel = it.maxAccessLevel?.integerValue,
                                    userPermissions = it.userPermissions.let { p ->
                                        UserPermissions(
                                            pushCode = p.pushCode,
                                            downloadCode = p.downloadCode,
                                            adminProject = p.adminProject,
                                            adminIssue = p.adminIssue,
                                            createIssue = p.createIssue,
                                            createMergeRequestIn = p.createMergeRequestIn
                                        )
                                    }
                                )
                            )
                        }
                    } ?: emptyList()

                    val personal = currentUser?.projectMemberships?.nodes?.mapNotNull { node ->
                        node?.project?.let {
                            Project(
                                id = it.id.substringAfterLast("/").toLong(),
                                name = it.name,
                                nameWithNamespace = it.nameWithNamespace,
                                pathWithNamespace = it.fullPath,
                                description = it.description,
                                avatarUrl = it.avatarUrl,
                                starCount = it.starCount,
                                forksCount = it.forksCount,
                                lastActivityAt = it.lastActivityAt?.toString(),
                                visibility = it.visibility,
                                webUrl = it.webUrl,
                                permissions = Permissions(
                                    projectAccess = ProjectAccess(
                                        accessLevel = node.accessLevel?.integerValue ?: 0
                                    ),
                                    maxAccessLevel = it.maxAccessLevel?.integerValue,
                                    userPermissions = it.userPermissions.let { p ->
                                        UserPermissions(
                                            pushCode = p.pushCode,
                                            downloadCode = p.downloadCode,
                                            adminProject = p.adminProject,
                                            adminIssue = p.adminIssue,
                                            createIssue = p.createIssue,
                                            createMergeRequestIn = p.createMergeRequestIn
                                        )
                                    }
                                )
                            )
                        }
                    } ?: emptyList()

                    _uiState.value = ProjectUiState.Success(
                        user = user,
                        projects = emptyList(), // Not used in dashboard
                        starredProjects = starred,
                        personalProjects = personal,
                        recentProjects = emptyList() // Updated by observer
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProjectUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun saveRecentProject(project: Project) {
        viewModelScope.launch {
            repository.saveRecentProject(project)
        }
    }
}

sealed interface ProjectUiState {
    data object Loading : ProjectUiState
    data class Success(
        val user: User,
        val projects: List<Project>,
        val starredProjects: List<Project>,
        val personalProjects: List<Project> = emptyList(),
        val recentProjects: List<Project> = emptyList(),
        val activePipeline: Pipeline? = null,
        val activePipelineProject: Project? = null,
        val pipelineJobs: List<PipelineJob> = emptyList()
    ) : ProjectUiState

    data class Error(val message: String) : ProjectUiState
}
