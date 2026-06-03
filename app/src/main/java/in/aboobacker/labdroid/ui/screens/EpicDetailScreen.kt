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
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import `in`.aboobacker.labdroid.data.model.Discussion
import `in`.aboobacker.labdroid.data.model.Epic
import `in`.aboobacker.labdroid.data.model.Issue
import `in`.aboobacker.labdroid.ui.components.DiscussionItem
import `in`.aboobacker.labdroid.ui.components.ExpressiveDetailTopBar
import `in`.aboobacker.labdroid.ui.components.LocalTopBarActions
import `in`.aboobacker.labdroid.ui.components.MarkdownText
import `in`.aboobacker.labdroid.ui.components.StatusBadge
import `in`.aboobacker.labdroid.ui.components.formatTimeAgo
import `in`.aboobacker.labdroid.ui.viewmodel.EpicDetailUiState
import `in`.aboobacker.labdroid.ui.viewmodel.EpicDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpicDetailScreen(
    groupId: Long,
    epicIid: Long,
    viewModel: EpicDetailViewModel,
    onIssueClick: (Long, Long) -> Unit = { _, _ -> },
    onUserClick: (String) -> Unit = {},
    onEditClick: (Long, Long) -> Unit = { _, _ -> },
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
        if (state is EpicDetailUiState.Success) {
            val epic = state.epic
            topBarActions.topBar = {
                var showMenu by remember { mutableStateOf(false) }

                ExpressiveDetailTopBar(
                    title = epic.title,
                    onBackClick = onBackClick,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        IconButton(onClick = { onEditClick(groupId, epicIid) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }

                        IconButton(onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, epic.webUrl)
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
                                            Intent(Intent.ACTION_VIEW, Uri.parse(epic.webUrl))
                                        context.startActivity(intent)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Copy Link") },
                                    onClick = {
                                        showMenu = false
                                        clipboardManager.setText(AnnotatedString(epic.webUrl))
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    LaunchedEffect(groupId, epicIid) {
        viewModel.loadEpicDetail(groupId, epicIid)
    }

    val onReferenceClick: (String) -> Unit = { ref ->
        when {
            ref.startsWith("!") -> {
                // Without project context, we can't navigate to MR/Issue directly by IID
            }

            ref.startsWith("#") -> {
                // Same
            }

            ref.startsWith("@") -> {
                onUserClick(ref.drop(1))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .background(Color(0xFFF8F9FA))
    ) {
        when (val state = uiState) {
            is EpicDetailUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is EpicDetailUiState.Success -> {
                EpicDetailContent(
                    epic = state.epic,
                    issues = state.issues,
                    discussions = state.discussions,
                    onIssueClick = onIssueClick,
                    onEditClick = { onEditClick(groupId, epicIid) },
                    onReferenceClick = onReferenceClick
                )
            }

            is EpicDetailUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
        }

        FloatingActionButton(
            onClick = {},
            containerColor = Color(0xFF673AB7),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }
}

@Composable
fun EpicDetailContent(
    epic: Epic,
    issues: List<Issue>,
    discussions: List<Discussion> = emptyList(),
    onIssueClick: (Long, Long) -> Unit = { _, _ -> },
    onEditClick: () -> Unit = {},
    onReferenceClick: (String) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EpicHeader(epic)
        }

        item {
            OverallProgressCard(issues)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Description",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            epic.description?.let {
                val epicBaseUrl = remember(epic.webUrl) {
                    epic.webUrl.substringBefore("/-/epics")
                }
                MarkdownText(it, baseUrl = epicBaseUrl, onReferenceClick = onReferenceClick)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoCard(
                    label = "ASSIGNEE",
                    value = "Cloud Platform Team",
                    icon = Icons.Default.People,
                    modifier = Modifier.weight(1f)
                )
                InfoCard(
                    label = "LABELS",
                    value = "",
                    icon = Icons.AutoMirrored.Filled.Label,
                    modifier = Modifier.weight(1f),
                    customContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF5233))
                            )
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFB6230C))
                            )
                        }
                    }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Issues (${issues.size} open)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = {}) {
                    Text("View All", color = Color(0xFF673AB7))
                }
            }
        }

        items(issues) { issue ->
            EpicIssueItem(issue, onClick = { onIssueClick(issue.projectId, issue.iid) })
        }

        if (discussions.isNotEmpty()) {
            item {
                Text(
                    text = "Discussions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            items(discussions) { discussion ->
                DiscussionItem(
                    discussion = discussion,
                    onReferenceClick = onReferenceClick
                )
            }
        }

        item {
            Text(
                text = "System Architecture",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(12.dp))
            AsyncImage(
                model = "https://images.unsplash.com/photo-1558494949-ef010cbdcc48?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80",
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun EpicHeader(epic: Epic) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusBadge(
                epic.state.uppercase(),
                Color(0xFF673AB7).copy(alpha = 0.1f),
                Color(0xFF673AB7)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("•", color = Color.Gray)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "EPIC #${epic.iid}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateBadge(epic.startDate ?: "Oct 12, 2023", Icons.Default.CalendarToday)
            DateBadge(epic.endDate ?: "Dec 20, 2023", Icons.Default.CalendarToday)
        }
    }
}

@Composable
fun DateBadge(date: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        color = Color(0xFFF5F3F3),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = date, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun OverallProgressCard(issues: List<Issue>) {
    val total = issues.size
    val closed = issues.count { it.state == "closed" }
    val progress = if (total > 0) closed.toFloat() / total else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Overall Progress",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF673AB7)
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "$closed of $total Issues Closed",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF673AB7),
                trackColor = Color(0xFFF1F0F7)
            )
        }
    }
}

@Composable
fun InfoCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    customContent: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFFB6230C),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            if (customContent != null) {
                customContent()
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EpicIssueItem(issue: Issue, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (issue.state == "opened") Icons.Default.ErrorOutline else Icons.Default.CheckCircleOutline,
                contentDescription = null,
                tint = if (issue.state == "opened") Color(0xFFB6230C) else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = issue.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "#${issue.iid} • Updated ${formatTimeAgo(issue.updatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            issue.assignees.firstOrNull()?.let {
                AsyncImage(
                    model = it.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}
