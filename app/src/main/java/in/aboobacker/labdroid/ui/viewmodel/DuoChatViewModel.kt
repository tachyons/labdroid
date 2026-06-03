package `in`.aboobacker.labdroid.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.aboobacker.labdroid.ApprovalDecision
import `in`.aboobacker.labdroid.DuoContextItem
import `in`.aboobacker.labdroid.GitLabWorkflowClient
import `in`.aboobacker.labdroid.StartRequest
import `in`.aboobacker.labdroid.WorkflowClientEvent
import `in`.aboobacker.labdroid.WorkflowStatus
import `in`.aboobacker.labdroid.WorkflowWebSocketOptions
import `in`.aboobacker.labdroid.data.local.AuthPreferences
import `in`.aboobacker.labdroid.extractApprovalTools
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val isSystem: Boolean = false,
    val requiresApproval: Boolean = false,
    val toolNames: List<String> = emptyList()
)

@HiltViewModel
class DuoChatViewModel @Inject constructor(
    private val workflowClient: GitLabWorkflowClient,
    private val authPreferences: AuthPreferences
) : ViewModel() {

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> get() = _messages

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting

    private val _workflowStatus = MutableStateFlow<WorkflowStatus?>(null)
    val workflowStatus: StateFlow<WorkflowStatus?> = _workflowStatus.asStateFlow()

    private val _pendingApprovalTools = MutableStateFlow<List<String>>(emptyList())
    val pendingApprovalTools: StateFlow<List<String>> = _pendingApprovalTools.asStateFlow()

    private var currentProjectId: Long? = null
    private var currentRootNamespaceId: Long? = null
    private var currentAdditionalContext: List<DuoContextItem>? = null

    fun setContext(
        projectId: Long?,
        rootNamespaceId: Long?,
        initialGoal: String? = null,
        additionalContext: List<DuoContextItem>? = null
    ) {
        currentProjectId = projectId
        currentRootNamespaceId = rootNamespaceId
        currentAdditionalContext = additionalContext

        if (initialGoal != null && _messages.isEmpty()) {
            sendMessage(initialGoal)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(text = text, isFromUser = true)
        _messages.add(userMessage)

        viewModelScope.launch {
            // Ensure previous connection is closed before creating a new one
            workflowClient.close()
            connectAndStart(text)
        }
    }

    private suspend fun connectAndStart(initialGoal: String) {
        _isConnecting.value = true
        val token = authPreferences.getAccessToken() ?: run {
            _isConnecting.value = false
            _messages.add(
                ChatMessage(
                    text = "Error: Not authenticated",
                    isFromUser = false,
                    isSystem = true
                )
            )
            return
        }

        val instanceUrl = "https://gitlab.com"
        val headers = mapOf("Authorization" to "Bearer $token")

        try {
            val workflowId = workflowClient.createWorkflow(
                instanceUrl = instanceUrl,
                headers = headers,
                goal = initialGoal,
                projectId = currentProjectId?.toString(),
                namespaceId = null
            )

            val options = WorkflowWebSocketOptions(
                instanceUrl = instanceUrl,
                headers = headers,
                projectId = currentProjectId?.toString(),
                rootNamespaceId = currentRootNamespaceId?.toString()
            )

            workflowClient.connect(options) { event ->
                handleWorkflowEvent(event)
            }
            _isConnected.value = true
            _isConnecting.value = false
            startWorkflow(initialGoal, workflowId)
        } catch (e: Exception) {
            _isConnecting.value = false
            _messages.add(
                ChatMessage(
                    text = "Failed to initialize workflow: ${e.message}",
                    isFromUser = false,
                    isSystem = true
                )
            )
        }
    }

    private fun startWorkflow(goal: String, workflowId: String) {
        val aiMessage = ChatMessage(text = "...", isFromUser = false, isLoading = true)
        _messages.add(aiMessage)

        val additionalContextMaps = currentAdditionalContext?.map { item ->
            mutableMapOf<String, Any>().apply {
                put("category", item.category)
                item.id?.let { put("id", it) }
                item.content?.let { put("content", it) }

                // metadata must be a JSON string
                val metadataJson = if (item.metadata != null) {
                    JSONObject(item.metadata).toString()
                } else {
                    "{}"
                }
                put("metadata", metadataJson)
            }
        }

        workflowClient.sendStartRequest(
            StartRequest(
                workflowID = workflowId,
                goal = goal,
                workflowDefinition = "chat",
                projectId = currentProjectId?.toString(),
                rootNamespaceId = currentRootNamespaceId?.toString(),
                additional_context = additionalContextMaps
            )
        )
    }

    private fun handleWorkflowEvent(event: WorkflowClientEvent) {
        Log.d("DuoChatViewModel", "Processing event: $event")
        viewModelScope.launch {
            when (event) {
                is WorkflowClientEvent.Checkpoint -> {
                    _workflowStatus.value = event.checkpoint.status
                    val content = event.checkpoint.content
                    if (content != null) {
                        updateLastAiMessage(content)
                    }
                    if (event.checkpoint.status == WorkflowStatus.TOOL_CALL_APPROVAL_REQUIRED) {
                        val tools = extractApprovalTools(event.checkpoint)
                        _pendingApprovalTools.value = tools

                        // Update the last AI message (the one showing "...") to show approval UI
                        val lastIndex = _messages.indexOfLast { !it.isFromUser && !it.isSystem }
                        if (lastIndex != -1) {
                            _messages[lastIndex] = _messages[lastIndex].copy(
                                text = content ?: "Duo needs your approval to run some tools.",
                                isLoading = false,
                                requiresApproval = true,
                                toolNames = tools
                            )
                        }
                    }
                }

                WorkflowClientEvent.Completed -> {
                    _workflowStatus.value = WorkflowStatus.COMPLETED
                    finalizeLastAiMessage()
                }

                is WorkflowClientEvent.Failed -> {
                    _workflowStatus.value = WorkflowStatus.FAILED
                    _messages.add(
                        ChatMessage(
                            text = "Error: ${event.error.message}",
                            isFromUser = false,
                            isSystem = true
                        )
                    )
                }

                is WorkflowClientEvent.Closed -> {
                    if (!_isConnecting.value) {
                        _isConnected.value = false
                        _workflowStatus.value = null
                    }
                }

                is WorkflowClientEvent.ToolRequest -> {
                    _messages.add(
                        ChatMessage(
                            text = "[System] Running tool: ${event.tool.name}",
                            isFromUser = false,
                            isSystem = true
                        )
                    )
                }

                is WorkflowClientEvent.HttpRequest -> {
                    _messages.add(
                        ChatMessage(
                            text = "[System] HTTP Request: ${event.request.method} ${event.request.url}",
                            isFromUser = false,
                            isSystem = true
                        )
                    )
                }

                is WorkflowClientEvent.UnsupportedToolRequest -> {
                    _messages.add(
                        ChatMessage(
                            text = "[System] Unsupported tool requested: ${event.toolName}",
                            isFromUser = false,
                            isSystem = true
                        )
                    )
                }

                is WorkflowClientEvent.ApprovalRequired -> {
                    // This is often redundant with Checkpoint status, only add if we don't have tools yet
                    if (_pendingApprovalTools.value.isEmpty() && event.tools.isNotEmpty()) {
                        _pendingApprovalTools.value = event.tools
                        _messages.add(
                            ChatMessage(
                                text = "Duo needs your approval to run the following tools: ${event.tools.joinToString()}",
                                isFromUser = false,
                                isSystem = false,
                                requiresApproval = true,
                                toolNames = event.tools
                            )
                        )
                    }
                }
            }
        }
    }

    fun handleApprovalDecision(decision: ApprovalDecision) {
        val workflowId = workflowClient.currentWorkflowId ?: return
        _pendingApprovalTools.value = emptyList()

        // Mark the last approval message as handled
        val lastIndex = _messages.indexOfLast { it.requiresApproval }
        if (lastIndex != -1) {
            _messages[lastIndex] = _messages[lastIndex].copy(requiresApproval = false)
        }

        viewModelScope.launch {
            workflowClient.sendApprovalDecision(workflowId, decision)
        }
    }

    private fun updateLastAiMessage(newText: String) {
        val lastIndex = _messages.indexOfLast { !it.isFromUser && !it.isSystem }
        if (lastIndex != -1) {
            val lastMsg = _messages[lastIndex]
            _messages[lastIndex] = lastMsg.copy(text = newText, isLoading = false)
        }
    }

    private fun finalizeLastAiMessage() {
        val lastIndex = _messages.indexOfLast { !it.isFromUser && !it.isSystem }
        if (lastIndex != -1) {
            val lastMsg = _messages[lastIndex]
            _messages[lastIndex] = lastMsg.copy(isLoading = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        workflowClient.close()
    }

    fun clearChat() {
        workflowClient.close()
        _messages.clear()
        _workflowStatus.value = null
        _isConnected.value = false
        _isConnecting.value = false
    }
}
