package `in`.aboobacker.labdroid.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import `in`.aboobacker.labdroid.data.model.Discussion
import `in`.aboobacker.labdroid.data.model.Issue
import `in`.aboobacker.labdroid.data.model.Note
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.ui.components.CommentInput
import `in`.aboobacker.labdroid.ui.components.DiscussionItem
import `in`.aboobacker.labdroid.ui.components.ExpressiveDetailTopBar
import `in`.aboobacker.labdroid.ui.components.LocalMarkdownBaseUrl
import `in`.aboobacker.labdroid.ui.components.LocalTopBarActions
import `in`.aboobacker.labdroid.ui.components.MarkdownText
import `in`.aboobacker.labdroid.ui.components.StatusBadge
import `in`.aboobacker.labdroid.ui.components.formatInstant
import `in`.aboobacker.labdroid.ui.components.formatTimeAgo
import `in`.aboobacker.labdroid.ui.components.isColorDark
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import `in`.aboobacker.labdroid.ui.viewmodel.IssueDetailUiState
import `in`.aboobacker.labdroid.ui.viewmodel.IssueDetailViewModel
import `in`.aboobacker.labdroid.util.FileUtils
import `in`.aboobacker.labdroid.util.GitLabLink
import `in`.aboobacker.labdroid.util.LinkUtils
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailScreen(
    projectId: Long,
    issueIid: Long,
    viewModel: IssueDetailViewModel,
    onEditClick: (Long, Long) -> Unit = { _, _ -> },
    onProfileClick: (Long) -> Unit = {},
    onIssueClick: (Long, Long) -> Unit = { _, _ -> },
    onMRClick: (Long, Long) -> Unit = { _, _ -> },
    onUserClick: (String) -> Unit = {},
    onDuoClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    hasDuoAccess: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topBarActions = LocalTopBarActions.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(uiState, hasDuoAccess) {
        val state = uiState
        if (state is IssueDetailUiState.Success) {
            val issue = state.issue
            val canEdit = state.canEdit
            topBarActions.topBar = {
                var showMenu by remember { mutableStateOf(false) }

                ExpressiveDetailTopBar(
                    title = issue.title,
                    onBackClick = onBackClick,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        if (canEdit) {
                            IconButton(onClick = {
                                if (issue.state == "opened") {
                                    viewModel.closeIssue(projectId, issueIid)
                                } else {
                                    viewModel.reopenIssue(projectId, issueIid)
                                }
                            }) {
                                Icon(
                                    imageVector = if (issue.state == "opened") Icons.Default.Cancel else Icons.Default.CheckCircle,
                                    contentDescription = if (issue.state == "opened") "Close" else "Reopen",
                                    tint = if (issue.state == "opened") MaterialTheme.colorScheme.error else Color(
                                        0xFF4CAF50
                                    )
                                )
                            }
                            IconButton(onClick = { onEditClick(projectId, issueIid) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }

                        if (hasDuoAccess) {
                            IconButton(onClick = onDuoClick) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = "GitLab Duo",
                                    tint = Color(0xFF8E75C2)
                                )
                            }
                        }

                        IconButton(onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, issue.webUrl)
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
                                            Intent(Intent.ACTION_VIEW, Uri.parse(issue.webUrl))
                                        context.startActivity(intent)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Copy Link") },
                                    onClick = {
                                        showMenu = false
                                        clipboardManager.setText(AnnotatedString(issue.webUrl))
                                    }
                                )
                                if (canEdit) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Delete Issue",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            viewModel.deleteIssue(projectId, issueIid) {
                                                onBackClick()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    LaunchedEffect(projectId, issueIid) {
        viewModel.loadIssueDetail(projectId, issueIid)
    }

    var replyingToDiscussionId by remember { mutableStateOf<String?>(null) }
    var replyingToAuthor by remember { mutableStateOf<String?>(null) }

    val onReferenceClick: (String) -> Unit = { ref ->
        val link = if (ref.startsWith("http")) {
            LinkUtils.parseUrl(ref)
        } else {
            LinkUtils.parseReference(ref, projectId)
        }

        when (link) {
            is GitLabLink.Issue -> {
                onIssueClick(link.projectId ?: projectId, link.iid)
            }

            is GitLabLink.MergeRequest -> {
                onMRClick(link.projectId ?: projectId, link.iid)
            }

            is GitLabLink.User -> {
                onUserClick(link.username)
            }

            is GitLabLink.External -> {
                // Fallback to external browser if needed, but for now we assume it's handled by some browser launcher if we don't have one
            }

            else -> {}
        }
    }

    val isUploadingImage by viewModel.isUploadingImage.collectAsStateWithLifecycle()
    val commentText by viewModel.commentText.collectAsStateWithLifecycle()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                val file = FileUtils.getFileFromUri(context, it)
                if (file != null) {
                    viewModel.uploadImage(projectId, file)
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (val state = uiState) {
                is IssueDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is IssueDetailUiState.Success -> {
                    IssueDetailContent(
                        issue = state.issue,
                        discussions = state.discussions,
                        isLoadingDiscussions = state.isLoadingDiscussions,
                        hasMoreDiscussions = state.hasMoreDiscussions,
                        canEdit = state.canEdit,
                        onEditClick = { onEditClick(projectId, issueIid) },
                        onProfileClick = onProfileClick,
                        onReplyClick = { discussionId, author ->
                            replyingToDiscussionId = discussionId
                            replyingToAuthor = author
                        },
                        onLoadMoreDiscussions = {
                            viewModel.loadMoreDiscussions(
                                projectId,
                                issueIid
                            )
                        },
                        onReferenceClick = onReferenceClick,
                        hasDuoAccess = hasDuoAccess
                    )

                    if (hasDuoAccess) {
                        ExtendedFloatingActionButton(
                            onClick = onDuoClick,
                            icon = { Icon(Icons.Default.AutoAwesome, null) },
                            text = { Text("Ask Duo") },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            containerColor = Color(0xFF8E75C2),
                            contentColor = Color.White
                        )
                    }
                }

                is IssueDetailUiState.Error -> {
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

        if (uiState is IssueDetailUiState.Success) {
            CommentInput(
                replyingTo = replyingToAuthor,
                onCancelReply = {
                    replyingToDiscussionId = null
                    replyingToAuthor = null
                },
                onSendClick = { body ->
                    viewModel.postComment(projectId, issueIid, body, replyingToDiscussionId)
                    replyingToDiscussionId = null
                    replyingToAuthor = null
                },
                onImageUploadClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                isUploading = isUploadingImage,
                text = commentText,
                onTextChange = { viewModel.onCommentTextChange(it) }
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun IssueDetailContent(
    issue: Issue,
    discussions: List<Discussion>,
    isLoadingDiscussions: Boolean = false,
    hasMoreDiscussions: Boolean = false,
    canEdit: Boolean = false,
    onEditClick: () -> Unit = {},
    onProfileClick: (Long) -> Unit = {},
    onReplyClick: (String, String) -> Unit = { _, _ -> },
    onLoadMoreDiscussions: () -> Unit = {},
    onReferenceClick: (String) -> Unit = {},
    hasDuoAccess: Boolean = false
) {
    val issueBaseUrl = remember(issue.webUrl) {
        issue.webUrl.substringBefore("/-/issues")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = if (hasDuoAccess) 80.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            IssueHeader(
                issue = issue,
                onProfileClick = onProfileClick
            )
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                IssueInfoGrid(issue, onProfileClick)
            }
        }

        item {
            CompositionLocalProvider(LocalMarkdownBaseUrl provides issueBaseUrl) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (canEdit) {
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
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    issue.description?.let {
                        MarkdownText(
                            it,
                            onReferenceClick = onReferenceClick
                        )
                    }
                }
            }
        }

        item {
            var isNewestFirst by remember { mutableStateOf(true) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Activity",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = { isNewestFirst = !isNewestFirst }) {
                    Text(
                        text = if (isNewestFirst) "Sort: Newest" else "Sort: Oldest",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Icon(
                        if (isNewestFirst) Icons.Default.ArrowDropDown else Icons.Default.ArrowDropUp,
                        contentDescription = null
                    )
                }
            }

            val sortedDiscussions = if (isNewestFirst) {
                discussions.sortedByDescending { it.notes.firstOrNull()?.createdAt }
            } else {
                discussions.sortedBy { it.notes.firstOrNull()?.createdAt }
            }

            CompositionLocalProvider(LocalMarkdownBaseUrl provides issueBaseUrl) {
                sortedDiscussions.forEach { discussion ->
                    val firstNote = discussion.notes.firstOrNull()
                    if (firstNote?.system == true) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            SystemNoteItem(firstNote, onProfileClick)
                        }
                    } else {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            DiscussionItem(
                                discussion = discussion,
                                onReplyClick = {
                                    onReplyClick(
                                        discussion.id,
                                        firstNote?.author?.name ?: ""
                                    )
                                },
                                onReferenceClick = onReferenceClick
                            )
                        }
                    }
                }
            }
        }

        if (isLoadingDiscussions) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }

        if (!isLoadingDiscussions && hasMoreDiscussions) {
            item {
                TextButton(
                    onClick = onLoadMoreDiscussions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Load more discussions")
                }
            }
        }
    }
}

@Composable
fun IssueHeader(
    issue: Issue,
    onProfileClick: (Long) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (containerColor, contentColor) = if (issue.state == "opened") {
                    MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                }
                StatusBadge(
                    text = issue.state.replaceFirstChar { it.uppercase() },
                    containerColor = containerColor,
                    contentColor = contentColor,
                    icon = if (issue.state == "opened") Icons.Default.CheckCircle else Icons.Default.Cancel
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Created ${formatTimeAgo(issue.createdAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "by",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = issue.author?.name ?: "Unknown",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { issue.author?.id?.let { onProfileClick(it) } }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            issue.labels.forEach { label ->
                val color = try {
                    Color(label.color.let { if (it.startsWith("#")) it else "#$it" }.toColorInt())
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.primaryContainer
                }

                val textColor = try {
                    label.textColor?.let { Color(it.toColorInt()) }
                        ?: if (isColorDark(color)) Color.White else Color.Black
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }

                StatusBadge(label.name, color, textColor)
            }
        }
    }
}

@Composable
fun IssueInfoGrid(issue: Issue, onProfileClick: (Long) -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoItem(
                    label = "ASSIGNEE",
                    value = issue.assignees.firstOrNull()?.name ?: "Unassigned",
                    avatarUrl = issue.assignees.firstOrNull()?.avatarUrl,
                    modifier = Modifier.weight(1f),
                    onValueClick = { issue.assignees.firstOrNull()?.id?.let { onProfileClick(it) } }
                )
                InfoItem(
                    label = "MILESTONE",
                    value = issue.milestone?.title ?: "None",
                    valueColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoItem(
                    label = "WEIGHT",
                    value = issue.weight?.toString() ?: "None",
                    modifier = Modifier.weight(1f)
                )
                InfoItem(
                    label = "CONFIDENTIAL",
                    value = if (issue.confidential) "Yes" else "No",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun InfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    avatarUrl: String? = null,
    onValueClick: () -> Unit = {}
) {
    Column(modifier = modifier.clickable { onValueClick() }) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun SystemNoteItem(note: Note, onProfileClick: (Long) -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Label,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = note.author.name,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable { onProfileClick(note.author.id) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatInstant(note.createdAt.toEpochMilliseconds()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = note.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@OptIn(ExperimentalTime::class)
@Composable
fun IssueDetailScreenPreview() {
    val sampleUser =
        User(id = 1, name = "Aboobacker", username = "aboobacker", avatarUrl = null, webUrl = "")
    val sampleIssue = Issue(
        id = 1,
        iid = 1209,
        projectId = 101,
        title = "Memory leak in worker nodes during spike",
        state = "opened",
        createdAt = "2023-10-01T10:00:00Z",
        updatedAt = "2023-10-01T10:00:00Z",
        author = sampleUser,
        description = "We are seeing high memory usage in production workers when traffic spikes.",
        webUrl = ""
    )
    val sampleDiscussions = listOf(
        Discussion(
            id = "1",
            individualNote = true,
            notes = listOf(
                Note(
                    id = 1,
                    body = "I'll investigate this.",
                    author = sampleUser,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                    system = false,
                    noteableId = 1,
                    noteableType = "Issue"
                )
            )
        )
    )

    LabdroidTheme {
        Box(modifier = Modifier.background(Color(0xFFF8F9FA))) {
            IssueDetailContent(issue = sampleIssue, discussions = sampleDiscussions, canEdit = true)
        }
    }
}
