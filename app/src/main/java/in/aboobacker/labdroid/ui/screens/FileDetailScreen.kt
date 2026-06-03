package `in`.aboobacker.labdroid.ui.screens

import android.util.Base64
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import `in`.aboobacker.labdroid.data.model.RepositoryFile
import `in`.aboobacker.labdroid.ui.components.MarkdownText
import `in`.aboobacker.labdroid.ui.viewmodel.FileDetailUiState
import `in`.aboobacker.labdroid.ui.viewmodel.FileDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailScreen(
    projectId: Long,
    filePath: String,
    ref: String,
    viewModel: FileDetailViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId, filePath, ref) {
        viewModel.loadFile(projectId, filePath, ref)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (val state = uiState) {
            is FileDetailUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is FileDetailUiState.Success -> {
                FileContent(state.file, state.content)
            }

            is FileDetailUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun FileContent(file: RepositoryFile, content: String) {
    val extension = file.fileName.substringAfterLast('.', "").lowercase()
    val isImage = extension in listOf("png", "jpg", "jpeg", "gif", "webp", "bmp")
    val isMarkdown = extension in listOf("md", "markdown")

    if (isImage) {
        val imageBytes = remember(file.content) {
            if (file.encoding == "base64") {
                try {
                    Base64.decode(file.content, Base64.DEFAULT)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (imageBytes != null) {
                AsyncImage(
                    model = imageBytes,
                    contentDescription = file.fileName,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text("Failed to load image")
            }
        }
    } else if (isMarkdown) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            MarkdownText(content)
        }
    } else {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            content.split("\n").forEachIndexed { index, line ->
                Row {
                    Text(
                        text = "${index + 1}",
                        modifier = Modifier.width(32.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}
