package `in`.aboobacker.labdroid.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.aboobacker.labdroid.data.model.Change
import `in`.aboobacker.labdroid.data.model.Discussion
import `in`.aboobacker.labdroid.data.model.MergeRequest
import `in`.aboobacker.labdroid.data.model.Note
import `in`.aboobacker.labdroid.data.model.Pipeline
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.ui.components.CommentInput
import `in`.aboobacker.labdroid.ui.components.CommitItem
import `in`.aboobacker.labdroid.ui.components.DiscussionItem
import `in`.aboobacker.labdroid.ui.components.ExpressiveDetailTopBar
import `in`.aboobacker.labdroid.ui.components.LocalMarkdownBaseUrl
import `in`.aboobacker.labdroid.ui.components.LocalTopBarActions
import `in`.aboobacker.labdroid.ui.components.MarkdownText
import `in`.aboobacker.labdroid.ui.components.PipelineItem
import `in`.aboobacker.labdroid.ui.components.StatusBadge
import `in`.aboobacker.labdroid.ui.components.formatCommitDate
import `in`.aboobacker.labdroid.ui.components.formatTimeAgo
import `in`.aboobacker.labdroid.ui.components.isColorDark
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import `in`.aboobacker.labdroid.ui.viewmodel.MergeRequestUiState
import `in`.aboobacker.labdroid.ui.viewmodel.MergeRequestViewModel
import `in`.aboobacker.labdroid.util.FileUtils
import `in`.aboobacker.labdroid.util.GitLabLink
import `in`.aboobacker.labdroid.util.LinkUtils
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun MergeRequestScreen(
    viewModel: MergeRequestViewModel,
    projectId: Long,
    iid: Long,
    onPipelineClick: (Long, Long) -> Unit = { _, _ -> },
    onProfileClick: (Long) -> Unit = {},
    onIssueClick: (Long, Long) -> Unit = { _, _ -> },
    onMRClick: (Long, Long) -> Unit = { _, _ -> },
    onUserClick: (String) -> Unit = {},
    onEditClick: (Long, Long) -> Unit = { _, _ -> },
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
        if (state is MergeRequestUiState.Success) {
            val mr = state.mergeRequest
            val currentUser = state.currentUser
            val isAuthor = currentUser != null && (mr.author?.id == currentUser.id)
            val canEdit = isAuthor
            val canMerge = mr.state == "opened"

            topBarActions.topBar = {
                var showMenu by remember { mutableStateOf(false) }

                ExpressiveDetailTopBar(
                    title = mr.title,
                    onBackClick = onBackClick,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        if (canMerge) {
                            IconButton(onClick = { viewModel.mergeMergeRequest(projectId, iid) }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.MergeType,
                                    contentDescription = "Merge",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (canEdit) {
                            IconButton(onClick = { onEditClick(projectId, iid) }) {
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
                                putExtra(Intent.EXTRA_TEXT, mr.webUrl)
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
                                if (canEdit && mr.state == "opened") {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Close Merge Request",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            viewModel.closeMergeRequest(projectId, iid)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Open in Browser") },
                                    onClick = {
                                        showMenu = false
                                        val intent =
                                            Intent(Intent.ACTION_VIEW, Uri.parse(mr.webUrl))
                                        context.startActivity(intent)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Copy Link") },
                                    onClick = {
                                        showMenu = false
                                        clipboardManager.setText(AnnotatedString(mr.webUrl))
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    LaunchedEffect(projectId, iid) {
        viewModel.fetchMergeRequest(projectId, iid)
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
                link.iid.let { onIssueClick(link.projectId ?: projectId, it) }
            }

            is GitLabLink.MergeRequest -> {
                link.iid.let { onMRClick(link.projectId ?: projectId, it) }
            }

            is GitLabLink.User -> {
                onUserClick(link.username)
            }

            is GitLabLink.External -> {
                // Fallback
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
            .imePadding()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            MergeRequestScreenContent(
                projectId = projectId,
                uiState = uiState,
                onPipelineClick = onPipelineClick,
                onProfileClick = onProfileClick,
                onReplyClick = { discussionId, author ->
                    replyingToDiscussionId = discussionId
                    replyingToAuthor = author
                },
                onLoadMoreDiscussions = { viewModel.loadMoreDiscussions(projectId, iid) },
                onReferenceClick = onReferenceClick,
                onEditClick = { onEditClick(projectId, iid) },
                onDuoClick = onDuoClick,
                hasDuoAccess = hasDuoAccess
            )

            if (uiState is MergeRequestUiState.Success && hasDuoAccess) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ExtendedFloatingActionButton(
                        onClick = onDuoClick,
                        icon = { Icon(Icons.Default.AutoAwesome, null) },
                        text = { Text("Ask Duo") },
                        containerColor = Color(0xFF8E75C2),
                        contentColor = Color.White
                    )
                }
            }
        }

        if (uiState is MergeRequestUiState.Success) {
            CommentInput(
                replyingTo = replyingToAuthor,
                onCancelReply = {
                    replyingToDiscussionId = null
                    replyingToAuthor = null
                },
                onSendClick = { body ->
                    viewModel.postComment(projectId, iid, body, replyingToDiscussionId)
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

@Composable
fun MergeRequestScreenContent(
    projectId: Long,
    uiState: MergeRequestUiState,
    onPipelineClick: (Long, Long) -> Unit = { _, _ -> },
    onProfileClick: (Long) -> Unit = {},
    onReplyClick: (String, String) -> Unit = { _, _ -> },
    onLoadMoreDiscussions: () -> Unit = {},
    onReferenceClick: (String) -> Unit = {},
    onEditClick: () -> Unit = {},
    onDuoClick: () -> Unit = {},
    hasDuoAccess: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (uiState) {
                is MergeRequestUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is MergeRequestUiState.Success -> {
                    MergeRequestContent(
                        projectId = projectId,
                        state = uiState,
                        onPipelineClick = onPipelineClick,
                        onProfileClick = onProfileClick,
                        onReplyClick = onReplyClick,
                        onLoadMoreDiscussions = onLoadMoreDiscussions,
                        onReferenceClick = onReferenceClick,
                        onEditClick = onEditClick,
                        onDuoClick = onDuoClick,
                        hasDuoAccess = hasDuoAccess
                    )
                }

                is MergeRequestUiState.Error -> {
                    Text(
                        text = uiState.message,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun MergeRequestContent(
    projectId: Long,
    state: MergeRequestUiState.Success,
    onPipelineClick: (Long, Long) -> Unit,
    onProfileClick: (Long) -> Unit,
    onReplyClick: (String, String) -> Unit,
    onLoadMoreDiscussions: () -> Unit = {},
    onReferenceClick: (String) -> Unit = {},
    onEditClick: () -> Unit = {},
    onDuoClick: () -> Unit = {},
    hasDuoAccess: Boolean = false
) {
    val mr = state.mergeRequest
    var selectedTab by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = if (hasDuoAccess) 120.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (containerColor, contentColor) = if (mr.state == "opened") {
                            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        StatusBadge(
                            text = mr.state.uppercase(),
                            containerColor = containerColor,
                            contentColor = contentColor,
                            icon = if (mr.state == "opened") Icons.Default.Check else Icons.Default.Close
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "!${mr.iid}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (mr.draft) "Draft by " else "By ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = mr.author?.name ?: "Unknown",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .semantics {
                                contentDescription = "Author: ${mr.author?.name ?: "Unknown"}"
                            }
                            .clickable { mr.author?.id?.let { onProfileClick(it) } }
                    )
                    Text(
                        text = " • ${formatTimeAgo(mr.createdAt)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (mr.labels.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mr.labels.forEach { label ->
                            val color = try {
                                Color(label.color.let { if (it.startsWith("#")) it else "#$it" }
                                    .toColorInt())
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primaryContainer
                            }

                            val textColor = try {
                                label.textColor?.let { Color(it.toColorInt()) } ?: if (isColorDark(
                                        color
                                    )
                                ) Color.White else Color.Black
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }

                            StatusBadge(label.name, color, textColor)
                        }
                    }
                }
            }
        }

        item {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Overview") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Commits") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Pipelines") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Changes")
                            val count = state.changes?.changes?.size
                            if (count != null) {
                                Spacer(Modifier.width(4.dp))
                                Badge { Text(text = count.toString()) }
                            }
                        }
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                item {
                    val latestPipeline = state.pipelines.firstOrNull()
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        PipelineStatusCard(
                            pipeline = latestPipeline,
                            isLoading = state.isLoadingPipelines,
                            onClick = { latestPipeline?.let { onPipelineClick(projectId, it.id) } }
                        )
                    }
                }

                if (mr.description != null) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "DESCRIPTION",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val isAuthor =
                                                state.currentUser != null && (state.mergeRequest.author?.id == state.currentUser.id)
                                            if (isAuthor) {
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
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }

                                            if (hasDuoAccess) {
                                                Button(
                                                    onClick = onDuoClick,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFFF1EDFA),
                                                        contentColor = Color(0xFF8E75C2)
                                                    ),
                                                    contentPadding = PaddingValues(
                                                        horizontal = 12.dp,
                                                        vertical = 4.dp
                                                    ),
                                                    modifier = Modifier.height(32.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.AutoAwesome,
                                                        null,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        "Summarize with Duo",
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val mrBaseUrl = remember(mr.webUrl) {
                                        mr.webUrl.substringBefore("/-/merge_requests")
                                    }
                                    CompositionLocalProvider(LocalMarkdownBaseUrl provides mrBaseUrl) {
                                        MarkdownText(
                                            mr.description ?: "",
                                            onReferenceClick = onReferenceClick
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Group notes by discussion
                val sortedDiscussions =
                    state.discussions.sortedByDescending { it.notes.firstOrNull()?.createdAt }

                sortedDiscussions.forEach { discussion ->
                    val firstNote = discussion.notes.firstOrNull()
                    if (firstNote?.system == true) {
                        // System notes
                    } else {
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
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

                if (state.isLoadingDiscussions) {
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

                if (!state.isLoadingDiscussions && state.hasMoreDiscussions) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(
                                onClick = onLoadMoreDiscussions,
                                modifier = Modifier.widthIn(max = 320.dp)
                            ) {
                                Text("Load more discussions")
                            }
                        }
                    }
                }
            }

            1 -> {
                if (state.isLoadingCommits) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (state.commits.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No commits found", color = Color.Gray)
                        }
                    }
                } else {
                    val groupedCommits =
                        state.commits.groupBy { formatCommitDate(it.committedDate) }
                    groupedCommits.forEach { (date, commits) ->
                        item {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(commits) { commit ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                CommitItem(commit)
                            }
                        }
                    }
                }
            }

            2 -> {
                if (state.isLoadingPipelines) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (state.pipelines.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No pipelines found", color = Color.Gray)
                        }
                    }
                } else {
                    items(state.pipelines) { pipeline ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            PipelineItem(
                                pipeline,
                                onClick = { onPipelineClick(projectId, pipeline.id) })
                        }
                    }
                }
            }

            3 -> {
                if (state.isLoadingChanges) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (state.changes == null || state.changes.changes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No changes found", color = Color.Gray)
                        }
                    }
                } else {
                    item {
                        Text(
                            "Changed files",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    state.changes.changes.forEach { change ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Assignment,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = change.newPath,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(state.changes.changes) { change ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            DiffCard(change)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DiffCard(change: Change) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(12.dp)
            ) {
                Text(
                    text = change.newPath,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(modifier = Modifier.padding(8.dp)) {
                val lines = change.diff.split("\n")
                lines.forEach { line ->
                    val backgroundColor = when {
                        line.startsWith("+") -> Color(0xFFE8F5E9).copy(alpha = 0.5f) // Success-ish
                        line.startsWith("-") -> Color(0xFFFFEBEE).copy(alpha = 0.5f) // Error-ish
                        else -> Color.Transparent
                    }
                    val textColor = when {
                        line.startsWith("+") -> Color(0xFF2E7D32)
                        line.startsWith("-") -> Color(0xFFC62828)
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Text(
                        text = line,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(backgroundColor)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun PipelineStatusCard(pipeline: Pipeline?, isLoading: Boolean = false, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Loading pipeline...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val statusColor = when (pipeline?.status) {
                    "success" -> Color(0xFF4CAF50)
                    "running" -> MaterialTheme.colorScheme.primary
                    "failed" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (pipeline?.status == "success") Icons.Default.Check else Icons.Default.Upload,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (pipeline != null) "Pipeline ${pipeline.status.replaceFirstChar { it.uppercase() }}" else "No Pipeline",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (pipeline != null) {
                        Text(
                            "#${pipeline.id} on ${pipeline.ref}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.widthIn(max = 200.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (pipeline != null) {
                    Text(
                        "Details",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun MRCommentPreview() {
    val discussion = Discussion(
        id = "1",
        individualNote = true,
        notes = listOf(
            Note(
                id = 1, body = "hello",
                author = User(
                    name = "John doe",
                    id = 1,
                    username = "john11111111111111111111111",
                    avatarUrl = "",
                    webUrl = "",
                    bio = "",
                    jobTitle = "",
                    followers = 1,
                    following = 2,
                    starCount = 3
                ),
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                system = false,
                noteableId = 1,
                noteableType = "MergeRequest"
            )
        )
    )
    LabdroidTheme {
        DiscussionItem(discussion = discussion)
    }
}

@Preview(showBackground = true)
@Composable
fun PipelineStatusCardPreview() {
    LabdroidTheme {
        PipelineStatusCard(null)
    }
}

@OptIn(ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun MergeRequestScreenPreview() {
    val sampleUser = User(
        id = 1,
        name = "Aboobacker MK",
        username = "aboobacker",
        avatarUrl = null
    )
    val sampleMr = MergeRequest(
        id = 1,
        iid = 42,
        projectId = 1,
        title = "Update README.md with project details",
        description = "This merge request updates the README file with more detailed information about the project structure and setup instructions.",
        state = "opened",
        createdAt = "2024-03-20T10:00:00Z",
        updatedAt = "2024-03-20T10:00:00Z",
        author = sampleUser,
        webUrl = "https://gitlab.com/example/project/-/merge_requests/42",
        sourceBranch = "update-readme",
        targetBranch = "main"
    )
    val sampleDiscussions = listOf(
        Discussion(
            id = "1",
            individualNote = true,
            notes = listOf(
                Note(
                    id = 1,
                    body = "Great changes! LGTM.",
                    author = sampleUser,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                    system = false,
                    noteableId = 1,
                    noteableType = "MergeRequest"
                )
            )
        )
    )
    val sampleChanges = `in`.aboobacker.labdroid.data.model.MergeRequestChanges(
        iid = 42,
        changes = listOf(
            Change(
                oldPath = "src/styles/tokens.json",
                newPath = "src/styles/tokens.json",
                diff = "@@ -42,7 +42,7 @@\n \"colors\": {\n- \"primary\": \"#3F51B5\",\n+ \"primary\": \"#5e3fa8\","
            )
        )
    )
    val sampleCommits = listOf(
        `in`.aboobacker.labdroid.data.model.Commit(
            id = "ae982bc4",
            shortId = "ae982bc4",
            title = "refactor: optimize validation engine",
            authorName = "Alex Rivera",
            authorEmail = "",
            authoredDate = "2023-10-24T14:54:00Z",
            committerName = "",
            committerEmail = "",
            committedDate = "2023-10-24T14:54:00Z"
        )
    )
    val samplePipelines = listOf(
        Pipeline(
            id = 982145,
            projectId = 1,
            status = "running",
            ref = "main/1111111111111111111111",
            sha = "ae982bc4",
            webUrl = "",
            createdAt = "2023-10-24T14:54:00Z",
            updatedAt = "2023-10-24T14:54:00Z",
            duration = 120.0
        )
    )

    LabdroidTheme {
        MergeRequestScreenContent(
            projectId = 1,
            uiState = MergeRequestUiState.Success(
                mergeRequest = sampleMr,
                currentUser = sampleUser,
                discussions = sampleDiscussions,
                changes = sampleChanges,
                commits = sampleCommits,
                pipelines = samplePipelines,
                isLoadingDiscussions = false,
                isLoadingChanges = false,
                isLoadingCommits = false,
                isLoadingPipelines = false,
                hasMoreDiscussions = true
            ),
            onProfileClick = {}
        )
    }
}
