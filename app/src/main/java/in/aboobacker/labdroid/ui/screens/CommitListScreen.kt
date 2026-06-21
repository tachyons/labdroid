package `in`.aboobacker.labdroid.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import `in`.aboobacker.labdroid.data.model.Commit
import `in`.aboobacker.labdroid.ui.components.CommitItem
import `in`.aboobacker.labdroid.ui.components.formatCommitDate
import `in`.aboobacker.labdroid.ui.viewmodel.CommitListViewModel

@Composable
fun CommitListScreen(
    viewModel: CommitListViewModel,
    projectId: Long,
    ref: String? = null,
    onCommitClick: (String) -> Unit
) {
    val commits = viewModel.commits.collectAsLazyPagingItems()

    LaunchedEffect(projectId, ref) {
        viewModel.fetchCommits(projectId, ref)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            commits.loadState.refresh is LoadState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            commits.loadState.refresh is LoadState.Error -> {
                val error = commits.loadState.refresh as LoadState.Error
                Text(
                    text = error.error.message ?: "Unknown error",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }

            else -> {
                CommitListPagingContent(
                    commits = commits,
                    onCommitClick = onCommitClick
                )
            }
        }
    }
}

@Composable
fun CommitListPagingContent(
    commits: LazyPagingItems<Commit>,
    onCommitClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        var lastDate: String? = null
        items(
            count = commits.itemCount,
            key = commits.itemKey { it.id }
        ) { index ->
            val commit = commits[index]
            if (commit != null) {
                val date = formatCommitDate(commit.committedDate)
                if (date != lastDate) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    lastDate = date
                }
                CommitItem(commit, onClick = { onCommitClick(commit.id) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (commits.loadState.append is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
