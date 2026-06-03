package `in`.aboobacker.labdroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import `in`.aboobacker.labdroid.R
import `in`.aboobacker.labdroid.data.model.Permissions
import `in`.aboobacker.labdroid.data.model.Pipeline
import `in`.aboobacker.labdroid.data.model.PipelineJob
import `in`.aboobacker.labdroid.data.model.Project
import `in`.aboobacker.labdroid.data.model.ProjectAccess
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.data.model.UserPermissions
import `in`.aboobacker.labdroid.ui.components.GroupList
import `in`.aboobacker.labdroid.ui.components.SkeletonLoader
import `in`.aboobacker.labdroid.ui.components.StatusBadge
import `in`.aboobacker.labdroid.ui.components.formatTimeAgo
import `in`.aboobacker.labdroid.ui.components.getAccessLevelName
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import `in`.aboobacker.labdroid.ui.viewmodel.ProjectUiState
import `in`.aboobacker.labdroid.ui.viewmodel.ProjectViewModel

@Composable
fun ProjectListScreen(
    viewModel: ProjectViewModel,
    onProjectClick: (Project) -> Unit = {},
    onGroupClick: (Long) -> Unit = {},
    onSeeAllStarredClick: () -> Unit = {},
    onSeeAllPersonalClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProjectListScreenContent(
        uiState = uiState,
        onProjectClick = { project ->
            viewModel.saveRecentProject(project)
            onProjectClick(project)
        },
        onGroupClick = onGroupClick,
        onSeeAllStarredClick = onSeeAllStarredClick,
        onSeeAllPersonalClick = onSeeAllPersonalClick,
        onRefresh = { viewModel.fetchProjects() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreenContent(
    uiState: ProjectUiState,
    onProjectClick: (Project) -> Unit = {},
    onGroupClick: (Long) -> Unit = {},
    onSeeAllStarredClick: () -> Unit = {},
    onSeeAllPersonalClick: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Projects", "Groups")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, _ ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                if (index == 0) stringResource(R.string.projects) else stringResource(
                                    R.string.groups
                                )
                            )
                        }
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (selectedTabIndex == 0) {
                    PullToRefreshBox(
                        isRefreshing = uiState is ProjectUiState.Loading,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (uiState) {
                            is ProjectUiState.Loading -> {
                                ProjectDashboardSkeleton()
                            }

                            is ProjectUiState.Success -> {
                                ProjectDashboard(
                                    starredProjects = uiState.starredProjects,
                                    personalProjects = uiState.personalProjects,
                                    recentProjects = uiState.recentProjects,
                                    onProjectClick = onProjectClick,
                                    onSeeAllStarredClick = onSeeAllStarredClick,
                                    onSeeAllPersonalClick = onSeeAllPersonalClick,
                                )
                            }

                            is ProjectUiState.Error -> {
                                Text(
                                    text = uiState.message,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                } else {
                    GroupsTabContent(onGroupClick = onGroupClick, onRefresh = onRefresh)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsTabContent(onGroupClick: (Long) -> Unit, onRefresh: () -> Unit = {}) {
    val groupViewModel: `in`.aboobacker.labdroid.ui.viewmodel.GroupViewModel =
        androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
    val groupUiState by groupViewModel.uiState.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = groupUiState is `in`.aboobacker.labdroid.ui.viewmodel.GroupUiState.Loading,
        onRefresh = {
            onRefresh()
            groupViewModel.fetchGroups()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = groupUiState) {
                is `in`.aboobacker.labdroid.ui.viewmodel.GroupUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is `in`.aboobacker.labdroid.ui.viewmodel.GroupUiState.Success -> {
                    GroupList(state.groups, onGroupClick = onGroupClick)
                }

                is `in`.aboobacker.labdroid.ui.viewmodel.GroupUiState.Error -> {
                    Text(
                        state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}


@Composable
fun ProjectDashboard(
    starredProjects: List<Project>,
    personalProjects: List<Project>,
    recentProjects: List<Project>,
    onProjectClick: (Project) -> Unit,
    onSeeAllStarredClick: () -> Unit = {},
    onSeeAllPersonalClick: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (recentProjects.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.recently_visited),
                    icon = Icons.Default.History
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    recentProjects.take(3).forEach { project ->
                        RecentlyVisitedItem(
                            project.name,
                            "Updated ${formatTimeAgo(project.lastActivityAt.orEmpty())}",
                            Icons.Default.Folder,
                            imageUrl = project.avatarUrl,
                            onClick = { onProjectClick(project) }
                        )
                    }
                }
            }
        }

        if (starredProjects.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.starred),
                    icon = Icons.Default.StarBorder,
                    onSeeAllClick = onSeeAllStarredClick
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    starredProjects.take(5).forEach { project ->
                        StarredProjectCard(project, onProjectClick)
                    }
                }
            }
        }

        if (personalProjects.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.personal_projects),
                    icon = Icons.Default.Person,
                    onSeeAllClick = onSeeAllPersonalClick
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    personalProjects.take(5).forEach { project ->
                        StarredProjectCard(project, onProjectClick)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector, onSeeAllClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        if (onSeeAllClick != null) {
            Text(
                text = stringResource(R.string.see_all),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onSeeAllClick() }
            )
        }
    }
}

@Composable
fun StarredProjectCard(project: Project, onClick: (Project) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(project) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (!project.avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = project.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )

                    val accessLevel = project.permissions?.projectAccess?.accessLevel
                        ?: project.permissions?.maxAccessLevel
                    if (accessLevel != null && accessLevel >= 40) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = getAccessLevelName(accessLevel),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = project.nameWithNamespace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(
                        "${project.starCount} stars",
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatusBadge(
                        "${project.forksCount} forks",
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun RecentlyVisitedItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    imageUrl: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            if (!imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}


@Composable
fun ProjectDashboardSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        repeat(3) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkeletonLoader(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    SkeletonLoader(modifier = Modifier.size(150.dp, 20.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(3) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SkeletonLoader(
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                SkeletonLoader(modifier = Modifier.size(120.dp, 16.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                SkeletonLoader(modifier = Modifier.size(80.dp, 12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProjectListScreenPreview() {
    val sampleProjects = listOf(
        Project(
            id = 1,
            name = "gitlab-mobile-app",
            nameWithNamespace = "mobile-team / android",
            description = "A professional GitLab client for Android built with Jetpack Compose.",
            webUrl = "https://gitlab.com/aboobacker/labdroid",
            starCount = 12,
            forksCount = 5,
            defaultBranch = "main",
            lastActivityAt = "2023-10-01T10:00:00Z",
            permissions = Permissions(
                projectAccess = ProjectAccess(accessLevel = 40),
                userPermissions = UserPermissions(
                    pushCode = true,
                    downloadCode = true,
                    adminProject = true,
                    adminIssue = true,
                    createIssue = true,
                    createMergeRequestIn = true
                )
            )
        ),
        Project(
            id = 2,
            name = "design-system-m3",
            nameWithNamespace = "ux-foundations / core",
            description = "A collection of reusable Jetpack Compose components.",
            webUrl = "https://gitlab.com/aboobacker/compose-ui-kit",
            starCount = 45,
            forksCount = 10,
            defaultBranch = "main",
            lastActivityAt = "2023-10-02T10:00:00Z",
            permissions = Permissions(
                projectAccess = ProjectAccess(accessLevel = 30),
                userPermissions = UserPermissions(
                    pushCode = true,
                    downloadCode = true,
                    createIssue = true,
                    createMergeRequestIn = true
                )
            )
        )
    )
    LabdroidTheme {
        ProjectListScreenContent(
            uiState = ProjectUiState.Success(
                user = User(
                    1,
                    "Aboobacker",
                    "aboobacker",
                    "https://secure.gravatar.com/avatar/sample"
                ),
                projects = sampleProjects,
                starredProjects = sampleProjects,
                activePipeline = Pipeline(
                    id = 1,
                    projectId = 1,
                    status = "running",
                    ref = "main",
                    sha = "abc",
                    webUrl = "https://gitlab.com",
                    createdAt = "2023-10-01T10:00:00Z",
                    updatedAt = "2023-10-01T10:00:00Z"
                ),
                activePipelineProject = sampleProjects.first(),
                pipelineJobs = listOf(
                    PipelineJob(1, "success", "build", "build-job"),
                    PipelineJob(2, "running", "test", "test-job"),
                    PipelineJob(3, "pending", "deploy", "deploy-job"),
                )
            ),
            onProjectClick = {},
        )
    }
}
