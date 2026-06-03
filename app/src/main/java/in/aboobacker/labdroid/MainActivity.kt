package `in`.aboobacker.labdroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import `in`.aboobacker.labdroid.ui.components.DetailTopBar
import `in`.aboobacker.labdroid.ui.components.HomeBottomNavigation
import `in`.aboobacker.labdroid.ui.components.HomeTopBar
import `in`.aboobacker.labdroid.ui.components.LocalTopBarActions
import `in`.aboobacker.labdroid.ui.components.TopBarActions
import `in`.aboobacker.labdroid.ui.screens.LoginScreen
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import `in`.aboobacker.labdroid.ui.viewmodel.AuthState
import `in`.aboobacker.labdroid.ui.viewmodel.AuthViewModel
import `in`.aboobacker.labdroid.ui.viewmodel.SettingsViewModel
import kotlinx.serialization.Serializable
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var authService: AuthorizationService

    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (data != null) {
            val response = AuthorizationResponse.fromIntent(data)
            val ex = AuthorizationException.fromIntent(data)
            val authViewModel: AuthViewModel by viewModels()
            if (response != null) {
                authViewModel.onAuthResponse(response)
            } else if (ex != null) {
                authViewModel.onAuthError(ex)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authService = AuthorizationService(this)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()

            LabdroidTheme(darkTheme = isDarkMode) {
                val authViewModel: AuthViewModel = hiltViewModel()
                val authState by authViewModel.authState.collectAsState()
                val currentUser by authViewModel.currentUser.collectAsState()

                LaunchedEffect(authState) {
                    if (authState is AuthState.Authenticated) {
                        settingsViewModel.toggleDarkMode(isDarkMode) // Ensure sync if needed, though state is already from store
                    }
                }

                val topBarActions = remember { TopBarActions() }

                CompositionLocalProvider(LocalTopBarActions provides topBarActions) {
                    val navigationState = rememberNavigationState(
                        startRoute = Screen.Projects,
                        topLevelRoutes = setOf(
                            Screen.Projects,
                            Screen.Issues,
                            Screen.MergeRequests,
                            Screen.Profile
                        )
                    )
                    val navigator = remember { Navigator(navigationState) }

                    val entryProvider = createAppEntryProvider(
                        navigator = navigator,
                        authViewModel = authViewModel,
                        settingsViewModel = settingsViewModel
                    )

                    when (val state = authState) {
                        is AuthState.Authenticated -> {
                            val currentBackStack =
                                navigationState.backStacks[navigationState.topLevelRoute]
                            val currentScreen =
                                (currentBackStack?.lastOrNull() ?: navigationState.startRoute) as Screen
                            val isTopLevel = currentScreen.isTopLevel
                            val hideNavigation = currentScreen.hideNavigation

                            LaunchedEffect(currentScreen) {
                                topBarActions.topBar = null
                            }

                            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
                                rememberTopAppBarState()
                            )

                            val content = @Composable {
                                Scaffold(
                                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                                    topBar = {
                                        val customTopBar = topBarActions.topBar
                                        if (customTopBar != null) {
                                            customTopBar()
                                        } else if (isTopLevel) {
                                            HomeTopBar(
                                                user = currentUser,
                                                title = (currentScreen as? Screen)?.getTitle()
                                                    ?: "GitLab",
                                                scrollBehavior = scrollBehavior,
                                                onSearchClick = { navigator.navigate(Screen.Search) },
                                                onTodosClick = { navigator.navigate(Screen.Todo) },
                                                onProfileClick = { navigator.navigate(Screen.Profile) }
                                            )
                                        } else if (currentScreen !is Screen.Search && currentScreen !is Screen.DuoChat && !hideNavigation) {
                                            DetailTopBar(
                                                title = (currentScreen as? Screen)?.getTitle()
                                                    ?: "GitLab",
                                                onBackClick = { navigator.goBack() }
                                            )
                                        }
                                    }
                                ) { padding ->
                                    Box(modifier = Modifier.padding(padding)) {
                                        NavDisplay(
                                            entries = navigationState.toEntries(entryProvider),
                                            onBack = { navigator.goBack() },
                                            sceneStrategies = remember { listOf(DialogSceneStrategy<NavKey>()) }
                                        )
                                    }
                                }
                            }

                            if (hideNavigation) {
                                content()
                            } else {
                                NavigationSuiteScaffold(
                                    navigationSuiteItems = {
                                        item(
                                            selected = currentScreen is Screen.Projects,
                                            onClick = { navigator.navigate(Screen.Projects) },
                                            icon = { Icon(Icons.Default.Folder, null) },
                                            label = { Text("Projects") }
                                        )
                                        item(
                                            selected = currentScreen is Screen.Issues,
                                            onClick = { navigator.navigate(Screen.Issues) },
                                            icon = { Icon(Icons.AutoMirrored.Filled.Assignment, null) },
                                            label = { Text("Issues") }
                                        )
                                        item(
                                            selected = currentScreen is Screen.MergeRequests,
                                            onClick = { navigator.navigate(Screen.MergeRequests) },
                                            icon = { Icon(Icons.AutoMirrored.Filled.CallMerge, null) },
                                            label = { Text("MRs") }
                                        )
                                        item(
                                            selected = currentScreen is Screen.Todo,
                                            onClick = { navigator.navigate(Screen.Todo) },
                                            icon = { Icon(Icons.Default.CheckCircle, null) },
                                            label = { Text("Todos") }
                                        )
                                        item(
                                            selected = currentScreen is Screen.Profile,
                                            onClick = { navigator.navigate(Screen.Profile) },
                                            icon = { Icon(Icons.Default.Person, null) },
                                            label = { Text("Profile") }
                                        )
                                    }
                                ) {
                                    content()
                                }
                            }
                        }

                        is AuthState.Unauthenticated, is AuthState.Error -> {
                            LoginScreen(
                                onLoginClick = {
                                    val authRequest = authViewModel.getAuthorizationRequest()
                                    val intent =
                                        authService.getAuthorizationRequestIntent(authRequest)
                                    authLauncher.launch(intent)
                                },
                                error = (state as? AuthState.Error)?.message
                            )
                        }

                        is AuthState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        authService.dispose()
    }
}

@Serializable
data class DuoContextItem(
    val category: String,
    val id: String? = null,
    val content: String? = null,
    val metadata: Map<String, String>? = null
)

sealed class Screen : NavKey {
    @Serializable
    data object Profile : Screen()

    @Serializable
    data object EditProfile : Screen()

    @Serializable
    data object Settings : Screen()

    @Serializable
    data object Projects : Screen()

    @Serializable
    data object Todo : Screen()

    @Serializable
    data object Issues : Screen()

    @Serializable
    data object MergeRequests : Screen()

    @Serializable
    data object Search : Screen()

    @Serializable
    data class UserProfile(val userId: Long? = null, val username: String? = null) : Screen()

    @Serializable
    data class MergeRequestDetail(val projectId: Long, val iid: Long) : Screen()

    @Serializable
    data class MergeRequestEdit(val projectId: Long, val iid: Long) : Screen()

    @Serializable
    data class Repository(val projectId: Long) : Screen()

    @Serializable
    data class GroupDetail(val groupId: Long) : Screen()

    @Serializable
    data class IssueDetail(val projectId: Long, val issueIid: Long) : Screen()

    @Serializable
    data class CreateIssue(val projectId: Long) : Screen()

    @Serializable
    data class EditIssue(val projectId: Long, val issueIid: Long) : Screen()

    @Serializable
    data class EpicDetail(val groupId: Long, val epicIid: Long) : Screen()

    @Serializable
    data class PipelineDetail(val projectId: Long, val pipelineId: Long) : Screen()

    @Serializable
    data class CommitList(val projectId: Long, val ref: String? = null) : Screen()

    @Serializable
    data class CommitDetail(val projectId: Long, val sha: String) : Screen()

    @Serializable
    data class FileDetail(val projectId: Long, val filePath: String, val ref: String) : Screen()

    @Serializable
    data class Pipelines(val projectId: Long) : Screen()

    @Serializable
    data class DuoChat(
        val projectId: Long? = null,
        val rootNamespaceId: Long? = null,
        val initialGoal: String? = null,
        val additionalContext: List<DuoContextItem>? = null
    ) : Screen()

    fun toCategory(): NavigationCategory = when (this) {
        is Projects, is Repository, is GroupDetail,
        is CommitList, is CommitDetail, is FileDetail, is Pipelines -> NavigationCategory.PROJECTS

        is Issues, is IssueDetail, is CreateIssue, is EditIssue, is EpicDetail -> NavigationCategory.ISSUES
        is MergeRequests, is MergeRequestDetail, is MergeRequestEdit, is PipelineDetail -> NavigationCategory.MRS
        is Profile, is EditProfile, is Settings, is UserProfile, is DuoChat -> NavigationCategory.PROFILE
        is Todo -> NavigationCategory.NONE
        is Search -> NavigationCategory.NONE
    }

    val isTopLevel: Boolean
        get() = this is Projects || this is Issues || this is MergeRequests || this is Profile || this is Todo

    val hideNavigation: Boolean
        get() = this is DuoChat || this is EditIssue || this is CreateIssue || this is MergeRequestEdit

    fun getTitle(): String? = when (this) {
        is Projects -> "Projects"
        is Issues -> "Work Items"
        is MergeRequests -> "Merge Requests"
        is Profile -> "Profile"
        is Todo -> "Pending Actions"
        is Repository -> "Repository"
        is GroupDetail -> "Group Details"
        is IssueDetail -> "Issue Details"
        is CreateIssue -> "New Issue"
        is EditIssue -> "Edit Issue"
        is MergeRequestDetail -> "Merge Request"
        is MergeRequestEdit -> "Edit Merge Request"
        is EpicDetail -> "Epic Details"
        is PipelineDetail -> "Pipeline"
        is CommitList -> "Commits"
        is CommitDetail -> "Commit"
        is FileDetail -> "File"
        is Search -> "Search"
        is UserProfile -> "User Profile"
        is EditProfile -> "Edit Profile"
        is Settings -> "Settings"
        is Pipelines -> "Pipelines"
        is DuoChat -> "GitLab Duo Chat"
    }
}

enum class NavigationCategory {
    PROJECTS, ISSUES, MRS, PROFILE, NONE
}
