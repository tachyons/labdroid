package `in`.aboobacker.labdroid.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import `in`.aboobacker.labdroid.data.model.Todo
import `in`.aboobacker.labdroid.ui.components.formatTimeAgo
import `in`.aboobacker.labdroid.ui.viewmodel.TodoViewModel

@Composable
fun TodosScreenWear(
    viewModel: TodoViewModel,
    onTodoClick: (Long, Long, String) -> Unit = { _, _, _ -> }
) {
    val todos = viewModel.todos.collectAsLazyPagingItems()

    TodosScreenContentWear(
        todos = todos,
        onTodoClick = onTodoClick
    )
}

@Composable
fun TodosScreenContentWear(
    todos: LazyPagingItems<Todo>,
    onTodoClick: (Long, Long, String) -> Unit = { _, _, _ -> }
) {
    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState) {
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

            if (todos.loadState.refresh is LoadState.Loading && todos.itemCount == 0) {
                item {
                    CircularProgressIndicator(modifier = Modifier.padding(20.dp))
                }
            } else if (todos.loadState.refresh is LoadState.Error && todos.itemCount == 0) {
                val error = todos.loadState.refresh as LoadState.Error
                item {
                    Text(
                        error.error.message ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (todos.itemCount == 0 && todos.loadState.refresh is LoadState.NotLoading) {
                item {
                    Text(
                        "No pending tasks",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(
                    count = todos.itemCount,
                    key = todos.itemKey { it.id }
                ) { index ->
                    val todo = todos[index]
                    if (todo != null) {
                        TodoItemWear(
                            todo = todo,
                            onClick = {
                                val projectId = todo.project?.id ?: 0L
                                val iid = todo.target?.iid ?: 0L
                                val type = todo.targetType ?: ""
                                onTodoClick(projectId, iid, type)
                            }
                        )
                    }
                }

                if (todos.loadState.append is LoadState.Loading) {
                    item {
                        CircularProgressIndicator(modifier = Modifier.padding(10.dp))
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
        title = {
            Text(
                todo.target?.title ?: todo.body ?: "",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        time = { Text(formatTimeAgo(todo.createdAt ?: "")) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
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
    // For preview purposes, we can't easily create LazyPagingItems without a real or mocked Flow.
    // In a real app, you might use a fake/mock for this.
    MaterialTheme {
        Text("Preview requires Mock Paging Data")
    }
}
