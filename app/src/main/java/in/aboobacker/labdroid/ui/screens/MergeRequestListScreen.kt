package `in`.aboobacker.labdroid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.aboobacker.labdroid.R
import `in`.aboobacker.labdroid.data.model.MergeRequest
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.ui.components.FilterChip
import `in`.aboobacker.labdroid.ui.components.MergeRequestItem
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import `in`.aboobacker.labdroid.ui.viewmodel.MergeRequestListUiState
import `in`.aboobacker.labdroid.ui.viewmodel.MergeRequestListViewModel

@Composable
fun MergeRequestListScreen(
    viewModel: MergeRequestListViewModel,
    onMRClick: (Long, Long) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("Assigned to me") }
    var selectedState by remember { mutableStateOf("Open") }
    var searchQuery by remember { mutableStateOf("") }

    MergeRequestListScreenContent(
        uiState = uiState,
        selectedFilter = selectedFilter,
        selectedState = selectedState,
        searchQuery = searchQuery,
        onSearchChange = {
            searchQuery = it
            viewModel.search(it)
        },
        onMRClick = onMRClick,
        onFilterChange = { filter ->
            selectedFilter = filter
            val scope = when (filter) {
                "Assigned to me" -> "assigned_to_me"
                "Created by me" -> "created_by_me"
                "Review requests" -> "reviews_for_me"
                else -> "all"
            }
            val state = if (selectedState == "Open") "opened" else "closed"
            viewModel.fetchMergeRequests(
                scope = scope,
                state = state,
                search = searchQuery.ifBlank { null })
        },
        onStateChange = { state ->
            selectedState = state
            val scope = when (selectedFilter) {
                "Assigned to me" -> "assigned_to_me"
                "Created by me" -> "created_by_me"
                "Review requests" -> "reviews_for_me"
                else -> "all"
            }
            viewModel.fetchMergeRequests(
                scope = scope,
                state = if (state == "Open") "opened" else "closed",
                search = searchQuery.ifBlank { null }
            )
        },
        onRefresh = {
            val scope = when (selectedFilter) {
                "Assigned to me" -> "assigned_to_me"
                "Created by me" -> "created_by_me"
                "Review requests" -> "reviews_for_me"
                else -> "all"
            }
            viewModel.fetchMergeRequests(
                scope = scope,
                state = if (selectedState == "Open") "opened" else "closed",
                search = searchQuery.ifBlank { null }
            )
        }
    )
}

@Composable
fun MergeRequestListScreenContent(
    uiState: MergeRequestListUiState,
    selectedFilter: String = "Assigned to me",
    selectedState: String = "Open",
    searchQuery: String = "",
    onSearchChange: (String) -> Unit = {},
    onMRClick: (Long, Long) -> Unit = { _, _ -> },
    onFilterChange: (String) -> Unit = {},
    onStateChange: (String) -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (val state = uiState) {
            is MergeRequestListUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is MergeRequestListUiState.Success -> {
                MergeRequestDashboard(
                    mergeRequests = state.mergeRequests,
                    selectedFilter = selectedFilter,
                    selectedState = selectedState,
                    searchQuery = searchQuery,
                    onSearchChange = onSearchChange,
                    onMRClick = onMRClick,
                    onFilterChange = onFilterChange,
                    onStateChange = onStateChange,
                    isLoading = state.isLoading,
                    onRefresh = onRefresh
                )
            }

            is MergeRequestListUiState.Error -> {
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
fun MergeRequestDashboard(
    mergeRequests: List<MergeRequest>,
    selectedFilter: String,
    selectedState: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onMRClick: (Long, Long) -> Unit,
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
            item(key = "search") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.search_merge_requests)) },
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

            item(key = "filters") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val states = listOf("Open", "Closed")
                        states.forEach { state ->
                            FilterChip(
                                label = state,
                                icon = if (state == "Open") Icons.Default.Adjust else Icons.Default.CheckCircle,
                                selected = selectedState == state,
                                onClick = { onStateChange(state) }
                            )
                        }
                    }
                    MergeRequestFilterRow(selectedFilter, onFilterChange)
                }
            }

            if (mergeRequests.isEmpty() && !isLoading) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.no_merge_requests_found),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                items(
                    items = mergeRequests,
                    key = { it.id }
                ) { mr ->
                    MergeRequestItem(mr = mr, onClick = { onMRClick(mr.projectId, mr.iid) })
                }
            }
        }
    }
}

@Composable
fun MergeRequestFilterRow(
    selectedFilter: String,
    onFilterChange: (String) -> Unit
) {
    val filters = listOf("Assigned to me", "Created by me", "Review requests", "All")
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        items(filters) { filter ->
            FilterChip(
                label = filter,
                icon = Icons.Default.Person,
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MergeRequestListScreenPreview() {
    val sampleUser = User(
        id = 1,
        name = "Aboobacker Siddique",
        username = "aboobacker",
        avatarUrl = null
    )
    val sampleMr = MergeRequest(
        id = 1,
        iid = 42,
        projectId = 1,
        title = "Update README.md with project details",
        description = "Updating readme",
        state = "opened",
        createdAt = "2024-03-20T10:00:00Z",
        updatedAt = "2024-03-20T10:00:00Z",
        author = sampleUser,
        webUrl = "https://gitlab.com",
        sourceBranch = "main",
        targetBranch = "main"
    )

    LabdroidTheme {
        MergeRequestListScreenContent(
            uiState = MergeRequestListUiState.Success(
                user = sampleUser,
                mergeRequests = listOf(sampleMr),
                isLoading = false
            )
        )
    }
}
