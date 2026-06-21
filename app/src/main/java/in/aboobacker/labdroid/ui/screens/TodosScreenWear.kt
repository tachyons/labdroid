package `in`.aboobacker.labdroid.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import `in`.aboobacker.labdroid.data.model.Project
import `in`.aboobacker.labdroid.data.model.Todo
import `in`.aboobacker.labdroid.data.model.TodoTarget
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.ui.components.formatTimeAgo
import `in`.aboobacker.labdroid.ui.viewmodel.TodoUiState
import `in`.aboobacker.labdroid.ui.viewmodel.TodoViewModel

@Composable
fun TodosScreenWear(
    viewModel: TodoViewModel,
    onTodoClick: (Long, Long, String) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()

    TodosScreenContentWear(
        uiState = uiState,
        onTodoClick = onTodoClick
    )
}

@Composable
fun TodosScreenContentWear(
    uiState: TodoUiState,
    onTodoClick: (Long, Long, String) -> Unit = { _, _, _ -> }
) {
    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState) {
        when (val state = uiState) {
            is TodoUiState.Loading -> {
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item { CircularProgressIndicator(modifier = Modifier.padding(20.dp)) }
                }
            }

            is TodoUiState.Success -> {
                ScalingLazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 20.dp)
                ) {
                    item {
                        ListHeader {
                            Text("Todos", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    if (state.todos.isEmpty()) {
                        item {
                            Text(
                                "No pending tasks",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(state.todos.size) { index ->
                            TodoItemWear(
                                todo = state.todos[index],
                                onClick = {
                                    val projectId = state.todos[index].project?.id ?: 0L
                                    val iid = state.todos[index].target?.iid ?: 0L
                                    val type = state.todos[index].targetType ?: ""
                                    onTodoClick(projectId, iid, type)
                                }
                            )
                        }
                    }
                }
            }

            is TodoUiState.Error -> {
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Text(
                            state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TodoItemWear(
    todo: Todo,
    onClick: () -> Unit
) {
    AppCard(
        onClick = onClick,
        appName = { Text(todo.targetType ?: "Todo") },
        title = { Text(todo.target?.title ?: todo.body ?: "", maxLines = 2, overflow = TextOverflow.Ellipsis) },
        time = { Text(formatTimeAgo(todo.createdAt ?: "")) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(
            text = todo.project?.pathWithNamespace ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun TodosScreenWearPreview() {
    MaterialTheme {
        TodosScreenContentWear(
            uiState = TodoUiState.Success(
                user = User(id = 1, name = "Test User", username = "testuser", avatarUrl = null, webUrl = ""),
                todos = listOf(
                    Todo(
                        id = 1,
                        project = Project(id = 101, name = "labdroid", nameWithNamespace = "aboobacker/labdroid", webUrl = "", starCount = 0, forksCount = 0, lastActivityAt = "", defaultBranch = "main"),
                        targetType = "Issue",
                        target = TodoTarget(title = "Fix memory leak", iid = 1),
                        createdAt = "2023-10-01T10:00:00Z"
                    ),
                    Todo(
                        id = 2,
                        project = Project(id = 101, name = "labdroid", nameWithNamespace = "aboobacker/labdroid", webUrl = "", starCount = 0, forksCount = 0, lastActivityAt = "", defaultBranch = "main"),
                        targetType = "MergeRequest",
                        target = TodoTarget(title = "Update dependencies", iid = 2),
                        createdAt = "2023-10-02T10:00:00Z"
                    )
                )
            )
        )
    }
}
