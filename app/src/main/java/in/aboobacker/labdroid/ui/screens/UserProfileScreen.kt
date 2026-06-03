package `in`.aboobacker.labdroid.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import `in`.aboobacker.labdroid.R
import `in`.aboobacker.labdroid.data.model.Event
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.ui.components.ActivityItem
import `in`.aboobacker.labdroid.ui.components.ContributionHeatmap
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import `in`.aboobacker.labdroid.ui.viewmodel.ProfileUiState
import `in`.aboobacker.labdroid.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: Long? = null,
    username: String? = null,
    viewModel: ProfileViewModel,
    onActivityClick: (Long, Long, String) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId, username) {
        viewModel.fetchProfileData(userId, username)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is ProfileUiState.Success -> {
                UserProfileContent(
                    state = state,
                    onActivityClick = onActivityClick,
                    onFollowClick = {
                        if (state.user.isFollowed == true) {
                            viewModel.unfollowUser(state.user.id)
                        } else {
                            viewModel.followUser(state.user.id)
                        }
                    }
                )
            }

            is ProfileUiState.Error -> {
                Text(state.message, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun UserProfileContent(
    state: ProfileUiState.Success,
    onActivityClick: (Long, Long, String) -> Unit = { _, _, _ -> },
    onFollowClick: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "user_info") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape)
                    ) {
                        AsyncImage(
                            model = state.user.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .size(24.dp)
                            .offset(x = (-4).dp, y = (-4).dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    state.user.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "@${state.user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    state.user.location?.let { location ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                location,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                    state.user.websiteUrl?.let { website ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val displayUrl =
                                website.removePrefix("https://").removePrefix("http://")
                                    .removeSuffix("/")
                            Text(
                                displayUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Text(
                    state.user.bio ?: "No bio provided",
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val isFollowed = state.user.isFollowed == true
                    Button(
                        onClick = onFollowClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowed)
                                MaterialTheme.colorScheme.surfaceVariant
                            else
                                MaterialTheme.colorScheme.primary,
                            contentColor = if (isFollowed)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isFollowed) BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline
                        ) else null
                    ) {
                        Text(
                            if (isFollowed)
                                stringResource(R.string.unfollow)
                            else
                                stringResource(R.string.follow)
                        )
                    }
                }
            }
        }

        item(key = "contributions_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.contributions),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (state.isLoadingCalendar) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    } else {
                        ContributionHeatmap(state.contributionCalendar)
                    }
                }
            }
        }


        item(key = "recent_activity_title") {
            Text(
                stringResource(R.string.recent_activity),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        }

        if (state.isLoadingEvents) {
            item(key = "loading_events") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        } else if (state.events.isEmpty()) {
            item(key = "no_activity") {
                Text(
                    stringResource(R.string.no_recent_activity),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            items(
                items = state.events,
                key = { it.id ?: it.hashCode() }
            ) { event ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ActivityItem(event) {
                        val projectId = event.projectId
                        val targetType = event.targetType

                        when (targetType) {
                            "Issue" -> {
                                event.targetIid?.let { onActivityClick(projectId, it, "Issue") }
                            }

                            "MergeRequest" -> {
                                event.targetIid?.let {
                                    onActivityClick(
                                        projectId,
                                        it,
                                        "MergeRequest"
                                    )
                                }
                            }

                            "Note" -> {
                                val noteableIid = event.note?.noteableIid
                                val noteableType = event.note?.noteableType
                                if (noteableIid != null && noteableType != null) {
                                    onActivityClick(projectId, noteableIid, noteableType)
                                }
                            }

                            null -> {

                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UserProfileContentPreview() {
    LabdroidTheme {
        val mockUser = User(
            id = 1,
            name = "John Doe",
            username = "johndoe",
            avatarUrl = null,
            bio = "Senior Android Developer focusing on Jetpack Compose and Kotlin Multiplatform.",
            location = "San Francisco, CA",
            websiteUrl = "https://github.com/johndoe"
        )

        val mockState = ProfileUiState.Success(
            user = mockUser,
            pinnedProjects = emptyList(),
            contributionCalendar = mapOf(
                "2023-10-27" to 5,
                "2023-10-26" to 3,
                "2023-10-25" to 8,
                "2023-10-24" to 0,
                "2023-10-23" to 2
            ),
            events = listOf(
                Event(
                    id = 1,
                    projectId = 101,
                    actionName = "pushed to",
                    createdAt = "2023-10-27T10:00:00Z",
                    targetTitle = "feat: add user profile preview"
                ),
                Event(
                    id = 2,
                    projectId = 102,
                    actionName = "opened issue",
                    createdAt = "2023-10-26T15:30:00Z",
                    targetTitle = "Bug: Login fails on older devices"
                )
            ),
            isLoadingCalendar = false,
            isLoadingEvents = false
        )

        Surface(color = MaterialTheme.colorScheme.background) {
            UserProfileContent(state = mockState)
        }
    }
}
