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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.aboobacker.labdroid.data.model.Pipeline
import `in`.aboobacker.labdroid.data.model.PipelineJob
import `in`.aboobacker.labdroid.ui.components.StatusBadge
import `in`.aboobacker.labdroid.ui.components.formatTimeAgo
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import `in`.aboobacker.labdroid.ui.viewmodel.PipelineUiState
import `in`.aboobacker.labdroid.ui.viewmodel.PipelineViewModel

@Composable
fun PipelineScreen(
    viewModel: PipelineViewModel,
    projectId: Long,
    pipelineId: Long
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId, pipelineId) {
        viewModel.fetchPipeline(projectId, pipelineId)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (val state = uiState) {
            is PipelineUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is PipelineUiState.Success -> {
                PipelineContent(state)
            }

            is PipelineUiState.Error -> {
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

@Composable
fun PipelineContent(state: PipelineUiState.Success) {
    val pipeline = state.pipeline
    val jobs = state.jobs

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PipelineHeaderCard(pipeline)
        }

        item {
            Text(
                text = "Pipeline Stages",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        val stages = jobs.groupBy { it.stage }
        stages.forEach { (stageName, stageJobs) ->
            item {
                StageItem(stageName, stageJobs)
            }
        }
    }
}

@Composable
fun PipelineHeaderCard(pipeline: Pipeline) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val statusColor = when (pipeline.status) {
                    "success" -> Color(0xFF4CAF50)
                    "running" -> Color(0xFF2196F3)
                    "failed" -> Color(0xFFF44336)
                    else -> Color.Gray
                }

                StatusBadge(
                    text = pipeline.status.uppercase(),
                    containerColor = statusColor.copy(alpha = 0.1f),
                    contentColor = statusColor
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Updated ${formatTimeAgo(pipeline.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Pipeline for ${pipeline.ref}",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("REF", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        pipeline.ref, fontWeight = FontWeight.Bold,
                        modifier = Modifier.widthIn(max = 180.dp)
                    )
                }
                Column {
                    Text(
                        "DURATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(formatDuration(pipeline.duration), fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("COMMIT", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        pipeline.sha.take(8),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    )
                }
            }
        }
    }
}

@Composable
fun StageItem(stageName: String, jobs: List<PipelineJob>) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stageName.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${jobs.size} jobs",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        jobs.forEach { job ->
            JobItem(job)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun JobItem(job: PipelineJob) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (icon, color) = when (job.status) {
                "success" -> Icons.Default.Check to Color(0xFF4CAF50)
                "running" -> Icons.Default.Refresh to Color(0xFF2196F3)
                "failed" -> Icons.Default.Close to Color(0xFFF44336)
                "manual" -> Icons.Default.PlayArrow to Color.Gray
                else -> Icons.Default.Check to Color.Gray
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = job.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            if (job.duration != null) {
                Text(
                    text = formatDuration(job.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

fun formatDuration(durationSeconds: Double?): String {
    if (durationSeconds == null) return ""
    val minutes = (durationSeconds / 60).toInt()
    val seconds = (durationSeconds % 60).toInt()
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}

@Preview(showBackground = true)
@Composable
fun PipelineScreenPreview() {
    LabdroidTheme {
        PipelineContent(
            state = PipelineUiState.Success(
                pipeline = Pipeline(
                    id = 8429103,
                    projectId = 1,
                    status = "running",
                    ref = "v2.4.0-rc1/11111111111111111111",
                    sha = "ae982bc4",
                    webUrl = "",
                    createdAt = "2023-10-24T14:54:00Z",
                    updatedAt = "2023-10-24T14:56:00Z",
                    duration = 765.0
                ),
                jobs = listOf(
                    PipelineJob(
                        id = 1,
                        name = "gradle-assemble",
                        stage = "Build",
                        status = "success",
                        duration = 252.0
                    ),
                    PipelineJob(
                        id = 2,
                        name = "lint-check",
                        stage = "Build",
                        status = "success",
                        duration = 105.0
                    ),
                    PipelineJob(
                        id = 3,
                        name = "unit-tests",
                        stage = "Test",
                        status = "running",
                        duration = 120.0
                    ),
                    PipelineJob(
                        id = 4,
                        name = "ui-tests",
                        stage = "Test",
                        status = "manual",
                        duration = null
                    )
                )
            )
        )
    }
}
