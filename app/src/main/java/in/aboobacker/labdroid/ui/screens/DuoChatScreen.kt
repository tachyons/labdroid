package `in`.aboobacker.labdroid.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.aboobacker.labdroid.ApprovalDecision
import `in`.aboobacker.labdroid.DuoContextItem
import `in`.aboobacker.labdroid.WorkflowStatus
import `in`.aboobacker.labdroid.ui.components.LocalTopBarActions
import `in`.aboobacker.labdroid.ui.components.MarkdownText
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import `in`.aboobacker.labdroid.ui.viewmodel.ChatMessage
import `in`.aboobacker.labdroid.ui.viewmodel.DuoChatViewModel

@Composable
fun DuoChatScreen(
    projectId: Long? = null,
    rootNamespaceId: Long? = null,
    initialGoal: String? = null,
    additionalContext: List<DuoContextItem>? = null,
    onBackClick: () -> Unit = {},
    viewModel: DuoChatViewModel = hiltViewModel()
) {
    val messages = viewModel.messages
    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val workflowStatus by viewModel.workflowStatus.collectAsStateWithLifecycle()

    LaunchedEffect(projectId, rootNamespaceId, initialGoal, additionalContext) {
        viewModel.setContext(projectId, rootNamespaceId, initialGoal, additionalContext)
    }

    DuoChatContent(
        messages = messages,
        isConnecting = isConnecting,
        isConnected = isConnected,
        workflowStatus = workflowStatus,
        onSendMessage = { viewModel.sendMessage(it) },
        onClearChat = { viewModel.clearChat() },
        onHandleApprovalDecision = { viewModel.handleApprovalDecision(it) },
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuoChatContent(
    messages: List<ChatMessage>,
    isConnecting: Boolean,
    isConnected: Boolean,
    workflowStatus: WorkflowStatus?,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onHandleApprovalDecision: (ApprovalDecision) -> Unit,
    onBackClick: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val topBarActions = LocalTopBarActions.current

    LaunchedEffect(isConnected, isConnecting, workflowStatus) {
        topBarActions.topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "GitLab Duo Chat",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        val statusText = when {
                            isConnecting -> "Connecting..."
                            isConnected -> "Connected" + (workflowStatus?.let { " • $it" } ?: "")
                            else -> "Disconnected"
                        }
                        Text(
                            statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    TextButton(onClick = onClearChat) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Chat", style = MaterialTheme.typography.labelLarge)
                    }
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (messages.isEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    DuoAvatar()
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Hi, I'm GitLab Duo",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "I can help you write code, summarize merge requests, and answer questions about your projects.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    DuoMessageBubble(
                        "How can I assist you with your work today?",
                        "GitLab Duo • Just now"
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    Text(
                        "SUGGESTED ACTIONS",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }

                item {
                    SuggestedActionItem(
                        icon = Icons.Default.Description,
                        text = "Summarize my latest MR",
                        onClick = { onSendMessage("Summarize my latest MR") }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SuggestedActionItem(
                        icon = Icons.Default.Code,
                        text = "Explain this code snippet",
                        onClick = { onSendMessage("Explain this code snippet") }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SuggestedActionItem(
                        icon = Icons.Default.Science,
                        text = "Write a unit test for my changes",
                        onClick = { onSendMessage("Write a unit test for my changes") }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else {
                items(messages.size) { index ->
                    val chatMessage = messages[index]
                    when {
                        chatMessage.isSystem -> {
                            SystemMessage(chatMessage.text)
                        }

                        chatMessage.isFromUser -> {
                            UserMessageBubble(chatMessage.text)
                        }

                        else -> {
                            DuoMessageBubble(
                                chatMessage.text,
                                footer = "GitLab Duo • Just now",
                                isLoading = chatMessage.isLoading,
                                approvalContent = if (chatMessage.requiresApproval) {
                                    {
                                        ApprovalButtons(
                                            onApprove = {
                                                onHandleApprovalDecision(
                                                    ApprovalDecision.APPROVE
                                                )
                                            },
                                            onReject = {
                                                onHandleApprovalDecision(
                                                    ApprovalDecision.REJECT
                                                )
                                            }
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (isConnecting) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        DuoInputBar(
            value = messageText,
            onValueChange = { messageText = it },
            onSend = {
                onSendMessage(messageText)
                messageText = ""
            }
        )
    }
}

@Composable
fun UserMessageBubble(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 64.dp),
        horizontalAlignment = Alignment.End
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp),
            modifier = Modifier.widthIn(min = 40.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp, 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun DuoMessageBubble(
    text: String,
    footer: String,
    isLoading: Boolean = false,
    approvalContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 32.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF8E75C2)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp, 8.dp)) {
                    MarkdownText(
                        markdown = text
                    )
                    if (isLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    approvalContent?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        it()
                    }
                }
            }
            Text(
                text = footer,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun SystemMessage(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ApprovalButtons(
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        OutlinedButton(
            onClick = onReject,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text("Reject")
        }
        Button(
            onClick = onApprove,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF059669) // Success green
            )
        ) {
            Text("Approve")
        }
    }
}

@Composable
fun SuggestedActionItem(icon: ImageVector, text: String, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF8E75C2),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DuoInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    IconButton(onClick = { /* Attachment */ }) {
                        Icon(
                            Icons.Outlined.AddCircleOutline,
                            contentDescription = "Attach",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask Duo anything...", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    IconButton(
                        onClick = onSend,
                        enabled = value.isNotBlank(),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (value.isNotBlank()) Color(0xFF8E75C2) else Color.Transparent,
                            contentColor = if (value.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Duo can make mistakes. Check important info. Learn more",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DuoAvatar() {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Color(0xFF8E75C2)), // Duo purple
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Psychology,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color.White
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DuoChatScreenPreview() {
    LabdroidTheme {
        DuoChatContent(
            messages = listOf(
                ChatMessage(text = "Hello! How can I help you?", isFromUser = false),
                ChatMessage(text = "What is GitLab Duo?", isFromUser = true),
                ChatMessage(
                    text = "GitLab Duo is a suite of AI-powered capabilities...",
                    isFromUser = false
                )
            ),
            isConnecting = false,
            isConnected = true,
            workflowStatus = null,
            onSendMessage = {},
            onClearChat = {},
            onHandleApprovalDecision = {},
            onBackClick = {}
        )
    }
}
