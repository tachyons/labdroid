package `in`.aboobacker.labdroid.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.aboobacker.labdroid.data.model.Project
import `in`.aboobacker.labdroid.data.model.Todo
import `in`.aboobacker.labdroid.data.model.TodoTarget
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.ui.components.SkeletonLoader
import `in`.aboobacker.labdroid.ui.components.StatusBadge
import `in`.aboobacker.labdroid.ui.components.formatTimeAgo
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import `in`.aboobacker.labdroid.ui.viewmodel.TodoUiState
import `in`.aboobacker.labdroid.ui.viewmodel.TodoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodosScreen(
    viewModel: TodoViewModel,
    onTodoClick: (Long, Long, String) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    TodosScreenContent(
        uiState = uiState,
        onTodoClick = onTodoClick,
        onFilterSelected = { viewModel.fetchTodos(it) },
        onMarkAsDone = { viewModel.markAsDone(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodosScreenContent(
    uiState: TodoUiState,
    onTodoClick: (Long, Long, String) -> Unit = { _, _, _ -> },
    onFilterSelected: (String) -> Unit = {},
    onMarkAsDone: (Long) -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (uiState) {
            is TodoUiState.Loading -> {
                TodosLoadingSkeleton()
            }

            is TodoUiState.Success -> {
                TodosContent(
                    todos = uiState.todos,
                    isRefreshing = uiState.isRefreshing,
                    onTodoClick = onTodoClick,
                    onFilterSelected = onFilterSelected,
                    onMarkAsDone = onMarkAsDone
                )
            }

            is TodoUiState.Error -> {
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodosContent(
    modifier: Modifier = Modifier,
    todos: List<Todo>,
    isRefreshing: Boolean = false,
    onTodoClick: (Long, Long, String) -> Unit,
    onFilterSelected: (String) -> Unit,
    onMarkAsDone: (Long) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Assigned", "Mentions", "Review Requests")

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { onFilterSelected(selectedFilter) },
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filters) { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            onClick = {
                                selectedFilter = filter
                                onFilterSelected(filter)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFFFF5233) else MaterialTheme.colorScheme.surface,
                            border = if (isSelected) null else BorderStroke(
                                1.dp,
                                Color.LightGray.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = filter,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            items(todos) { todo ->
                TodoItem(
                    todo = todo,
                    onClick = {
                        val projectId = todo.project?.id ?: 0L
                        val iid = todo.target?.iid ?: 0L
                        val type = todo.targetType ?: ""
                        if (projectId != 0L && iid != 0L) {
                            onTodoClick(projectId, iid, type)
                        }
                    },
                    onMarkAsDone = { onMarkAsDone(todo.id) }
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "You're almost caught up!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun TodoItem(
    todo: Todo,
    onClick: () -> Unit = {},
    onMarkAsDone: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, label, color) = when (todo.targetType) {
                        "MergeRequest" -> Triple(
                            Icons.AutoMirrored.Filled.MergeType,
                            "MERGE REQUEST",
                            Color(0xFF673AB7)
                        )

                        "Issue" -> Triple(Icons.Default.ErrorOutline, "ISSUE", Color(0xFFFF5233))
                        else -> Triple(
                            Icons.AutoMirrored.Filled.Comment,
                            "MENTION",
                            Color(0xFFD32F2F)
                        )
                    }
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = color
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = color
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = formatTimeAgo(todo.createdAt ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = todo.target?.title ?: todo.body ?: "",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = todo.body ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    todo.project?.pathWithNamespace?.let {
                        StatusBadge(
                            text = it,
                            containerColor = Color.LightGray.copy(alpha = 0.2f),
                            contentColor = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (todo.targetType == "MergeRequest") {
                        StatusBadge(
                            text = "Critical",
                            containerColor = Color(0xFFFFEBEE),
                            contentColor = Color(0xFFD32F2F)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.LightGray.copy(alpha = 0.5f), CircleShape)
                    .clickable { onMarkAsDone() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Done",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TodosLoadingSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                SkeletonLoader(modifier = Modifier.size(200.dp, 32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonLoader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(4) {
                    SkeletonLoader(
                        modifier = Modifier.size(80.dp, 36.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        items(5) {
            TodoItemSkeleton()
        }
    }
}

@Composable
fun TodoItemSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkeletonLoader(modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    SkeletonLoader(modifier = Modifier.size(100.dp, 12.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                SkeletonLoader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonLoader(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    SkeletonLoader(modifier = Modifier.size(120.dp, 20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    SkeletonLoader(modifier = Modifier.size(60.dp, 20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            SkeletonLoader(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(20.dp))
        }
    }
}

private val sampleTodos = listOf(
    Todo(
        id = 1,
        project = Project(
            id = 101,
            name = "labdroid",
            nameWithNamespace = "aboobacker/labdroid",
            webUrl = "",
            starCount = 0,
            forksCount = 0,
            lastActivityAt = "",
            defaultBranch = "main"
        ),
        author = User(
            id = 2,
            name = "John Doe",
            username = "johndoe",
            avatarUrl = null,
            webUrl = ""
        ),
        actionName = "assigned",
        targetType = "Issue",
        target = TodoTarget(title = "Memory leak in worker nodes during spike", iid = 1209),
        targetUrl = "",
        body = "",
        state = "pending",
        createdAt = "2023-10-01T10:00:00Z"
    ),
    Todo(
        id = 2,
        project = Project(
            id = 101,
            name = "labdroid",
            nameWithNamespace = "aboobacker/labdroid",
            webUrl = "",
            starCount = 0,
            forksCount = 0,
            lastActivityAt = "",
            defaultBranch = "main"
        ),
        author = User(
            id = 1,
            name = "Aboobacker Siddique",
            username = "aboobacker",
            avatarUrl = null,
            webUrl = ""
        ),
        actionName = "commented",
        targetType = "MergeRequest",
        target = TodoTarget(title = "Fix race condition in CI pipeline", iid = 452),
        targetUrl = "",
        body = "Please check the logs for more info.",
        state = "pending",
        createdAt = "2023-10-02T10:00:00Z"
    )
)

@Preview(showBackground = true)
@Composable
fun TodosScreenPreview() {
    LabdroidTheme {
        val sampleUser = User(
            id = 1,
            name = "Aboobacker Siddique",
            username = "aboobacker",
            avatarUrl = "https://secure.gravatar.com/avatar/sample",
            webUrl = ""
        )
        TodosScreenContent(
            uiState = TodoUiState.Success(
                user = sampleUser,
                todos = sampleTodos
            )
        )
    }
}
