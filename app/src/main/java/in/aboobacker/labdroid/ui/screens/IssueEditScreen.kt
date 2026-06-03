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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import `in`.aboobacker.labdroid.data.model.Issue
import `in`.aboobacker.labdroid.data.model.IssueRequest
import `in`.aboobacker.labdroid.data.model.Label
import `in`.aboobacker.labdroid.data.model.Milestone
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.ui.components.LocalTopBarActions
import `in`.aboobacker.labdroid.ui.components.MarkdownEditor
import `in`.aboobacker.labdroid.ui.components.SearchableDropdown
import `in`.aboobacker.labdroid.ui.components.SelectorField
import `in`.aboobacker.labdroid.ui.components.StatusBadge
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import `in`.aboobacker.labdroid.ui.viewmodel.IssueDetailUiState
import `in`.aboobacker.labdroid.ui.viewmodel.IssueDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueEditScreen(
    projectId: Long,
    issue: Issue? = null,
    viewModel: IssueDetailViewModel,
    onBackClick: () -> Unit = {},
    onIssueSaved: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.fetchMetadata(projectId)
    }

    val state = uiState
    val metadata = if (state is IssueDetailUiState.Success) {
        Triple(state.availableMembers, state.availableMilestones, state.availableLabels)
    } else {
        Triple(emptyList(), emptyList(), emptyList())
    }

    IssueEditContent(
        issue = issue,
        availableMembers = metadata.first,
        availableMilestones = metadata.second,
        availableLabels = metadata.third,
        onBackClick = onBackClick,
        onSaveClick = { request ->
            if (issue == null) {
                viewModel.createIssue(projectId, request)
            } else {
                viewModel.updateIssue(projectId, issue.iid, request)
            }
            onIssueSaved()
        },
        onDeleteClick = {
            issue?.let {
                viewModel.deleteIssue(projectId, it.iid) {
                    onBackClick()
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueEditContent(
    issue: Issue? = null,
    availableMembers: List<User> = emptyList(),
    availableMilestones: List<Milestone> = emptyList(),
    availableLabels: List<Label> = emptyList(),
    onBackClick: () -> Unit = {},
    onSaveClick: (IssueRequest) -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    var title by remember { mutableStateOf(issue?.title ?: "") }
    var description by remember { mutableStateOf(issue?.description ?: "") }
    var weight by remember { mutableIntStateOf(issue?.weight ?: 0) }
    var confidential by remember { mutableStateOf(issue?.confidential ?: false) }
    var labels by remember { mutableStateOf(issue?.labels ?: emptyList()) }
    var assignee by remember { mutableStateOf(issue?.assignees?.firstOrNull()) }
    var milestone by remember { mutableStateOf(issue?.milestone) }
    var stateEvent by remember { mutableStateOf(issue?.state ?: "opened") }

    var assigneeMenuExpanded by remember { mutableStateOf(false) }
    var milestoneMenuExpanded by remember { mutableStateOf(false) }
    var labelMenuExpanded by remember { mutableStateOf(false) }

    val topBarActions = LocalTopBarActions.current
    LaunchedEffect(issue, title, stateEvent) {
        topBarActions.topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (issue == null) "New Issue" else title,
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
                                val request = IssueRequest(
                                    title = title,
                                    description = description,
                                    weight = weight,
                                    confidential = confidential,
                                    labels = if (labels.isNotEmpty()) labels.joinToString(",") { it.name } else null,
                                    assigneeIds = assignee?.let { listOf(it.id) },
                                    milestoneId = milestone?.id,
                                    stateEvent = if (issue != null) (if (stateEvent == "closed") "close" else "reopen") else null
                                )
                                onSaveClick(request)
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        enabled = title.isNotBlank()
                    ) {
                        Text(if (issue == null) "Create" else "Save")
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
                placeholder = { Text("What needs to be fixed?") },
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
                placeholder = "Provide context, reproduction steps, and expected behavior..."
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
            SearchableDropdown(
                expanded = assigneeMenuExpanded,
                onDismissRequest = { assigneeMenuExpanded = false },
                items = availableMembers,
                itemText = { it.name },
                onItemClick = {
                    assignee = it
                    assigneeMenuExpanded = false
                },
                searchPlaceholder = "Search members...",
                unassignedLabel = "Unassigned",
                onUnassignedClick = {
                    assignee = null
                    assigneeMenuExpanded = false
                },
                itemContent = { member ->
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
                }
            )
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
            SearchableDropdown(
                expanded = milestoneMenuExpanded,
                onDismissRequest = { milestoneMenuExpanded = false },
                items = availableMilestones,
                itemText = { it.title },
                onItemClick = {
                    milestone = it
                    milestoneMenuExpanded = false
                },
                searchPlaceholder = "Search milestones...",
                unassignedLabel = "None",
                onUnassignedClick = {
                    milestone = null
                    milestoneMenuExpanded = false
                }
            )
        }

        // Weight Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Color.LightGray.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Scale,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Weight",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (weight > 0) weight-- },
                        modifier = Modifier
                            .size(32.dp)
                            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Text("-", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = weight.toString(),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(
                        onClick = { weight++ },
                        modifier = Modifier
                            .size(32.dp)
                            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
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
                    if (issue != null) {
                        Text(
                            "Edit",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    labels.forEach { label ->
                        StatusBadge(label.name, Color(0xFFFDE7E7), Color(0xFFB6230C))
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), CircleShape)
                            .clickable { labelMenuExpanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
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
                                                    Icons.Default.Add, // Using Add as a placeholder for checkmark
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

        // Confidential Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Confidential Issue",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "Limit visibility to project members",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Switch(
                checked = confidential,
                onCheckedChange = { confidential = it }
            )
        }

        // Danger Zone (only in Edit mode)
        if (issue != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Danger Zone",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB6230C)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE7E7).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFFB6230C).copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Deleting this work item is permanent and cannot be undone. All comments, history, and attachments will be lost.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDeleteClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB6230C)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Issue")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun IssueEditScreenNewPreview() {
    LabdroidTheme {
        IssueEditContent(
            issue = null
        )
    }
}

@Preview(showBackground = true)
@Composable
fun IssueEditScreenEditPreview() {
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
        webUrl = "",
        labels = listOf(Label(name = "Bug"), Label(name = "Critical")),
        weight = 5,
        confidential = true
    )

    LabdroidTheme {
        IssueEditContent(
            issue = sampleIssue
        )
    }
}
