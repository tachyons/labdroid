package `in`.aboobacker.labdroid.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.aboobacker.labdroid.data.model.Change
import `in`.aboobacker.labdroid.data.model.Commit
import `in`.aboobacker.labdroid.data.model.CommitStats
import `in`.aboobacker.labdroid.ui.components.CommentInput
import `in`.aboobacker.labdroid.ui.components.DiscussionItem
import `in`.aboobacker.labdroid.ui.components.ExpressiveDetailTopBar
import `in`.aboobacker.labdroid.ui.components.LocalTopBarActions
import `in`.aboobacker.labdroid.ui.components.StatusBadge
import `in`.aboobacker.labdroid.ui.components.formatTimeAgo
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import `in`.aboobacker.labdroid.ui.viewmodel.CommitDetailUiState
import `in`.aboobacker.labdroid.ui.viewmodel.CommitDetailViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CommitDetailScreen(
    viewModel: CommitDetailViewModel,
    projectId: Long,
    sha: String,
    onIssueClick: (Long, Long) -> Unit = { _, _ -> },
    onMRClick: (Long, Long) -> Unit = { _, _ -> },
    onUserClick: (String) -> Unit = {},
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
        if (state is CommitDetailUiState.Success) {
            val commit = state.commit
            topBarActions.topBar = {
                var showMenu by remember { mutableStateOf(false) }

                ExpressiveDetailTopBar(
                    title = commit.title,
                    onBackClick = onBackClick,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        IconButton(onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Commit ${commit.shortId}: ${commit.title}"
                                )
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
                                    text = { Text("Copy SHA") },
                                    onClick = {
                                        showMenu = false
                                        clipboardManager.setText(AnnotatedString(commit.id))
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    LaunchedEffect(projectId, sha) {
        viewModel.fetchCommitDetails(projectId, sha)
    }

    var replyingToDiscussionId by remember { mutableStateOf<String?>(null) }
    var replyingToAuthor by remember { mutableStateOf<String?>(null) }

    val onReferenceClick: (String) -> Unit = { ref ->
        when {
            ref.startsWith("!") -> {
                ref.drop(1).toLongOrNull()?.let { onMRClick(projectId, it) }
            }

            ref.startsWith("#") -> {
                ref.drop(1).toLongOrNull()?.let { onIssueClick(projectId, it) }
            }

            ref.startsWith("@") -> {
                onUserClick(ref.drop(1))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (val state = uiState) {
                is CommitDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is CommitDetailUiState.Success -> {
                    CommitDetailContent(
                        state = state,
                        onReplyClick = { discussionId, author ->
                            replyingToDiscussionId = discussionId
                            replyingToAuthor = author
                        },
                        onReferenceClick = onReferenceClick
                    )
                }

                is CommitDetailUiState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (uiState is CommitDetailUiState.Success) {
            CommentInput(
                replyingTo = replyingToAuthor,
                onCancelReply = {
                    replyingToDiscussionId = null
                    replyingToAuthor = null
                },
                onSendClick = { body ->
                    viewModel.postComment(projectId, sha, body, replyingToDiscussionId)
                    replyingToDiscussionId = null
                    replyingToAuthor = null
                }
            )
        }
    }
}

@Composable
fun CommitDetailContent(
    state: CommitDetailUiState.Success,
    onReplyClick: (String, String) -> Unit = { _, _ -> },
    onReferenceClick: (String) -> Unit = {}
) {
    val commit = state.commit
    val diffs = state.diffs
    val discussions = state.discussions

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = commit.authorName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = commit.authorName,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { /* Commit author ID not available in simple Commit model */ }
                            )
                            Text(
                                text = formatTimeAgo(commit.committedDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        StatusBadge(
                            text = "PASSED",
                            containerColor = Color(0xFFE8F5E9),
                            contentColor = Color(0xFF2E7D32)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = commit.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )

                    if (commit.message != null && commit.message != commit.title) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = commit.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Description,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = commit.id,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                color = Color.Gray,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "FILES",
                    value = diffs.size.toString(),
                    icon = Icons.Outlined.Description,
                    color = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "ADDITIONS",
                    value = "+${commit.stats?.additions ?: 0}",
                    icon = Icons.Default.Add,
                    color = Color(0xFF2E7D32)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "DELETIONS",
                    value = "-${commit.stats?.deletions ?: 0}",
                    icon = Icons.Default.Remove,
                    color = Color(0xFFC62828)
                )
            }
        }

        if (discussions.isNotEmpty()) {
            item {
                Text(
                    text = "Discussions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(discussions) { discussion ->
                DiscussionItem(
                    discussion = discussion,
                    onReplyClick = { onReplyClick(discussion.id, it) },
                    onReferenceClick = onReferenceClick
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
                    text = "Changed Files",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "View all",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        items(diffs) { diff ->
            DiffCard(diff)
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.3f
            )
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = color)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (value.startsWith("+") || value.startsWith("-")) color else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CommitDetailScreenPreview() {
    LabdroidTheme {
        CommitDetailContent(
            state = CommitDetailUiState.Success(
                commit = Commit(
                    id = "ae982bc47d12f309a88c2190",
                    shortId = "ae982bc4",
                    title = "feat: implement biometric authentication for mobile vault",
                    message = "This commit adds biometric authentication support using the system BiometricPrompt API.",
                    authorName = "Alex Thompson",
                    authorEmail = "alex@example.com",
                    authoredDate = "2023-10-24T14:54:00Z",
                    committerName = "Alex Thompson",
                    committerEmail = "alex@example.com",
                    committedDate = "2023-10-24T14:54:00Z",
                    stats = CommitStats(additions = 45, deletions = 12, total = 57)
                ),
                diffs = listOf(
                    Change(
                        oldPath = "src/auth/BiometricService.kt",
                        newPath = "src/auth/BiometricService.kt",
                        diff = "--- a/src/auth/BiometricService.kt\n+++ b/src/auth/BiometricService.kt\n@@ -42,7 +42,7 @@\n     fun authenticate() {\n-        // legacy pin check\n+        val promptInfo = BiometricPrompt..."
                    ),
                    Change(
                        oldPath = "res/layout/activity_login.xml",
                        newPath = "res/layout/activity_login.xml",
                        diff = "--- a/res/layout/activity_login.xml\n+++ b/res/layout/activity_login.xml\n@@ -10,6 +10,12 @@"
                    )
                )
            )
        )
    }
}

