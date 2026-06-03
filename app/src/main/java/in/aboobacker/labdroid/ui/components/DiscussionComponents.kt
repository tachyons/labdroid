package `in`.aboobacker.labdroid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import `in`.aboobacker.labdroid.data.model.Discussion
import `in`.aboobacker.labdroid.data.model.Note
import `in`.aboobacker.labdroid.data.model.User
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun DiscussionItem(
    discussion: Discussion,
    modifier: Modifier = Modifier,
    onReplyClick: (String) -> Unit = {},
    onReferenceClick: (String) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        discussion.notes.forEachIndexed { index, note ->
            NoteItem(
                note = note,
                isReply = index > 0,
                onReplyClick = { onReplyClick(discussion.id) },
                onReferenceClick = onReferenceClick
            )
            if (index < discussion.notes.size - 1) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun NoteItem(
    note: Note,
    isReply: Boolean = false,
    onReplyClick: () -> Unit = {},
    onReferenceClick: (String) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isReply) 16.dp else 0.dp)
    ) {
        AsyncImage(
            model = note.author.avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(if (isReply) 32.dp else 40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = note.author.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "@${note.author.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatInstant(note.createdAt.toEpochMilliseconds()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            MarkdownText(markdown = note.body, onReferenceClick = onReferenceClick)

            if (!note.system && !isReply) {
                TextButton(
                    onClick = onReplyClick,
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reply", fontSize = 12.sp)
                }
            }
        }
    }
}

fun formatInstant(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Preview(showBackground = true)
@OptIn(ExperimentalTime::class)
@Composable
fun DiscussionItemPreview() {
    val sampleUser = User(
        id = 1,
        name = "Aboobacker",
        username = "aboobacker11111111111111111111111",
        avatarUrl = null,
        webUrl = ""
    )
    val sampleNote = Note(
        id = 1,
        body = "This is a sample note with **markdown** support.",
        author = sampleUser,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
        system = false,
        noteableId = 1,
        noteableType = "Issue",
        discussionId = "abc"
    )
    val sampleReply = Note(
        id = 2,
        body = "This is a reply to the sample note.",
        author = sampleUser,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
        system = false,
        noteableId = 1,
        noteableType = "Issue",
        discussionId = "abc"
    )
    val sampleDiscussion = Discussion(
        id = "abc",
        individualNote = false,
        notes = listOf(sampleNote, sampleReply)
    )

    LabdroidTheme {
        DiscussionItem(
            discussion = sampleDiscussion,
            modifier = Modifier.padding(16.dp)
        )
    }
}
