package `in`.aboobacker.labdroid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import `in`.aboobacker.labdroid.R
import `in`.aboobacker.labdroid.data.model.Issue
import `in`.aboobacker.labdroid.ui.components.FilterRow
import `in`.aboobacker.labdroid.ui.components.IssueItem
import `in`.aboobacker.labdroid.ui.viewmodel.IssueViewModel

@Composable
fun IssuesScreen(
    viewModel: IssueViewModel,
    onIssueClick: (Long, Long) -> Unit = { _, _ -> },
    onEpicClick: (Long, Long) -> Unit = { _, _ -> },
    onFabClick: () -> Unit = {}
) {
    val issues = viewModel.issues.collectAsLazyPagingItems()
    var selectedFilter by remember { mutableStateOf("Assigned to me") }
    var selectedState by remember { mutableStateOf("Open") }
    var searchQuery by remember { mutableStateOf("") }

    IssuesScreenContent(
        issues = issues,
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
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssuesScreenContent(
    issues: LazyPagingItems<Issue>,
    selectedFilter: String = "Assigned to me",
    selectedState: String = "Open",
    searchQuery: String = "",
    onSearchChange: (String) -> Unit = {},
    onIssueClick: (Long, Long) -> Unit = { _, _ -> },
    onFilterChange: (String) -> Unit = {},
    onStateChange: (String) -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        PullToRefreshBox(
            isRefreshing = issues.loadState.refresh is LoadState.Loading && issues.itemCount > 0,
            onRefresh = { issues.refresh() },
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

                when {
                    issues.loadState.refresh is LoadState.Loading && issues.itemCount == 0 -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    issues.loadState.refresh is LoadState.Error && issues.itemCount == 0 -> {
                        item {
                            val error = issues.loadState.refresh as LoadState.Error
                            Text(
                                text = error.error.message ?: "Unknown error",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    issues.itemCount == 0 && issues.loadState.refresh is LoadState.NotLoading -> {
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
                    }

                    else -> {
                        items(
                            count = issues.itemCount,
                            key = issues.itemKey { it.id }
                        ) { index ->
                            val issue = issues[index]
                            if (issue != null) {
                                IssueItem(
                                    issue = issue,
                                    onClick = { onIssueClick(issue.projectId, issue.iid) })
                            }
                        }

                        if (issues.loadState.append is LoadState.Loading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
