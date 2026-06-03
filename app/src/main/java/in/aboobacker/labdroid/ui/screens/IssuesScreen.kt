package `in`.aboobacker.labdroid.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.aboobacker.labdroid.R
import `in`.aboobacker.labdroid.data.model.Issue
import `in`.aboobacker.labdroid.data.model.Label
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.ui.components.FilterRow
import `in`.aboobacker.labdroid.ui.components.IssueItem
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import `in`.aboobacker.labdroid.ui.viewmodel.IssueUiState
import `in`.aboobacker.labdroid.ui.viewmodel.IssueViewModel

@Composable
fun IssuesScreen(
    viewModel: IssueViewModel,
    onIssueClick: (Long, Long) -> Unit = { _, _ -> },
    onEpicClick: (Long, Long) -> Unit = { _, _ -> },
    onFabClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("Assigned to me") }
    var selectedState by remember { mutableStateOf("Open") }
    var searchQuery by remember { mutableStateOf("") }

    IssuesScreenContent(
        uiState = uiState,
        selectedFilter = selectedFilter,
        selectedState = selectedState,
        searchQuery = searchQuery,
        onSearchChange = {
            searchQuery = it
            viewModel.search(it)
        },
        onIssueClick = onIssueClick,
        onFilterChange = { filter ->
            selectedFilter = filter
            val scope = when (filter) {
                "Assigned to me" -> "assigned_to_me"
                "Created by me" -> "created_by_me"
                else -> "all"
            }
            val state = if (selectedState == "Open") "opened" else "closed"
            viewModel.fetchData(scope = scope, state = state, search = searchQuery.ifBlank { null })
        },
        onStateChange = { state ->
            selectedState = state
            val scope = when (selectedFilter) {
                "Assigned to me" -> "assigned_to_me"
                "Created by me" -> "created_by_me"
                else -> "all"
            }
            viewModel.fetchData(
                scope = scope,
                state = if (state == "Open") "opened" else "closed",
                search = searchQuery.ifBlank { null }
            )
        },
        onRefresh = {
            val scope = when (selectedFilter) {
                "Assigned to me" -> "assigned_to_me"
                "Created by me" -> "created_by_me"
                else -> "all"
            }
            viewModel.fetchData(
                scope = scope,
                state = if (selectedState == "Open") "opened" else "closed",
                search = searchQuery.ifBlank { null }
            )
        }
    )
}

@Composable
fun IssuesScreenContent(
    uiState: IssueUiState,
    selectedFilter: String = "Assigned to me",
    selectedState: String = "Open",
    searchQuery: String = "",
    onSearchChange: (String) -> Unit = {},
    onIssueClick: (Long, Long) -> Unit = { _, _ -> },
    onFilterChange: (String) -> Unit = {},
    onStateChange: (String) -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (val state = uiState) {
            is IssueUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is IssueUiState.Success -> {
                WorkItemsDashboard(
                    issues = state.issues,
                    selectedFilter = selectedFilter,
                    selectedState = selectedState,
                    searchQuery = searchQuery,
                    onSearchChange = onSearchChange,
                    onIssueClick = onIssueClick,
                    onFilterChange = onFilterChange,
                    onStateChange = onStateChange,
                    isLoading = state.isLoading,
                    onRefresh = onRefresh
                )
            }

            is IssueUiState.Error -> {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkItemsDashboard(
    issues: List<Issue>,
    selectedFilter: String,
    selectedState: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onIssueClick: (Long, Long) -> Unit,
    onFilterChange: (String) -> Unit,
    onStateChange: (String) -> Unit,
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.search_issues)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val states = listOf("Open", "Closed")
                        states.forEachIndexed { index, state ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = states.size
                                ),
                                onClick = { onStateChange(state) },
                                selected = selectedState == state,
                                icon = { SegmentedButtonDefaults.Icon(selectedState == state) }
                            ) {
                                Text(state)
                            }
                        }
                    }
                    FilterRow(selectedFilter, onFilterChange)
                }
            }

            if (issues.isEmpty() && !isLoading) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.no_work_items_found),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                items(
                    items = issues,
                    key = { it.id }
                ) { issue ->
                    IssueItem(issue = issue, onClick = { onIssueClick(issue.projectId, issue.iid) })
                }
            }
        }
    }
}


@Composable
fun EpicsContent(
    modifier: Modifier = Modifier,
    epics: List<`in`.aboobacker.labdroid.data.model.Epic>,
    onEpicClick: (Long, Long) -> Unit = { _, _ -> }
) {
    if (epics.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No epics found")
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(epics) { epic ->
                // Basic Epic Item
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEpicClick(0L, epic.iid) }, // Group ID needed
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = epic.title, fontWeight = FontWeight.Bold)
                        Text(text = "Epic &${epic.iid}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private val sampleIssues = listOf(
    Issue(
        id = 1,
        iid = 49210,
        projectId = 101,
        title = "Refactor CI/CD pipeline stage for production deployment",
        state = "opened",
        createdAt = "2023-10-01T10:00:00Z",
        updatedAt = "2023-10-01T10:00:00Z",
        labels = listOf(Label(name = "High Priority"), Label(name = "Backend")),
        author = User(
            id = 1,
            name = "Aboobacker Siddique",
            username = "aboobacker",
            avatarUrl = "https://secure.gravatar.com/avatar/sample",
            webUrl = ""
        ),
        webUrl = ""
    ),
    Issue(
        id = 2,
        iid = 49208,
        projectId = 101,
        title = "Implement dark mode toggle in navigation settings",
        state = "opened",
        createdAt = "2023-10-02T10:00:00Z",
        updatedAt = "2023-10-02T10:00:00Z",
        labels = listOf(
            Label(name = "UI/UX"),
            Label(name = "Enhancement111111111111111111111111111111111111111"),
            Label(name = "22222222")
        ),
        author = User(
            id = 2,
            name = "John Doe",
            username = "johndoe",
            avatarUrl = null,
            webUrl = ""
        ),
        assignees = listOf(
            User(
                id = 3,
                name = "Jane",
                username = "jane",
                avatarUrl = "https://secure.gravatar.com/avatar/sample",
                webUrl = ""
            )
        ),
        webUrl = ""
    ),
    Issue(
        id = 3,
        iid = 49195,
        projectId = 101,
        title = "Fix memory leak in background worker service",
        state = "opened",
        createdAt = "2023-10-03T10:00:00Z",
        updatedAt = "2023-10-03T10:00:00Z",
        labels = listOf(Label(name = "Bug"), Label(name = "Critical")),
        author = User(
            id = 1,
            name = "Aboobacker",
            username = "aboobacker",
            avatarUrl = "https://secure.gravatar.com/avatar/sample",
            webUrl = ""
        ),
        assignees = listOf(
            User(id = 2, name = "User 2", username = "u2", avatarUrl = null, webUrl = ""),
            User(id = 3, name = "User 3", username = "u3", avatarUrl = null, webUrl = ""),
            User(id = 4, name = "User 4", username = "u4", avatarUrl = null, webUrl = "")
        ),
        webUrl = ""
    ),
    Issue(
        id = 4,
        iid = 49182,
        projectId = 101,
        title = "Documentation: API v2 update for authentication modules",
        state = "opened",
        createdAt = "2023-10-04T10:00:00Z",
        updatedAt = "2023-10-04T10:00:00Z",
        labels = listOf(Label(name = "Docs")),
        author = User(
            id = 4,
            name = "Sarah",
            username = "sarah",
            avatarUrl = "https://secure.gravatar.com/avatar/sample",
            webUrl = ""
        ),
        webUrl = ""
    )
)

@Preview(showBackground = true)
@Composable
fun IssuesScreenPreview() {
    val sampleUser = User(
        id = 1,
        name = "Aboobacker Siddique",
        username = "aboobacker",
        avatarUrl = "https://secure.gravatar.com/avatar/sample",
        webUrl = ""
    )
    LabdroidTheme {
        IssuesScreenContent(
            uiState = IssueUiState.Success(
                user = sampleUser,
                issues = sampleIssues
            ),
            selectedFilter = "Open"
        )
    }
}
