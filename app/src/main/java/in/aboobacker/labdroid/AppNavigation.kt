package `in`.aboobacker.labdroid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import `in`.aboobacker.labdroid.ui.screens.CommitDetailScreen
import `in`.aboobacker.labdroid.ui.screens.CommitListScreen
import `in`.aboobacker.labdroid.ui.screens.DuoChatScreen
import `in`.aboobacker.labdroid.ui.screens.EditProfileScreen
import `in`.aboobacker.labdroid.ui.screens.EpicDetailScreen
import `in`.aboobacker.labdroid.ui.screens.FileDetailScreen
import `in`.aboobacker.labdroid.ui.screens.GroupDetailScreen
import `in`.aboobacker.labdroid.ui.screens.IssueDetailScreen
import `in`.aboobacker.labdroid.ui.screens.IssueEditScreen
import `in`.aboobacker.labdroid.ui.screens.IssuesScreen
import `in`.aboobacker.labdroid.ui.screens.MergeRequestEditScreen
import `in`.aboobacker.labdroid.ui.screens.MergeRequestListScreen
import `in`.aboobacker.labdroid.ui.screens.MergeRequestScreen
import `in`.aboobacker.labdroid.ui.screens.PipelineScreen
import `in`.aboobacker.labdroid.ui.screens.PipelinesScreen
import `in`.aboobacker.labdroid.ui.screens.ProfileScreen
import `in`.aboobacker.labdroid.ui.screens.ProjectListScreen
import `in`.aboobacker.labdroid.ui.screens.RepositoryScreen
import `in`.aboobacker.labdroid.ui.screens.SearchScreen
import `in`.aboobacker.labdroid.ui.screens.SettingsScreen
import `in`.aboobacker.labdroid.ui.screens.TodosScreen
import `in`.aboobacker.labdroid.ui.screens.UserProfileScreen
import `in`.aboobacker.labdroid.ui.viewmodel.AuthViewModel
import `in`.aboobacker.labdroid.ui.viewmodel.CommitDetailViewModel
import `in`.aboobacker.labdroid.ui.viewmodel.CommitListViewModel
import `in`.aboobacker.labdroid.ui.viewmodel.EpicDetailViewModel
import `in`.aboobacker.labdroid.ui.viewmodel.FileDetailViewModel
import `in`.aboobacker.labdroid.ui.viewmodel.GroupDetailViewModel
import `in`.aboobacker.labdroid.ui.viewmodel.IssueDetailUiState
import `in`.aboobacker.labdroid.ui.viewmodel.IssueDetailViewModel
import `in`.aboobacker.labdroid.ui.viewmodel.IssueViewModel
import `in`.aboobacker.labdroid.ui.viewmodel.MergeRequestListViewModel
import `in`.aboobacker.labdroid.ui.viewmodel.MergeRequestUiState
import `in`.aboobacker.labdroid.ui.viewmodel.MergeRequestViewModel
import `in`.aboobacker.labdroid.ui.viewmodel.PipelineViewModel
import `in`.aboobacker.labdroid.ui.viewmodel.PipelinesViewModel
import `in`.aboobacker.labdroid.ui.viewmodel.ProfileUiState
import `in`.aboobacker.labdroid.ui.viewmodel.ProfileViewModel
import `in`.aboobacker.labdroid.ui.viewmodel.ProjectViewModel
import `in`.aboobacker.labdroid.ui.viewmodel.RepositoryUiState
import `in`.aboobacker.labdroid.ui.viewmodel.RepositoryViewModel
import `in`.aboobacker.labdroid.ui.viewmodel.SearchViewModel
import `in`.aboobacker.labdroid.ui.viewmodel.SettingsViewModel
import `in`.aboobacker.labdroid.ui.viewmodel.TodoViewModel

@Composable
fun createAppEntryProvider(
    navigator: Navigator,
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel
): (androidx.navigation3.runtime.NavKey) -> androidx.navigation3.runtime.NavEntry<androidx.navigation3.runtime.NavKey> {
    val currentUser by authViewModel.currentUser.collectAsState()

    return entryProvider {
        entry<Screen.CommitList> { key ->
            val commitListViewModel: CommitListViewModel = hiltViewModel()
            CommitListScreen(
                viewModel = commitListViewModel,
                projectId = key.projectId,
                ref = key.ref,
                onCommitClick = { sha ->
                    navigator.navigate(Screen.CommitDetail(key.projectId, sha))
                }
            )
        }
        entry<Screen.CommitDetail> { key ->
            val commitDetailViewModel: CommitDetailViewModel = hiltViewModel()
            CommitDetailScreen(
                viewModel = commitDetailViewModel,
                projectId = key.projectId,
                sha = key.sha,
                onIssueClick = { projectId, issueIid ->
                    navigator.navigate(Screen.IssueDetail(projectId, issueIid))
                },
                onMRClick = { projectId, iid ->
                    navigator.navigate(Screen.MergeRequestDetail(projectId, iid))
                }
            )
        }
        entry<Screen.Pipelines> { key ->
            val pipelinesViewModel: PipelinesViewModel = hiltViewModel()
            PipelinesScreen(
                projectId = key.projectId,
                viewModel = pipelinesViewModel,
                onPipelineClick = { projectId, pipelineId ->
                    navigator.navigate(Screen.PipelineDetail(projectId, pipelineId))
                }
            )
        }
        entry<Screen.Search> {
            val searchViewModel: SearchViewModel = hiltViewModel()
            SearchScreen(
                viewModel = searchViewModel,
                onBackClick = { navigator.goBack() },
                onProjectClick = { projectId ->
                    navigator.navigate(Screen.Repository(projectId))
                },
                onUserClick = { userId ->
                    navigator.navigate(Screen.UserProfile(userId))
                },
                onIssueClick = { projectId, issueIid ->
                    navigator.navigate(Screen.IssueDetail(projectId, issueIid))
                },
                onMRClick = { projectId, iid ->
                    navigator.navigate(Screen.MergeRequestDetail(projectId, iid))
                }
            )
        }
        entry<Screen.EditProfile> {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            val state by profileViewModel.uiState.collectAsState()
            (state as? ProfileUiState.Success)?.user?.let { user ->
                EditProfileScreen(
                    user = user,
                    viewModel = profileViewModel
                )
            } ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        entry<Screen.Settings> {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            val state by profileViewModel.uiState.collectAsState()
            (state as? ProfileUiState.Success)?.user?.let { user ->
                SettingsScreen(
                    user = user,
                    viewModel = settingsViewModel,
                    onProfileDetailsClick = { navigator.navigate(Screen.EditProfile) },
                    onLogoutClick = { authViewModel.logout() }
                )
            } ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        entry<Screen.UserProfile> { key ->
            val profileViewModel: ProfileViewModel = hiltViewModel()
            UserProfileScreen(
                userId = key.userId,
                username = key.username,
                viewModel = profileViewModel
            )
        }
        entry<Screen.Projects> {
            val projectViewModel: ProjectViewModel = hiltViewModel()
            ProjectListScreen(
                viewModel = projectViewModel,
                onProjectClick = { project ->
                    navigator.navigate(Screen.Repository(project.id))
                },
                onGroupClick = { groupId ->
                    navigator.navigate(Screen.GroupDetail(groupId))
                }
            )
        }
        entry<Screen.GroupDetail> { key ->
            val groupDetailViewModel: GroupDetailViewModel = hiltViewModel()
            GroupDetailScreen(
                groupId = key.groupId,
                viewModel = groupDetailViewModel,
                onProjectClick = { project ->
                    navigator.navigate(Screen.Repository(project.id))
                },
                onSubgroupClick = { subGroupId ->
                    navigator.navigate(Screen.GroupDetail(subGroupId))
                },
                onIssueClick = { projectId, issueIid ->
                    navigator.navigate(Screen.IssueDetail(projectId, issueIid))
                },
                onMRClick = { projectId, iid ->
                    navigator.navigate(Screen.MergeRequestDetail(projectId, iid))
                }
            )
        }
        entry<Screen.Repository> { key ->
            val repositoryViewModel: RepositoryViewModel = hiltViewModel()

            RepositoryScreen(
                projectId = key.projectId,
                viewModel = repositoryViewModel,
                onIssueClick = { projectId, issueIid ->
                    navigator.navigate(Screen.IssueDetail(projectId, issueIid))
                },
                onMRClick = { projectId, iid ->
                    navigator.navigate(Screen.MergeRequestDetail(projectId, iid))
                },
                onCommitClick = { projectId, sha ->
                    navigator.navigate(Screen.CommitDetail(projectId, sha))
                },
                onCommitsListClick = { projectId, ref ->
                    navigator.navigate(Screen.CommitList(projectId, ref))
                },
                onFileClick = { path, ref ->
                    navigator.navigate(Screen.FileDetail(key.projectId, path, ref))
                },
                onCreateIssueClick = { projectId ->
                    navigator.navigate(Screen.CreateIssue(projectId))
                },
                onPipelinesClick = { projectId ->
                    navigator.navigate(Screen.Pipelines(projectId))
                },
                onNamespaceClick = { id, kind ->
                    if (kind == "group") {
                        navigator.navigate(Screen.GroupDetail(id))
                    } else {
                        navigator.navigate(Screen.UserProfile(id))
                    }
                },
                onDuoClick = {
                    val state = repositoryViewModel.uiState.value as? RepositoryUiState.Success
                    val project = state?.project
                    val context = project?.let {
                        listOf(
                            DuoContextItem(
                                category = "repository",
                                id = it.id.toString(),
                                content = "I'm looking at this repository: ${it.nameWithNamespace}\nURL: ${it.webUrl}\n\nDescription: ${it.description}"
                            )
                        )
                    }
                    navigator.navigate(
                        Screen.DuoChat(
                            projectId = key.projectId,
                            rootNamespaceId = 9970,
                            additionalContext = context
                        )
                    )
                },
                hasDuoAccess = currentUser?.hasDuoAccess ?: false
            )
        }
        entry<Screen.FileDetail> { key ->
            val fileDetailViewModel: FileDetailViewModel = hiltViewModel()
            FileDetailScreen(
                projectId = key.projectId,
                filePath = key.filePath,
                ref = key.ref,
                viewModel = fileDetailViewModel
            )
        }
        entry<Screen.Todo> {
            val todoViewModel: TodoViewModel = hiltViewModel()
            TodosScreen(
                viewModel = todoViewModel,
                onTodoClick = { projectId, iid, type ->
                    when (type) {
                        "Issue" -> navigator.navigate(
                            Screen.IssueDetail(
                                projectId,
                                iid
                            )
                        )

                        "MergeRequest" -> navigator.navigate(
                            Screen.MergeRequestDetail(
                                projectId,
                                iid
                            )
                        )
                    }
                }
            )
        }
        entry<Screen.Issues> {
            val issueViewModel: IssueViewModel = hiltViewModel()
            IssuesScreen(
                viewModel = issueViewModel,
                onIssueClick = { projectId, issueIid ->
                    navigator.navigate(Screen.IssueDetail(projectId, issueIid))
                },
                onEpicClick = { groupId, epicIid ->
                    navigator.navigate(Screen.EpicDetail(groupId, epicIid))
                }
            )
        }
        entry<Screen.MergeRequests> {
            val mrListViewModel: MergeRequestListViewModel = hiltViewModel()
            MergeRequestListScreen(
                viewModel = mrListViewModel,
                onMRClick = { projectId, iid ->
                    navigator.navigate(Screen.MergeRequestDetail(projectId, iid))
                }
            )
        }
        entry<Screen.MergeRequestDetail> { key ->
            val mrViewModel: MergeRequestViewModel = hiltViewModel()
            val uiState by mrViewModel.uiState.collectAsState()
            MergeRequestScreen(
                viewModel = mrViewModel,
                projectId = key.projectId,
                iid = key.iid,
                onPipelineClick = { projectId, pipelineId ->
                    navigator.navigate(Screen.PipelineDetail(projectId, pipelineId))
                },
                onProfileClick = { userId ->
                    navigator.navigate(Screen.UserProfile(userId))
                },
                onIssueClick = { projectId, issueIid ->
                    navigator.navigate(Screen.IssueDetail(projectId, issueIid))
                },
                onMRClick = { projectId, iid ->
                    navigator.navigate(Screen.MergeRequestDetail(projectId, iid))
                },
                onEditClick = { projectId, iid ->
                    navigator.navigate(Screen.MergeRequestEdit(projectId, iid))
                },
                onDuoClick = {
                    val state = uiState as? MergeRequestUiState.Success
                    val mr = state?.mergeRequest
                    val context = mr?.let {
                        listOf(
                            DuoContextItem(
                                category = "merge_request",
                                id = it.id.toString(),
                                content = "I'm looking at this Merge Request: ${it.title}\nURL: ${it.webUrl}\n\nDescription: ${it.description}"
                            )
                        )
                    }
                    navigator.navigate(
                        Screen.DuoChat(
                            projectId = key.projectId,
                            rootNamespaceId = 9970,
                            additionalContext = context
                        )
                    )
                },
                onBackClick = { navigator.goBack() },
                hasDuoAccess = currentUser?.hasDuoAccess ?: false
            )
        }
        entry<Screen.MergeRequestEdit> { key ->
            val mrViewModel: MergeRequestViewModel = hiltViewModel()
            val uiState by mrViewModel.uiState.collectAsState()

            LaunchedEffect(key.projectId, key.iid) {
                mrViewModel.fetchMergeRequest(key.projectId, key.iid)
            }

            when (val state = uiState) {
                is MergeRequestUiState.Success -> {
                    MergeRequestEditScreen(
                        projectId = key.projectId,
                        mergeRequest = state.mergeRequest,
                        viewModel = mrViewModel,
                        onBackClick = { navigator.goBack() },
                        onSaved = { navigator.goBack() }
                    )
                }

                is MergeRequestUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is MergeRequestUiState.Error -> {
                    Text(state.message)
                }
            }
        }
        entry<Screen.IssueDetail> { key ->
            val issueDetailViewModel: IssueDetailViewModel = hiltViewModel()
            val uiState by issueDetailViewModel.uiState.collectAsState()
            IssueDetailScreen(
                projectId = key.projectId,
                issueIid = key.issueIid,
                viewModel = issueDetailViewModel,
                onEditClick = { projectId, iid ->
                    navigator.navigate(Screen.EditIssue(projectId, iid))
                },
                onProfileClick = { userId ->
                    navigator.navigate(Screen.UserProfile(userId))
                },
                onIssueClick = { projectId, issueIid ->
                    navigator.navigate(Screen.IssueDetail(projectId, issueIid))
                },
                onMRClick = { projectId, iid ->
                    navigator.navigate(Screen.MergeRequestDetail(projectId, iid))
                },
                onDuoClick = {
                    val state = uiState as? IssueDetailUiState.Success
                    val issue = state?.issue
                    val context = issue?.let {
                        listOf(
                            DuoContextItem(
                                category = "issue",
                                id = it.id.toString(),
                                content = "I'm looking at this issue: ${it.title}\nURL: ${it.webUrl}\n\nDescription: ${it.description}"
                            )
                        )
                    }
                    navigator.navigate(
                        Screen.DuoChat(
                            projectId = key.projectId,
                            rootNamespaceId = 9970,
                            additionalContext = context
                        )
                    )
                },
                onBackClick = { navigator.goBack() },
                hasDuoAccess = currentUser?.hasDuoAccess ?: false
            )
        }
        entry<Screen.CreateIssue> { key ->
            val issueDetailViewModel: IssueDetailViewModel = hiltViewModel()
            IssueEditScreen(
                projectId = key.projectId,
                viewModel = issueDetailViewModel,
                onBackClick = { navigator.goBack() },
                onIssueSaved = { navigator.goBack() }
            )
        }
        entry<Screen.EditIssue> { key ->
            val issueDetailViewModel: IssueDetailViewModel = hiltViewModel()
            val state by issueDetailViewModel.uiState.collectAsState()

            LaunchedEffect(key.projectId, key.issueIid) {
                issueDetailViewModel.loadIssueDetail(key.projectId, key.issueIid)
            }

            when (val s = state) {
                is IssueDetailUiState.Success -> {
                    IssueEditScreen(
                        projectId = key.projectId,
                        issue = s.issue,
                        viewModel = issueDetailViewModel,
                        onBackClick = { navigator.goBack() },
                        onIssueSaved = { navigator.goBack() }
                    )
                }

                is IssueDetailUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is IssueDetailUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = s.message)
                    }
                }
            }
        }
        entry<Screen.EpicDetail> { key ->
            val epicDetailViewModel: EpicDetailViewModel = hiltViewModel()
            EpicDetailScreen(
                groupId = key.groupId,
                epicIid = key.epicIid,
                viewModel = epicDetailViewModel,
                onIssueClick = { projectId, issueIid ->
                    navigator.navigate(Screen.IssueDetail(projectId, issueIid))
                }
            )
        }
        entry<Screen.PipelineDetail> { key ->
            val pipelineViewModel: PipelineViewModel = hiltViewModel()
            PipelineScreen(
                viewModel = pipelineViewModel,
                projectId = key.projectId,
                pipelineId = key.pipelineId
            )
        }
        entry<Screen.DuoChat> { key ->
            DuoChatScreen(
                projectId = key.projectId,
                rootNamespaceId = key.rootNamespaceId,
                initialGoal = key.initialGoal,
                additionalContext = key.additionalContext,
                onBackClick = { navigator.goBack() }
            )
        }
        entry<Screen.Profile> {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            ProfileScreen(
                viewModel = profileViewModel,
                onProjectClick = { projectId ->
                    navigator.navigate(Screen.Repository(projectId))
                },
                onSeeAllClick = {
                    navigator.navigate(Screen.Projects)
                },
                onEditProfileClick = { navigator.navigate(Screen.EditProfile) },
                onSettingsClick = { navigator.navigate(Screen.Settings) },
                onActivityClick = { projectId, iid, type ->
                    when (type.lowercase()) {
                        "issue" -> navigator.navigate(
                            Screen.IssueDetail(
                                projectId,
                                iid
                            )
                        )

                        "mergerequest", "merge_request" -> navigator.navigate(
                            Screen.MergeRequestDetail(
                                projectId,
                                iid
                            )
                        )

                        "push" -> navigator.navigate(Screen.Repository(projectId))
                        "project" -> navigator.navigate(Screen.Repository(projectId))
                    }
                }
            )
        }
    }
}
