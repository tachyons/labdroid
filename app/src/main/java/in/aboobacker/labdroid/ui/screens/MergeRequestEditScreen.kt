package `in`.aboobacker.labdroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import `in`.aboobacker.labdroid.data.model.Label
import `in`.aboobacker.labdroid.data.model.MergeRequest
import `in`.aboobacker.labdroid.data.model.Milestone
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.ui.components.LocalTopBarActions
import `in`.aboobacker.labdroid.ui.components.MarkdownEditor
import `in`.aboobacker.labdroid.ui.components.SelectorField
import `in`.aboobacker.labdroid.ui.components.StatusBadge
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import `in`.aboobacker.labdroid.ui.viewmodel.MergeRequestUiState
import `in`.aboobacker.labdroid.ui.viewmodel.MergeRequestViewModel

@Composable
fun MergeRequestEditScreen(
    projectId: Long,
    mergeRequest: MergeRequest,
    viewModel: MergeRequestViewModel,
    onBackClick: () -> Unit = {},
    onSaved: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.fetchMetadata(projectId)
    }

    val state = uiState
    val metadata = if (state is MergeRequestUiState.Success) {
        Triple(state.availableMembers, state.availableMilestones, state.availableLabels)
    } else {
        Triple(emptyList(), emptyList(), emptyList())
    }

    MergeRequestEditContent(
        mergeRequest = mergeRequest,
        availableMembers = metadata.first,
        availableMilestones = metadata.second,
        availableLabels = metadata.third,
        onBackClick = onBackClick,
        onSaveClick = { title, description, milestoneId, assigneeId, labels ->
            viewModel.updateMergeRequest(
                projectId,
                mergeRequest.iid,
                title,
                description,
                milestoneId,
                assigneeId,
                labels
            )
            onSaved()
        }
    )
}

@Composable
fun MergeRequestEditContent(
    mergeRequest: MergeRequest,
    availableMembers: List<User> = emptyList(),
    availableMilestones: List<Milestone> = emptyList(),
    availableLabels: List<Label> = emptyList(),
    onBackClick: () -> Unit = {},
    onSaveClick: (String, String, Long?, Long?, String?) -> Unit = { _, _, _, _, _ -> }
) {
    var title by remember { mutableStateOf(mergeRequest.title) }
    var description by remember { mutableStateOf(mergeRequest.description ?: "") }
    var labels by remember { mutableStateOf(mergeRequest.labels) }
    var assignee by remember { mutableStateOf(mergeRequest.assignees.firstOrNull()) }
    var milestone by remember { mutableStateOf(mergeRequest.milestone) }

    var assigneeMenuExpanded by remember { mutableStateOf(false) }
    var milestoneMenuExpanded by remember { mutableStateOf(false) }
    var labelMenuExpanded by remember { mutableStateOf(false) }

    val topBarActions = LocalTopBarActions.current
    LaunchedEffect(mergeRequest, title) {
        topBarActions.topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSaveClick(
                                    title,
                                    description,
                                    milestone?.id,
                                    assignee?.id,
                                    if (labels.isNotEmpty()) labels.joinToString(",") { it.name } else null
                                )
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        enabled = title.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            topBarActions.topBar = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Section
        Column {
            Text("Title", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Description Section
        Column {
            Text("Description", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            MarkdownEditor(
                value = description,
                onValueChange = { description = it },
                placeholder = "Provide context..."
            )
        }

        // Selectors
        Box {
            SelectorField(
                label = "ASSIGNEE",
                value = assignee?.name ?: "Unassigned",
                onClick = { assigneeMenuExpanded = true },
                icon = {
                    if (assignee?.avatarUrl != null) {
                        AsyncImage(
                            model = assignee?.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            )
            DropdownMenu(
                expanded = assigneeMenuExpanded,
                onDismissRequest = { assigneeMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Unassigned") },
                    onClick = {
                        assignee = null
                        assigneeMenuExpanded = false
                    }
                )
                availableMembers.forEach { member ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = member.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(member.name)
                            }
                        },
                        onClick = {
                            assignee = member
                            assigneeMenuExpanded = false
                        }
                    )
                }
            }
        }

        Box {
            SelectorField(
                label = "MILESTONE",
                value = milestone?.title ?: "Select milestone",
                onClick = { milestoneMenuExpanded = true },
                icon = {
                    Icon(
                        Icons.Default.OutlinedFlag,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Gray
                    )
                }
            )
            DropdownMenu(
                expanded = milestoneMenuExpanded,
                onDismissRequest = { milestoneMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("None") },
                    onClick = {
                        milestone = null
                        milestoneMenuExpanded = false
                    }
                )
                availableMilestones.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(m.title) },
                        onClick = {
                            milestone = m
                            milestoneMenuExpanded = false
                        }
                    )
                }
            }
        }

        // Labels Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Color.LightGray.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LABELS", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        "Edit",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { labelMenuExpanded = true }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    labels.forEach { label ->
                        StatusBadge(label.name, Color(0xFFFDE7E7), Color(0xFFB6230C))
                    }
                }
                DropdownMenu(
                    expanded = labelMenuExpanded,
                    onDismissRequest = { labelMenuExpanded = false }
                ) {
                    availableLabels.forEach { label ->
                        val isSelected = labels.any { it.name == label.name }
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Close, // placeholder
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(label.name)
                                }
                            },
                            onClick = {
                                labels = if (isSelected) {
                                    labels.filter { it.name != label.name }
                                } else {
                                    labels + label
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MergeRequestEditScreenPreview() {
    val sampleUser =
        User(id = 1, name = "Aboobacker", username = "aboobacker", avatarUrl = null, webUrl = "")
    val sampleMR = MergeRequest(
        id = 1,
        iid = 42,
        projectId = 101,
        title = "Update README.md with project details",
        state = "opened",
        createdAt = "2023-10-01T10:00:00Z",
        updatedAt = "2023-10-01T10:00:00Z",
        author = sampleUser,
        description = "This MR updates the readme.",
        webUrl = "",
        sourceBranch = "main",
        targetBranch = "prod",
        labels = listOf(Label(name = "Docs"))
    )

    LabdroidTheme {
        MergeRequestEditContent(
            mergeRequest = sampleMR
        )
    }
}
