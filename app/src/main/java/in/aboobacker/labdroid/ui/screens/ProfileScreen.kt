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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import `in`.aboobacker.labdroid.R
import `in`.aboobacker.labdroid.data.model.Project
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.ui.components.ActivityItem
import `in`.aboobacker.labdroid.ui.components.ContributionHeatmap
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import `in`.aboobacker.labdroid.ui.viewmodel.ProfileUiState
import `in`.aboobacker.labdroid.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onProjectClick: (Long) -> Unit = {},
    onSeeAllClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onActivityClick: (Long, Long, String) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.fetchProfileData()
    }

    ProfileScreenContent(
        uiState = uiState,
        onProjectClick = onProjectClick,
        onSeeAllClick = onSeeAllClick,
        onEditProfileClick = onEditProfileClick,
        onSettingsClick = onSettingsClick,
        onActivityClick = onActivityClick
    )
}

@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    onProjectClick: (Long) -> Unit = {},
    onSeeAllClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onActivityClick: (Long, Long, String) -> Unit = { _, _, _ -> }
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is ProfileUiState.Success -> {
                ProfileContent(
                    state = state,
                    onProjectClick = onProjectClick,
                    onSeeAllClick = onSeeAllClick,
                    onSettingsClick = onSettingsClick,
                    onActivityClick = onActivityClick
                )
            }

            is ProfileUiState.Error -> {
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
fun ProfileContent(
    state: ProfileUiState.Success,
    onProjectClick: (Long) -> Unit = {},
    onSeeAllClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onActivityClick: (Long, Long, String) -> Unit = { _, _, _ -> }
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "header") {
            ProfileHeader(user = state.user)
        }

        item(key = "stats") {
            ProfileStats(user = state.user)
        }

        item(key = "settings_button") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings))
                }
            }
        }

        item(key = "contributions_title") {
            Text(
                stringResource(R.string.contributions),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            if (state.isLoadingCalendar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else {
                ContributionHeatmap(state.contributionCalendar)
            }
        }

        item(key = "pinned_projects_title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.pinned_projects),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = onSeeAllClick) {
                    Text(stringResource(R.string.see_all))
                }
            }
        }

        item(key = "pinned_projects_carousel") {
            val carouselState = rememberCarouselState { state.pinnedProjects.size }
            HorizontalMultiBrowseCarousel(
                state = carouselState,
                preferredItemWidth = 260.dp,
                itemSpacing = 8.dp,
                contentPadding = PaddingValues(horizontal = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) { index ->
                val project = state.pinnedProjects[index]
                PinnedProjectItem(
                    project = project,
                    onClick = { onProjectClick(project.id) },
                    modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge)
                )
            }
        }

        item(key = "recent_activity_title") {
            Text(
                stringResource(R.string.recent_activity),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
        }

        if (state.isLoadingEvents) {
            item(key = "loading_events") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
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
                        .padding(32.dp),
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

                            "User" -> {


                            }

                            "Project" -> {

                            }

                            "Epic" -> {

                            }

                            null -> {
                                if (event.actionName.contains("pushed", ignoreCase = true)) {
                                    onActivityClick(projectId, 0L, "Push")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ProfileHeader(user: User) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Avatar with gradient border
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(3.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(4.dp)
                .clip(CircleShape)
        ) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Online indicator
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = 40.dp, y = (-10).dp)
                .size(24.dp),
            shape = CircleShape,
            color = Color(0xFF4CAF50),
            border = BorderStroke(3.dp, MaterialTheme.colorScheme.surface)
        ) {}
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = user.name,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        val subtitle = if (user.jobTitle != null) {
            "${user.jobTitle} • @${user.username}"
        } else {
            "@${user.username}"
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        if (user.bio != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = user.bio,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 48.dp)
            )
        }
    }
}

@Composable
fun ProfileStats(user: User) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatItem(label = "FOLLOWERS", value = formatCount(user.followers))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        StatItem(label = "FOLLOWING", value = formatCount(user.following))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        StatItem(label = "STARS", value = formatCount(user.starCount))
    }
}

fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> String.format("%.1fM", count / 1000000f)
        count >= 1000 -> String.format("%.1fk", count / 1000f)
        else -> count.toString()
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun PinnedProjectItem(
    project: Project,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (project.description != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = project.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = project.starCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.CallSplit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = project.forksCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    LabdroidTheme {
        val sampleUser = User(
            id = 1,
            name = "Aboobacker MK",
            username = "aboobacker",
            avatarUrl = "https://secure.gravatar.com/avatar/sample",
            webUrl = "https://gitlab.com/aboobacker",
            bio = "Android Developer | Open Source Enthusiast",
            jobTitle = "Senior Android Engineer",
            followers = 1200,
            following = 438,
            starCount = 85
        )
        ProfileScreenContent(
            uiState = ProfileUiState.Success(
                user = sampleUser,
                pinnedProjects = listOf(
                    Project(
                        id = 1,
                        name = "labdroid",
                        nameWithNamespace = "aboobacker/labdroid",
                        description = "A professional GitLab client for Android built with Jetpack Compose.",
                        webUrl = "https://gitlab.com/aboobacker/labdroid",
                        starCount = 12,
                        forksCount = 5,
                        lastActivityAt = "2023-10-01T10:00:00Z",
                        defaultBranch = "main"
                    ),
                    Project(
                        id = 2,
                        name = "compose-ui-kit",
                        nameWithNamespace = "aboobacker/compose-ui-kit",
                        description = "A collection of reusable Jetpack Compose components.",
                        webUrl = "https://gitlab.com/aboobacker/compose-ui-kit",
                        starCount = 45,
                        forksCount = 10,
                        lastActivityAt = "2023-10-02T10:00:00Z",
                        defaultBranch = "main"
                    )
                ),
                contributionCalendar = mapOf(
                    "2023-10-01" to 5,
                    "2023-10-02" to 3,
                    "2023-10-05" to 12
                ),
                isLoadingCalendar = false,
                isLoadingEvents = false
            )
        )
    }
}
