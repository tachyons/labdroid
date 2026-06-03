package `in`.aboobacker.labdroid.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import coil3.compose.AsyncImage
import `in`.aboobacker.labdroid.data.model.AwardEmoji
import `in`.aboobacker.labdroid.data.model.Commit
import `in`.aboobacker.labdroid.data.model.Event
import `in`.aboobacker.labdroid.data.model.Issue
import `in`.aboobacker.labdroid.data.model.MergeRequest
import `in`.aboobacker.labdroid.data.model.Note
import `in`.aboobacker.labdroid.data.model.Pipeline
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun ActivityItem(event: Event, onClick: () -> Unit = {}) {
    val (icon, color) = when {
        event.actionName.contains(
            "pushed",
            ignoreCase = true
        ) -> Icons.Default.VerticalAlignTop to MaterialTheme.colorScheme.primary

        event.actionName.contains(
            "merged",
            ignoreCase = true
        ) -> Icons.AutoMirrored.Filled.MergeType to MaterialTheme.colorScheme.secondary

        event.actionName.contains(
            "opened",
            ignoreCase = true
        ) || event.actionName.contains(
            "created",
            ignoreCase = true
        ) -> Icons.Default.ErrorOutline to MaterialTheme.colorScheme.tertiary

        event.actionName.contains(
            "commented",
            ignoreCase = true
        ) -> Icons.AutoMirrored.Filled.Comment to MaterialTheme.colorScheme.primary

        else -> Icons.Default.Adjust to MaterialTheme.colorScheme.outline
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        // Timeline part
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = color
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .background(Color.Gray.copy(alpha = 0.2f))
            )
        }

        // Card part
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
                .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (event.author?.avatarUrl != null) {
                        AsyncImage(
                            model = event.author.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        event.author?.name ?: event.authorUsername ?: "Unknown",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = formatTimeAgo(event.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                val actionTitle = when {
                    event.actionName.contains("pushed", ignoreCase = true) -> {
                        val ref = event.pushData?.ref ?: "branch"
                        "Pushed to $ref"
                    }

                    event.actionName.contains(
                        "merged",
                        ignoreCase = true
                    ) -> "Merged ${event.targetType ?: "Merge Request"}"

                    event.actionName.contains(
                        "opened",
                        ignoreCase = true
                    ) -> "Opened ${event.targetType ?: "item"}"

                    event.actionName.contains(
                        "commented",
                        ignoreCase = true
                    ) -> "Commented on ${event.targetType ?: "item"}"

                    else -> event.actionName.replaceFirstChar { it.uppercase() }
                }

                Text(
                    text = actionTitle,
                    style = MaterialTheme.typography.bodyLarge
                )

                if (event.targetTitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.targetTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                    )
                }

                if (event.pushData != null && event.pushData.commitCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Commit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${event.pushData.commitCount} commit${if (event.pushData.commitCount > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IssueItem(issue: Issue, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusIcon = if (issue.state == "opened") {
                    Icons.Default.ErrorOutline
                } else {
                    Icons.Default.CheckCircle
                }
                val statusColor =
                    if (issue.state == "opened") MaterialTheme.colorScheme.error else Color(
                        0xFF4CAF50
                    )

                Icon(
                    statusIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = statusColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "#${issue.iid}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.weight(1f))

                if (issue.assignees.isNotEmpty()) {
                    AsyncImage(
                        model = issue.assignees.first().avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = issue.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 22.sp
                )
            )

            if (issue.labels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    issue.labels.take(3).forEach { label ->
                        val color = try {
                            Color(label.color.let { if (it.startsWith("#")) it else "#$it" }
                                .toColorInt())
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.secondaryContainer
                        }

                        val textColor = try {
                            label.textColor?.let { Color(it.toColorInt()) }
                                ?: if (isColorDark(color)) Color.White else Color.Black
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }

                        StatusBadge(
                            text = label.name,
                            containerColor = color,
                            contentColor = textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    issue.userNotesCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    Icons.Default.Adjust,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    formatTimeAgo(issue.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                if (issue.labels.any { it.name.contains("Critical", ignoreCase = true) }) {
                    Spacer(modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Critical",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MergeRequestItem(mr: MergeRequest, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mr.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    ),
                    modifier = Modifier.weight(1f)
                )

                mr.headPipeline?.status?.let { status ->
                    val (containerColor, contentColor) = when (status) {
                        "success" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                        "running" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                        "failed" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    StatusBadge(
                        text = status,
                        containerColor = containerColor,
                        contentColor = contentColor
                    )
                }
            }

            if (mr.labels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mr.labels.take(3).forEach { label ->
                        val color = try {
                            Color(label.color.let { if (it.startsWith("#")) it else "#$it" }
                                .toColorInt())
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.secondaryContainer
                        }

                        val textColor = try {
                            label.textColor?.let { Color(it.toColorInt()) }
                                ?: if (isColorDark(color)) Color.White else Color.Black
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }

                        StatusBadge(
                            text = label.name,
                            containerColor = color,
                            contentColor = textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "!${mr.iid}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "•",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = mr.author?.username ?: "unknown",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.MergeType,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = mr.sourceBranch,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.Adjust,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = mr.targetBranch,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Participants
                Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                    val participants = listOfNotNull(mr.author) + mr.assignees
                    participants.take(3).forEach { user ->
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(1.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    mr.userNotesCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                if (mr.assignees.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        mr.assignees.size.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun CommentItem(
    note: Note,
    onProfileClick: (Long) -> Unit = {},
    onReplyClick: (String) -> Unit = {},
    onReferenceClick: (String) -> Unit = {},
    isReply: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isReply) 48.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = note.author.avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(if (isReply) 28.dp else 36.dp)
                .clip(CircleShape)
                .clickable { onProfileClick(note.author.id) },
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.weight(1f)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = note.author.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable { onProfileClick(note.author.id) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatTimeAgo(note.createdAt.toString()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    MarkdownText(markdown = note.body, onReferenceClick = onReferenceClick)
                }
            }

            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Reactions
                note.awardEmoji.groupBy { it.name }.forEach { (name, emojis) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(getEmojiIcon(name), fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            emojis.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { note.discussionId?.let { onReplyClick(it) } }
                ) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Reply",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun getEmojiIcon(name: String): String {
    return when (name) {
        "thumbsup" -> "👍"
        "thumbsdown" -> "👎"
        "smile" -> "😄"
        "tada" -> "🎉"
        "confused" -> "😕"
        "heart" -> "❤️"
        "rocket" -> "🚀"
        "eyes" -> "👀"
        else -> "❓"
    }
}

@Composable
fun CommitItem(commit: Commit, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = commit.authorName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = commit.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Text(
                    text = "${commit.authorName} • ${formatTimeAgo(commit.committedDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.clickable { /* Commit author ID not available in simple Commit model */ }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = commit.shortId,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun PipelineItem(pipeline: Pipeline, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (icon, color) = when (pipeline.status) {
                    "success" -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
                    "running" -> Icons.Default.CheckCircle to Color(0xFF2196F3)
                    "failed" -> Icons.Default.CheckCircle to Color(0xFFF44336)
                    else -> Icons.Default.CheckCircle to Color.Gray
                }

                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "#${pipeline.id}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(
                    text = pipeline.status.uppercase(),
                    containerColor = color.copy(alpha = 0.1f),
                    contentColor = color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Pipeline for ${pipeline.ref}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Ref: ${pipeline.ref} • ${formatTimeAgo(pipeline.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun PreviewCommentItem() {
    LabdroidTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            val author = User(
                name = "Aboobacker",
                id = 1,
                username = "tachyons",
                avatarUrl = "https://avatars.githubusercontent.com/u/3112976?v=4&size=64",
                webUrl = "",
                bio = "Human",
                jobTitle = "Software Engineer",
                followers = 1,
                following = 2,
                starCount = 3
            )
            CommentItem(
                note = Note(
                    id = 1,
                    body = "This is a sample comment body. It can span multiple lines if needed.",
                    attachment = null,
                    author = author,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                    system = false,
                    noteableId = 1,
                    noteableType = "",
                    noteableIid = 1,
                    awardEmoji = listOf(
                        AwardEmoji(id = 1, name = "thumbsup", user = author),
                        AwardEmoji(id = 2, name = "thumbsup", user = author),
                        AwardEmoji(id = 3, name = "rocket", user = author)
                    )
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCommitItem() {
    LabdroidTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CommitItem(
                commit = Commit(
                    id = "ae982bc47d12f309a88c2190",
                    shortId = "ae982bc4",
                    title = "feat: implement biometric authentication for mobile vault",
                    authorName = "Alex Thompson",
                    authorEmail = "alex@example.com",
                    authoredDate = "2023-10-24T14:54:00Z",
                    committerName = "Alex Thompson",
                    committerEmail = "alex@example.com",
                    committedDate = "2023-10-24T14:54:00Z"
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPipelineItem() {
    LabdroidTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PipelineItem(
                pipeline = Pipeline(
                    id = 982145,
                    projectId = 1,
                    status = "running",
                    ref = "main",
                    sha = "ae982bc4",
                    webUrl = "",
                    createdAt = "2023-10-24T14:54:00Z",
                    updatedAt = "2023-10-24T14:58:00Z",
                    duration = 252.0
                )
            )
        }
    }
}
