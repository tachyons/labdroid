package `in`.aboobacker.labdroid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ChildButton
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.LinearProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import `in`.aboobacker.labdroid.ApprovalDecision
import `in`.aboobacker.labdroid.DuoContextItem
import `in`.aboobacker.labdroid.WorkflowStatus
import `in`.aboobacker.labdroid.ui.viewmodel.ChatMessage
import `in`.aboobacker.labdroid.ui.viewmodel.DuoChatViewModel
import kotlinx.coroutines.launch

@Composable
fun DuoChatScreenWear(
    projectId: Long? = null,
    rootNamespaceId: Long? = null,
    initialGoal: String? = null,
    additionalContext: List<DuoContextItem>? = null,
    viewModel: DuoChatViewModel = hiltViewModel()
) {
    val messages = viewModel.messages
    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val workflowStatus by viewModel.workflowStatus.collectAsStateWithLifecycle()

    LaunchedEffect(projectId, rootNamespaceId, initialGoal, additionalContext) {
        viewModel.setContext(projectId, rootNamespaceId, initialGoal, additionalContext)
    }

    AppScaffold {
        DuoChatContentWear(
            messages = messages,
            isConnecting = isConnecting,
            isConnected = isConnected,
            workflowStatus = workflowStatus,
            onSendMessage = { viewModel.sendMessage(it) },
            onClearChat = { viewModel.clearChat() },
            onHandleApprovalDecision = { viewModel.handleApprovalDecision(it) }
        )
    }
}

@Composable
fun DuoChatContentWear(
    messages: List<ChatMessage>,
    isConnecting: Boolean,
    isConnected: Boolean,
    workflowStatus: WorkflowStatus?,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onHandleApprovalDecision: (ApprovalDecision) -> Unit
) {
    val listState = rememberScalingLazyListState()
    var inputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(
                onClick = { 
                    coroutineScope.launch {
                        // Scroll to the input field and request focus
                        listState.scrollToItem(messages.size + 5) 
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                }
            ) {
                Text("Ask Duo")
            }
        }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 20.dp)
        ) {
            if (isConnecting) {
                item {
                    CircularProgressIndicator(modifier = Modifier.padding(20.dp))
                }
            } else if (messages.isEmpty()) {
                item {
                    ListHeader {
                        Text(
                            "GitLab Duo",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                item {
                    Text(
                        "Hi! I'm your AI assistant. How can I help you today?",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                items(messages.size) { index ->
                    val message = messages[index]
                    DuoMessageCard(
                        message = message,
                        onApprove = { onHandleApprovalDecision(ApprovalDecision.APPROVE) },
                        onReject = { onHandleApprovalDecision(ApprovalDecision.REJECT) }
                    )
                }
            }

            if (workflowStatus != null && !isConnected) {
                item {
                    Text(
                        "Disconnected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(40.dp)
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    }),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (inputText.isEmpty()) {
                                Text(
                                    "Type message...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            item {
                ChildButton(
                    onClick = onClearChat,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Text("Clear Chat")
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(40.dp)) // Space for EdgeButton
            }
        }
    }
}

@Composable
fun DuoMessageCard(
    message: ChatMessage,
    onApprove: () -> Unit = {},
    onReject: () -> Unit = {}
) {
    AppCard(
        onClick = {},
        appName = { Text(if (message.isFromUser) "You" else "GitLab Duo") },
        title = { },
        time = { Text("now") },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
            
            if (message.requiresApproval) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text("Reject", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Approve", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (message.isLoading) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { 0.5f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun DuoChatScreenWearPreview() {
    MaterialTheme {
        DuoChatContentWear(
            messages = listOf(
                ChatMessage(text = "Hello! How can I help you?", isFromUser = false),
                ChatMessage(text = "What is the status of my project?", isFromUser = true),
                ChatMessage(text = "Your project is on track.", isFromUser = false)
            ),
            isConnecting = false,
            isConnected = true,
            workflowStatus = null,
            onSendMessage = {},
            onClearChat = {},
            onHandleApprovalDecision = {}
        )
    }
}
