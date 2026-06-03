package `in`.aboobacker.labdroid.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.aboobacker.labdroid.data.model.SearchResponse
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.ui.components.StatusBadge
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import `in`.aboobacker.labdroid.ui.viewmodel.SearchUiState
import `in`.aboobacker.labdroid.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBackClick: () -> Unit,
    onProjectClick: (Long) -> Unit = {},
    onUserClick: (Long) -> Unit = {},
    onIssueClick: (Long, Long) -> Unit = { _, _ -> },
    onMRClick: (Long, Long) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    SearchScreenContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onSearch = { query, scope -> viewModel.search(query, scope) },
        onItemClick = { item, scope ->
            when (scope.lowercase()) {
                "projects" -> onProjectClick(item.id)
                "users" -> onUserClick(item.id)
                "issues" -> item.projectId?.let { onIssueClick(it, item.iid ?: 0L) }
                "mrs" -> item.projectId?.let { onMRClick(it, item.iid ?: 0L) }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreenContent(
    uiState: SearchUiState,
    onBackClick: () -> Unit,
    onSearch: (String, String) -> Unit = { _, _ -> },
    onItemClick: (SearchResponse, String) -> Unit = { _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Projects", "Issues", "MRs", "Users")
    val focusManager = LocalFocusManager.current
    var active by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = {
                            onSearch(searchQuery, tabs[selectedTabIndex])
                            active = false
                            focusManager.clearFocus()
                        },
                        expanded = active,
                        onExpandedChange = { active = it },
                        placeholder = { Text("Search GitLab...") },
                        leadingIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null)
                            }
                        }
                    )
                },
                expanded = active,
                onExpandedChange = { active = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (active) 0.dp else 16.dp),
            ) {
                // Suggestions could go here
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        Text(
                            "Recent Searches",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    // Sample suggestions
                    items(listOf("labdroid", "compose", "material3")) { suggestion ->
                        TextButton(
                            onClick = {
                                searchQuery = suggestion
                                onSearch(suggestion, tabs[selectedTabIndex])
                                active = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(suggestion)
                            }
                        }
                    }
                }
            }
        }

        if (!active) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            if (searchQuery.isNotBlank()) {
                                onSearch(searchQuery, tabs[index])
                            }
                        },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier.weight(1f)
            ) {
                when (uiState) {
                    is SearchUiState.Idle, is SearchUiState.Success -> {
                        SearchResults(
                            state = uiState as? SearchUiState.Success,
                            scope = tabs[selectedTabIndex],
                            onItemClick = onItemClick
                        )
                    }

                    is SearchUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    is SearchUiState.Error -> {
                        Text(uiState.message, modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResults(
    state: SearchUiState.Success?,
    scope: String,
    onItemClick: (SearchResponse, String) -> Unit
) {
    val results = state?.results as? List<SearchResponse> ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (results.isEmpty() && state != null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No results found",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(results) { item ->
                SearchResultItem(item, onClick = { onItemClick(item, scope) })
            }
        }
    }
}

@Composable
fun SearchResultItem(item: SearchResponse, onClick: () -> Unit) {
    val title = item.title ?: item.name ?: item.username ?: "Unknown"
    val subtitle = if (item.username != null) "@${item.username}" else item.webUrl ?: ""
    val description = item.description ?: ""

    val (icon, iconColor) = when {
        item.username != null -> Icons.Default.Search to Color(0xFF2196F3)
        item.iid != null -> Icons.Default.Adjust to Color(0xFF4CAF50)
        else -> Icons.AutoMirrored.Filled.MergeType to Color(0xFF673AB7)
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
                item.state?.let {
                    StatusBadge(
                        text = it.uppercase(),
                        containerColor = Color.LightGray.copy(alpha = 0.2f),
                        contentColor = Color.Gray
                    )
                }
            }
            if (description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 3
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    User(
        id = 1,
        name = "Aboobacker MK",
        username = "aboobacker",
        avatarUrl = "https://secure.gravatar.com/avatar/sample",
        webUrl = ""
    )
    LabdroidTheme {
        val sampleUser = User(
            id = 1,
            name = "Aboobacker Siddique",
            username = "aboobacker",
            avatarUrl = "https://secure.gravatar.com/avatar/sample",
            webUrl = ""
        )
        SearchScreenContent(
            uiState = SearchUiState.Success(user = sampleUser, results = emptyList()),
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenSuccessPreview() {
    val sampleUser = User(
        id = 1,
        name = "Aboobacker Siddique",
        username = "aboobacker",
        avatarUrl = "https://secure.gravatar.com/avatar/sample",
        webUrl = ""
    )
    LabdroidTheme {
        SearchScreenContent(
            uiState = SearchUiState.Success(user = sampleUser, results = emptyList()),
            onBackClick = {}
        )
    }
}
