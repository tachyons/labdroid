package `in`.aboobacker.labdroid.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import `in`.aboobacker.labdroid.data.model.Group
import `in`.aboobacker.labdroid.data.model.GroupDetail
import `in`.aboobacker.labdroid.data.model.GroupMember
import `in`.aboobacker.labdroid.data.model.Issue
import `in`.aboobacker.labdroid.data.model.MergeRequest
import `in`.aboobacker.labdroid.data.model.Project
import `in`.aboobacker.labdroid.ui.components.ExpressiveDetailTopBar
import `in`.aboobacker.labdroid.ui.components.IssueItem
import `in`.aboobacker.labdroid.ui.components.LocalTopBarActions
import `in`.aboobacker.labdroid.ui.components.MergeRequestItem
import `in`.aboobacker.labdroid.ui.components.formatTimeAgo
import `in`.aboobacker.labdroid.ui.components.getAccessLevelName
import `in`.aboobacker.labdroid.ui.viewmodel.GroupDetailUiState
import `in`.aboobacker.labdroid.ui.viewmodel.GroupDetailViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: Long,
    viewModel: GroupDetailViewModel,
    onProjectClick: (Project) -> Unit,
    onSubgroupClick: (Long) -> Unit,
    onIssueClick: (Long, Long) -> Unit,
    onMRClick: (Long, Long) -> Unit,
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val topBarActions = LocalTopBarActions.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is GroupDetailUiState.Success) {
            val group = state.group
            topBarActions.topBar = {
                var showMenu by remember { mutableStateOf(false) }

                ExpressiveDetailTopBar(
                    title = group.name,
                    onBackClick = onBackClick,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        IconButton(onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, group.webUrl)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }

                        Box {
                            IconButton(onClick = { showMenu = !showMenu }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Open in Browser") },
                                    onClick = {
                                        showMenu = false
                                        val intent =
                                            Intent(Intent.ACTION_VIEW, Uri.parse(group.webUrl))
                                        context.startActivity(intent)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Copy Link") },
                                    onClick = {
                                        showMenu = false
                                        clipboardManager.setText(AnnotatedString(group.webUrl))
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    LaunchedEffect(groupId) {
        viewModel.loadGroupDetail(groupId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        when (val state = uiState) {
            is GroupDetailUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is GroupDetailUiState.Success -> {
                GroupDetailContent(
                    state = state,
                    onProjectClick = onProjectClick,
                    onSubgroupClick = onSubgroupClick,
                    onIssueClick = onIssueClick,
                    onMRClick = onMRClick,
                    viewModel = viewModel
                )
            }

            is GroupDetailUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun GroupDetailContent(
    state: GroupDetailUiState.Success,
    onProjectClick: (Project) -> Unit,
    onSubgroupClick: (Long) -> Unit,
    onIssueClick: (Long, Long) -> Unit,
    onMRClick: (Long, Long) -> Unit,
    viewModel: GroupDetailViewModel
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Overview" to Icons.Default.History,
        "Subgroups" to Icons.Default.FolderOpen,
        "Members" to Icons.Default.PeopleOutline,
        "Issues" to Icons.Default.WorkOutline,
        "Merge Requests" to Icons.Default.AccountTree
    )

    Column(modifier = Modifier.fillMaxSize()) {
        GroupHeader(
            state.group,
            state.members.size,
            state.projects.size,
            state.subgroups.size,
            onParentClick = onSubgroupClick
        )

        SecondaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(selectedTabIndex, true),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, (title, icon) ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1
                        )
                    },
                    icon = {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when (selectedTabIndex) {
            0 -> GroupOverview(
                state,
                onProjectClick,
                onSubgroupClick,
                onViewAllSubgroups = { selectedTabIndex = 1 },
                onManageMembers = { selectedTabIndex = 2 }
            )

            1 -> GroupSubgroupsTab(
                subgroups = state.subgroups,
                onSubgroupClick = onSubgroupClick,
                isLoading = state.isLoadingSubgroups,
                hasMore = state.hasMoreSubgroups,
                onLoadMore = { viewModel.loadMoreSubgroups() }
            )

            2 -> GroupMembersTab(
                members = state.members,
                isLoading = state.isLoadingMembers,
                hasMore = state.hasMoreMembers,
                onLoadMore = { viewModel.loadMoreMembers() }
            )

            3 -> GroupIssues(state.issues, state.isLoadingIssues, onIssueClick)
            4 -> GroupMergeRequests(state.mergeRequests, state.isLoadingMergeRequests, onMRClick)
        }
    }
}

@Composable
fun GroupHeader(
    group: GroupDetail,
    memberCount: Int,
    projectCount: Int,
    subgroupCount: Int,
    onParentClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF673AB7)),
                contentAlignment = Alignment.Center
            ) {
                if (group.avatarUrl != null) {
                    AsyncImage(
                        model = group.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                if (group.parentId != null) {
                    Text(
                        text = "Parent Group",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onParentClick(group.parentId) }
                    )
                }
                Text(
                    text = group.fullPath,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HeaderStat(icon = Icons.Default.People, label = "$memberCount Members")
            HeaderStat(icon = Icons.Default.Work, label = "$projectCount Projects")
            HeaderStat(icon = Icons.Default.AccountTree, label = "$subgroupCount Subgroups")
        }
    }
}

@Composable
fun HeaderStat(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        color = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun GroupOverview(
    state: GroupDetailUiState.Success,
    onProjectClick: (Project) -> Unit,
    onSubgroupClick: (Long) -> Unit,
    onViewAllSubgroups: () -> Unit,
    onManageMembers: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionTitle(title = "Subgroups", onActionClick = onViewAllSubgroups)
        }
        if (state.isLoadingSubgroups && state.subgroups.isEmpty()) {
            item { SectionLoadingIndicator() }
        } else if (state.subgroups.isNotEmpty()) {
            items(state.subgroups.take(3)) { subgroup ->
                SubgroupCard(subgroup, onClick = { onSubgroupClick(subgroup.id) })
            }
        } else {
            item { SectionEmptyText("No subgroups") }
        }

        item {
            SectionTitle(title = "Projects", actionLabel = "Search", onActionClick = {})
        }
        if (state.isLoadingProjects && state.projects.isEmpty()) {
            item { SectionLoadingIndicator() }
        } else if (state.projects.isNotEmpty()) {
            items(state.projects.take(3)) { project ->
                GroupProjectCard(project, onClick = { onProjectClick(project) })
            }
        } else {
            item { SectionEmptyText("No projects") }
        }

        item {
            SectionTitle(
                title = "Members",
                actionLabel = "Manage",
                onActionClick = onManageMembers
            )
        }
        if (state.isLoadingMembers && state.members.isEmpty()) {
            item { SectionLoadingIndicator() }
        } else if (state.members.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.LightGray.copy(alpha = 0.3f)
                    ),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column {
                        state.members.take(3).forEach { member ->
                            MemberItem(member)
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = Color.LightGray.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        } else {
            item { SectionEmptyText("No members") }
        }
    }
}

@Composable
fun SectionLoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
    }
}

@Composable
fun SectionEmptyText(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray,
        textAlign = TextAlign.Center
    )
}

@Composable
fun SectionTitle(title: String, actionLabel: String = "View All", onActionClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val icon = when (title) {
                "Subgroups" -> Icons.Default.FolderOpen
                "Projects" -> Icons.Default.WorkOutline
                "Members" -> Icons.Default.PeopleOutline
                else -> Icons.Default.Folder
            }
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        TextButton(onClick = onActionClick) {
            Text(actionLabel, color = Color(0xFF673AB7))
        }
    }
}

@Composable
fun SubgroupCard(subgroup: Group, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = Color.Gray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(subgroup.name, fontWeight = FontWeight.Bold)
                Text(
                    subgroup.fullPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun GroupProjectCard(project: Project, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(project.name, color = Color(0xFF673AB7), fontWeight = FontWeight.Bold)

                    val accessLevel = project.permissions?.projectAccess?.accessLevel
                        ?: project.permissions?.maxAccessLevel
                    if (accessLevel != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = getAccessLevelName(accessLevel).uppercase(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Row {
                    if (project.visibility == "private") {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        Icons.Default.StarOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                project.description ?: "No description.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.Gray
                )
                Text(
                    " ${project.starCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )

                project.lastActivityAt?.let { lastActivity ->
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                    Text(
                        " ${formatTimeAgo(lastActivity)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun MemberItem(member: GroupMember) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = member.avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(member.name, fontWeight = FontWeight.Bold)
            Text(
                "@${member.username}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        Surface(
            color = when (member.accessLevelName) {
                "Owner" -> Color(0xFFFFE0D8)
                "Maintainer" -> Color(0xFFE8EAF6)
                else -> Color(0xFFF5F5F5)
            },
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = member.accessLevelName,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = when (member.accessLevelName) {
                    "Owner" -> Color(0xFFB6230C)
                    "Maintainer" -> Color(0xFF3F51B5)
                    else -> Color.Gray
                }
            )
        }
    }
}

@Composable
fun GroupIssues(issues: List<Issue>, isLoading: Boolean, onIssueClick: (Long, Long) -> Unit) {
    if (isLoading && issues.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(issues) { issue ->
                IssueItem(issue, onClick = { onIssueClick(issue.projectId, issue.iid) })
            }
            if (issues.isEmpty()) {
                item {
                    SectionEmptyText("No issues found")
                }
            }
        }
    }
}

@Composable
fun GroupMergeRequests(
    mergeRequests: List<MergeRequest>,
    isLoading: Boolean,
    onMRClick: (Long, Long) -> Unit
) {
    if (isLoading && mergeRequests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(mergeRequests) { mr ->
                MergeRequestItem(mr, onClick = { onMRClick(mr.projectId, mr.iid) })
            }
            if (mergeRequests.isEmpty()) {
                item {
                    SectionEmptyText("No merge requests found")
                }
            }
        }
    }
}

@Composable
fun GroupSubgroupsTab(
    subgroups: List<Group>,
    onSubgroupClick: (Long) -> Unit,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    hasMore: Boolean
) {
    if (isLoading && subgroups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(subgroups) { subgroup ->
                SubgroupCard(subgroup, onClick = { onSubgroupClick(subgroup.id) })
            }
            if (subgroups.isEmpty()) {
                item {
                    SectionEmptyText("No subgroups found")
                }
            }
            if (hasMore) {
                item {
                    OutlinedButton(
                        onClick = onLoadMore,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Load More Subgroups")
                    }
                }
            }
        }
    }
}

@Composable
fun GroupMembersTab(
    members: List<GroupMember>,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    hasMore: Boolean
) {
    if (isLoading && members.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(members) { member ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.LightGray.copy(alpha = 0.3f)
                    )
                ) {
                    MemberItem(member)
                }
            }
            if (members.isEmpty()) {
                item {
                    SectionEmptyText("No members found")
                }
            }
            if (hasMore) {
                item {
                    OutlinedButton(
                        onClick = onLoadMore,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Load More Members")
                    }
                }
            }
        }
    }
}
