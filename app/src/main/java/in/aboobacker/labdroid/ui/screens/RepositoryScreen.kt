package `in`.aboobacker.labdroid.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import `in`.aboobacker.labdroid.data.model.Branch
import `in`.aboobacker.labdroid.data.model.Commit
import `in`.aboobacker.labdroid.data.model.Issue
import `in`.aboobacker.labdroid.data.model.Label
import `in`.aboobacker.labdroid.data.model.MergeRequest
import `in`.aboobacker.labdroid.data.model.Project
import `in`.aboobacker.labdroid.data.model.RepositoryItem
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.ui.components.IssueItem
import `in`.aboobacker.labdroid.ui.components.MarkdownText
import `in`.aboobacker.labdroid.ui.components.MergeRequestItem
import `in`.aboobacker.labdroid.ui.components.StatusBadge
import `in`.aboobacker.labdroid.ui.components.formatTimeAgo
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import `in`.aboobacker.labdroid.ui.viewmodel.RepositoryUiState
import `in`.aboobacker.labdroid.ui.viewmodel.RepositoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryScreen(
    projectId: Long,
    viewModel: RepositoryViewModel,
    onIssueClick: (Long, Long) -> Unit,
    onMRClick: (Long, Long) -> Unit,
    onCommitClick: (Long, String) -> Unit,
    onCommitsListClick: (Long, String) -> Unit,
    onFileClick: (String, String) -> Unit,
    onCreateIssueClick: (Long) -> Unit = {},
    onPipelinesClick: (Long) -> Unit = {},
    onNamespaceClick: (Long, String) -> Unit = { _, _ -> },
    onDuoClick: () -> Unit = {},
    hasDuoAccess: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Files", "Work Items", "Merge Requests")
    var currentPath by remember(projectId) { mutableStateOf<String?>(null) }
    var currentRef by remember(projectId) { mutableStateOf<String?>(null) }

    var showBranchPicker by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        viewModel.loadRepositoryData(projectId)
    }

    LaunchedEffect(uiState) {
        if (uiState is RepositoryUiState.Success) {
            currentRef = (uiState as RepositoryUiState.Success).currentRef
        }
    }

    BackHandler(enabled = currentPath != null) {
        currentPath = currentPath?.substringBeforeLast("/", "")?.ifEmpty { null }
        viewModel.loadRepositoryData(projectId, currentPath, currentRef)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            when (val state = uiState) {
                is RepositoryUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is RepositoryUiState.Success -> {
                    when (selectedTabIndex) {
                        0 -> FilesContent(
                            project = state.project,
                            items = state.items,
                            lastCommit = state.lastCommit,
                            currentPath = currentPath,
                            currentRef = state.currentRef,
                            readmeContent = state.readmeContent,
                            onItemClick = { item ->
                                if (item.type == "tree") {
                                    currentPath = item.path
                                    viewModel.loadRepositoryData(projectId, item.path, currentRef)
                                } else {
                                    onFileClick(item.path, currentRef ?: "HEAD")
                                }
                            },
                            onBranchClick = { showBranchPicker = true },
                            onHistoryClick = { onCommitsListClick(projectId, state.currentRef) },
                            onPipelinesClick = { onPipelinesClick(projectId) },
                            onLastCommitClick = {
                                onCommitClick(
                                    projectId,
                                    state.lastCommit?.id ?: ""
                                )
                            },
                            onNamespaceClick = onNamespaceClick
                        )

                        1 -> WorkItemsContent(
                            issues = state.issues,
                            isLoadingMore = state.isLoadingMoreIssues,
                            isEnd = state.isIssuesEnd,
                            currentSearch = state.issueSearch,
                            currentScope = state.issueScope,
                            currentState = state.issueState,
                            onSearchChange = { viewModel.searchIssues(it) },
                            onFilterChange = { scope, state ->
                                viewModel.filterIssues(
                                    scope,
                                    state
                                )
                            },
                            onIssueClick = onIssueClick,
                            onLoadMore = { viewModel.loadMoreIssues() }
                        )

                        2 -> RepoMergeRequestsContent(
                            mergeRequests = state.mergeRequests,
                            isLoadingMore = state.isLoadingMoreMRs,
                            isEnd = state.isMRsEnd,
                            currentSearch = state.mrSearch,
                            currentScope = state.mrScope,
                            currentState = state.mrState,
                            onSearchChange = { viewModel.searchMRs(it) },
                            onFilterChange = { state, scope -> viewModel.filterMRs(state, scope) },
                            onMRClick = onMRClick,
                            onLoadMore = { viewModel.loadMoreMRs() }
                        )
                    }

                    if (showBranchPicker) {
                        BranchPickerSheet(
                            branches = state.branches,
                            currentBranch = state.currentRef,
                            onBranchSelected = { branch ->
                                currentRef = branch
                                showBranchPicker = false
                                viewModel.loadRepositoryData(projectId, currentPath, branch)
                            },
                            onDismiss = { showBranchPicker = false }
                        )
                    }

                    if (hasDuoAccess) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            if (selectedTabIndex == 1) {
                                FloatingActionButton(
                                    onClick = { onCreateIssueClick(projectId) },
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "New Issue")
                                }
                            }

                            ExtendedFloatingActionButton(
                                onClick = onDuoClick,
                                icon = { Icon(Icons.Default.AutoAwesome, null) },
                                text = { Text("Ask Duo") },
                                containerColor = Color(0xFF8E75C2),
                                contentColor = Color.White
                            )
                        }
                    } else if (selectedTabIndex == 1) {
                        FloatingActionButton(
                            onClick = { onCreateIssueClick(projectId) },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(24.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New Issue")
                        }
                    }
                }

                is RepositoryUiState.Error -> {
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
}

@Composable
fun FilesContent(
    project: Project,
    items: List<RepositoryItem>,
    lastCommit: Commit?,
    currentPath: String?,
    currentRef: String,
    readmeContent: String?,
    onItemClick: (RepositoryItem) -> Unit,
    onBranchClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onPipelinesClick: () -> Unit = {},
    onLastCommitClick: () -> Unit,
    onNamespaceClick: (Long, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "${project.namespace?.name ?: "Namespace"} / ${currentPath?.let { "$it /" } ?: ""}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = project.namespace?.let { ns ->
                        Modifier.clickable { onNamespaceClick(ns.id, ns.kind) }
                    } ?: Modifier
                )
                Text(
                    text = currentPath?.split("/")?.lastOrNull() ?: project.name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        item {
            RepoActionButtons(
                currentRef = currentRef,
                onBranchClick = onBranchClick,
                onHistoryClick = onHistoryClick,
                onPipelinesClick = onPipelinesClick
            )
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "NAME",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "LAST UPDATE",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    items.forEach { item ->
                        RepositoryItemRow(item, onClick = { onItemClick(item) })
                    }
                }
            }
        }

        if (lastCommit != null) {
            item {
                LastCommitCard(lastCommit, onClick = onLastCommitClick)
            }
        }

        if (readmeContent != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.LightGray.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "README.md",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        val repoBaseUrl = remember(project.webUrl, currentRef) {
                            "${project.webUrl?.removeSuffix("/")}/-/raw/$currentRef"
                        }
                        MarkdownText(readmeContent, baseUrl = repoBaseUrl)
                    }
                }
            }
        }
    }
}

@Composable
fun RepoActionButtons(
    currentRef: String,
    onBranchClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onPipelinesClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onBranchClick,
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier
                .height(48.dp)
                .weight(1f)
        ) {
            Icon(
                Icons.Default.AccountTree,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                currentRef,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        OutlinedIconButton(
            onClick = onPipelinesClick,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.RocketLaunch,
                contentDescription = "Pipelines",
                modifier = Modifier.size(22.dp)
            )
        }

        OutlinedIconButton(
            onClick = onHistoryClick,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = "History",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchPickerSheet(
    branches: List<Branch>,
    currentBranch: String,
    onBranchSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    "Switch Branch",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            items(branches) { branch ->
                ListItem(
                    modifier = Modifier.clickable { onBranchSelected(branch.name) },
                    headlineContent = {
                        Text(
                            text = branch.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (branch.name == currentBranch) FontWeight.Bold else FontWeight.Normal,
                            color = if (branch.name == currentBranch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.AccountTree,
                            contentDescription = null,
                            tint = if (branch.name == currentBranch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.6f
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingContent = {
                        if (branch.isDefault) {
                            StatusBadge(
                                text = "default",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            }
        }
    }
}


@Composable
fun RepositoryItemRow(item: RepositoryItem, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        },
        leadingContent = {
            val icon = when (item.type) {
                "tree" -> Icons.Default.Folder
                "blob" -> if (item.name.endsWith(".md")) Icons.AutoMirrored.Filled.MenuBook else Icons.AutoMirrored.Filled.InsertDriveFile
                else -> Icons.AutoMirrored.Filled.InsertDriveFile
            }
            val iconColor =
                if (item.type == "tree") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
fun LastCommitCard(commit: Commit, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Last Commit",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = commit.shortId,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = commit.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${commit.authorName} authored ${formatTimeAgo(commit.authoredDate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkItemsContent(
    issues: List<Issue>,
    isLoadingMore: Boolean,
    isEnd: Boolean,
    currentSearch: String,
    currentScope: String,
    currentState: String,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String?, String?) -> Unit,
    onIssueClick: (Long, Long) -> Unit,
    onLoadMore: () -> Unit
) {
    val filters = listOf("All Items", "Assigned to me", "Created by me")
    val selectedFilter = when (currentScope) {
        "assigned_to_me" -> "Assigned to me"
        "created_by_me" -> "Created by me"
        else -> "All Items"
    }

    val states = listOf("Opened", "Closed")
    val selectedIndex = if (currentState == "opened") 0 else 1

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            states.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = states.size),
                    onClick = { onFilterChange(null, label.lowercase()) },
                    selected = index == selectedIndex,
                    label = { Text(label) }
                )
            }
        }

        OutlinedTextField(
            value = currentSearch,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search Work Items...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            singleLine = true
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter, null) },
                    label = { Text(filter) },
                    shape = CircleShape,
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Active Items (${issues.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Newest",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(issues) { issue ->
                IssueItem(issue, onClick = { onIssueClick(issue.projectId, issue.iid) })

                LaunchedEffect(issue.id) {
                    if (issue == issues.last()) {
                        onLoadMore()
                    }
                }
            }

            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
            } else if (!isEnd && issues.isNotEmpty()) {
                item {
                    Button(
                        onClick = onLoadMore,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Load more issues")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoMergeRequestsContent(
    mergeRequests: List<MergeRequest>,
    isLoadingMore: Boolean,
    isEnd: Boolean,
    currentSearch: String,
    currentScope: String,
    currentState: String,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String?, String?) -> Unit,
    onMRClick: (Long, Long) -> Unit,
    onLoadMore: () -> Unit
) {
    val filters = listOf("All Items", "Assigned to me", "Created by me")
    val selectedFilter = when (currentScope) {
        "assigned_to_me" -> "Assigned to me"
        "created_by_me" -> "Created by me"
        else -> "All Items"
    }

    val states = listOf("Opened", "Closed")
    val selectedIndex = if (currentState == "opened") 0 else 1

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            states.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = states.size),
                    onClick = { onFilterChange(label.lowercase(), null) },
                    selected = index == selectedIndex,
                    label = { Text(label) }
                )
            }
        }

        // Search bar
        OutlinedTextField(
            value = currentSearch,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search or filter MRs...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            singleLine = true
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(null, filter) },
                    label = { Text(filter) },
                    shape = CircleShape,
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(mergeRequests) { mr ->
                MergeRequestItem(mr, onClick = { onMRClick(mr.projectId, mr.iid) })
            }

            if (!isEnd && mergeRequests.isNotEmpty()) {
                item {
                    Button(
                        onClick = onLoadMore,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(24.dp),
                        enabled = !isLoadingMore
                    ) {
                        if (isLoadingMore) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Load more merge requests")
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
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
fun RepositoryScreenPreview() {
    LabdroidTheme {
        FilesContent(
            project = Project(
                id = 1,
                name = "mobile-app",
                nameWithNamespace = "gitlab-org/mobile-app",
                description = "Sample mobile application",
                webUrl = "",
                starCount = 10,
                forksCount = 5,
                lastActivityAt = "2023-10-24T14:54:00Z",
                defaultBranch = "main"
            ),
            items = listOf(
                RepositoryItem(id = "1", name = "src", type = "tree", path = "src", mode = ""),
                RepositoryItem(id = "2", name = "res", type = "tree", path = "res", mode = ""),
                RepositoryItem(
                    id = "3",
                    name = "README.md",
                    type = "blob",
                    path = "README.md",
                    mode = ""
                ),
                RepositoryItem(
                    id = "4",
                    name = "build.gradle.kts",
                    type = "blob",
                    path = "build.gradle.kts",
                    mode = ""
                )
            ),
            lastCommit = Commit(
                id = "ae982bc47d12f309a88c2190",
                shortId = "ae982bc4",
                title = "feat: implement biometric authentication",
                authorName = "Alex Thompson",
                authorEmail = "",
                authoredDate = "2023-10-24T14:54:00Z",
                committerName = "",
                committerEmail = "",
                committedDate = ""
            ),
            currentPath = null,
            currentRef = "main",
            readmeContent = "# mobile-app\n\nThis is a sample mobile application project. Use the files list above to explore the source code.",
            onItemClick = {},
            onBranchClick = {},
            onHistoryClick = {},
            onLastCommitClick = {},
            onNamespaceClick = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WorkItemsContentPreview() {
    LabdroidTheme {
        WorkItemsContent(
            issues = listOf(
                Issue(
                    id = 1,
                    iid = 492,
                    projectId = 1,
                    title = "Implement robust retry logic for API gateway authentication failures",
                    state = "opened",
                    createdAt = "2023-10-24T14:54:00Z",
                    updatedAt = "2023-10-24T14:54:00Z",
                    labels = listOf(
                        Label(name = "severity::1"),
                        Label(name = "feature::auth"),
                        Label(name = "v14.2")
                    ),
                    userNotesCount = 12,
                    webUrl = ""
                ),
                Issue(
                    id = 2,
                    iid = 1044,
                    projectId = 1,
                    title = "Memory leak detected in k8s operator during namespace pruning",
                    state = "opened",
                    createdAt = "2023-10-22T14:54:00Z",
                    updatedAt = "2023-10-22T14:54:00Z",
                    labels = listOf(
                        Label(name = "priority::high"),
                        Label(name = "type::bug"),
                        Label(name = "Critical")
                    ),
                    userNotesCount = 3,
                    webUrl = ""
                )
            ),
            isLoadingMore = false,
            isEnd = true,
            currentSearch = "",
            currentScope = "all",
            currentState = "opened",
            onSearchChange = {},
            onFilterChange = { _, _ -> },
            onIssueClick = { _, _ -> },
            onLoadMore = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RepoMergeRequestsContentPreview() {
    LabdroidTheme {
        RepoMergeRequestsContent(
            mergeRequests = listOf(
                MergeRequest(
                    id = 1,
                    iid = 45920,
                    projectId = 1,
                    title = "Add CI/CD visual indicators for pipeline status in mobile view",
                    state = "opened",
                    createdAt = "2023-10-24T14:54:00Z",
                    updatedAt = "2023-10-24T14:54:00Z",
                    author = User(
                        id = 1,
                        name = "Alec",
                        username = "alec_engineer",
                        avatarUrl = null,
                        webUrl = ""
                    ),
                    sourceBranch = "feature/mobile-ui",
                    targetBranch = "master",
                    userNotesCount = 8,
                    webUrl = ""
                ),
                MergeRequest(
                    id = 2,
                    iid = 45918,
                    projectId = 1,
                    title = "Fix: Memory leak in worker threads for heavy repository parsing",
                    state = "opened",
                    createdAt = "2023-10-23T14:54:00Z",
                    updatedAt = "2023-10-23T14:54:00Z",
                    author = User(
                        id = 2,
                        name = "Max",
                        username = "dev_ops_max",
                        avatarUrl = null,
                        webUrl = ""
                    ),
                    sourceBranch = "bug/memory-leak",
                    targetBranch = "master",
                    userNotesCount = 3,
                    webUrl = ""
                )
            ),
            isLoadingMore = false,
            isEnd = true,
            currentSearch = "",
            currentScope = "all",
            currentState = "opened",
            onSearchChange = {},
            onFilterChange = { _, _ -> },
            onMRClick = { _, _ -> },
            onLoadMore = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BranchPickerSheetPreview() {
    LabdroidTheme {
        BranchPickerSheet(
            branches = listOf(
                Branch(
                    name = "main",
                    merged = false,
                    isProtected = true,
                    isDefault = true,
                    webUrl = ""
                ),
                Branch(
                    name = "develop",
                    merged = false,
                    isProtected = false,
                    isDefault = false,
                    webUrl = ""
                ),
                Branch(
                    name = "feature/auth",
                    merged = false,
                    isProtected = false,
                    isDefault = false,
                    webUrl = ""
                )
            ),
            currentBranch = "main",
            onBranchSelected = {},
            onDismiss = {}
        )
    }
}
