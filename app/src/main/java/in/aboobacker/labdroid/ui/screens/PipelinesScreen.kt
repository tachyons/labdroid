package `in`.aboobacker.labdroid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import `in`.aboobacker.labdroid.ui.components.PipelineItem
import `in`.aboobacker.labdroid.ui.viewmodel.PipelinesViewModel

@Composable
fun PipelinesScreen(
    projectId: Long,
    viewModel: PipelinesViewModel,
    onPipelineClick: (Long, Long) -> Unit
) {
    val pipelines = viewModel.pipelines.collectAsLazyPagingItems()

    LaunchedEffect(projectId) {
        viewModel.fetchPipelines(projectId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            pipelines.loadState.refresh is LoadState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            pipelines.loadState.refresh is LoadState.Error -> {
                val error = pipelines.loadState.refresh as LoadState.Error
                Text(
                    text = error.error.message ?: "Unknown error",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = pipelines.itemCount,
                        key = pipelines.itemKey { it.id }
                    ) { index ->
                        val pipeline = pipelines[index]
                        if (pipeline != null) {
                            PipelineItem(
                                pipeline,
                                onClick = { onPipelineClick(projectId, pipeline.id) }
                            )
                        }
                    }

                    if (pipelines.loadState.append is LoadState.Loading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
