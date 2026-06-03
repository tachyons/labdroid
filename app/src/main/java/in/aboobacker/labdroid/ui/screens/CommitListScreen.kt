package `in`.aboobacker.labdroid.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.aboobacker.labdroid.data.model.Commit
import `in`.aboobacker.labdroid.ui.components.CommitItem
import `in`.aboobacker.labdroid.ui.components.formatCommitDate
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import `in`.aboobacker.labdroid.ui.viewmodel.CommitListUiState
import `in`.aboobacker.labdroid.ui.viewmodel.CommitListViewModel

@Composable
fun CommitListScreen(
    viewModel: CommitListViewModel,
    projectId: Long,
    ref: String? = null,
    onCommitClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId, ref) {
        viewModel.fetchCommits(projectId, ref)
    }

    CommitListScreenContent(
        uiState = uiState,
        onCommitClick = onCommitClick
    )
}

@Composable
fun CommitListScreenContent(
    uiState: CommitListUiState,
    onCommitClick: (String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (uiState) {
            is CommitListUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is CommitListUiState.Success -> {
                val groupedCommits = uiState.commits.groupBy { formatCommitDate(it.committedDate) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    groupedCommits.forEach { (date, commits) ->
                        item {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(commits) { commit ->
                            CommitItem(commit, onClick = { onCommitClick(commit.id) })
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            is CommitListUiState.Error -> {
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

@Preview(showBackground = true)
@Composable
fun CommitListScreenPreview() {
    LabdroidTheme {
        CommitListScreenContent(
            uiState = CommitListUiState.Success(
                commits = listOf(
                    Commit(
                        id = "ae982bc47d12f309a88c2190",
                        shortId = "ae982bc4",
                        title = "feat: implement biometric authentication for mobile vault",
                        authorName = "Alex Thompson",
                        authorEmail = "alex@example.com",
                        authoredDate = "2023-10-24T14:54:00Z",
                        committerName = "Alex Thompson",
                        committerEmail = "alex@example.com",
                        committedDate = "2023-10-24T14:54:00Z"
                    ),
                    Commit(
                        id = "f291c90a8c2190ae982bc47d",
                        shortId = "f291c90a",
                        title = "fix: resolve memory leak in pipeline worker thread",
                        authorName = "Sarah Chen",
                        authorEmail = "sarah@example.com",
                        authoredDate = "2023-10-24T12:30:00Z",
                        committerName = "Sarah Chen",
                        committerEmail = "sarah@example.com",
                        committedDate = "2023-10-24T12:30:00Z"
                    ),
                    Commit(
                        id = "7b3d11ef8c2190ae982bc47d",
                        shortId = "7b3d11ef",
                        title = "docs: update readme with deployment steps",
                        authorName = "Michael Rodriguez",
                        authorEmail = "michael@example.com",
                        authoredDate = "2023-10-23T10:15:00Z",
                        committerName = "Michael Rodriguez",
                        committerEmail = "michael@example.com",
                        committedDate = "2023-10-23T10:15:00Z"
                    )
                )
            ),
            onCommitClick = {}
        )
    }
}
