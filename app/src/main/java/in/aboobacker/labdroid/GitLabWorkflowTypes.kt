package `in`.aboobacker.labdroid

import org.json.JSONObject

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const val CLIENT_VERSION = "8.51.0"
const val WS_HEARTBEAT_INTERVAL_MS = 60_000L
const val WS_KEEPALIVE_PING_INTERVAL_MS = 45_000L
const val STOP_REASON_USER = "USER_ACTION_TRIGGERED_STOP"

// ---------------------------------------------------------------------------
// Enums
// ---------------------------------------------------------------------------

enum class WorkflowType(val value: String) {
    CHAT("chat"),
    SOFTWARE_DEVELOPMENT("software_development");

    companion object {
        fun fromString(value: String?): WorkflowType =
            entries.find { it.value == value } ?: CHAT
    }
}

enum class WorkflowStatus {
    CREATED, RUNNING, PAUSED, FINISHED, COMPLETED, FAILED, STOPPED, CANCELLED,
    TOOL_CALL_APPROVAL_REQUIRED, PLAN_APPROVAL_REQUIRED;

    companion object {
        fun fromString(value: String?): WorkflowStatus =
            entries.find { it.name == value?.uppercase() } ?: RUNNING
    }
}

enum class ApprovalDecision {
    APPROVE, REJECT
}

// ---------------------------------------------------------------------------
// Data Classes
// ---------------------------------------------------------------------------

data class WorkflowWebSocketOptions(
    val instanceUrl: String,
    val modelRef: String = "default",
    val headers: Map<String, String>,
    val requestId: String? = null,
    val projectId: String? = null,
    val namespaceId: String? = null,
    val rootNamespaceId: String? = null,
    val aiCatalogItemVersionId: Int? = null,
    val workflowDefinition: String = WorkflowType.CHAT.value,
)

data class StartRequest(
    val workflowID: String,
    val clientVersion: String = "8.51.0",
    val workflowDefinition: String = "chat",
    val goal: String,
    val workflowMetadata: String? = null,
    val additional_context: List<Map<String, Any>>? = null,
    val clientCapabilities: List<String> = listOf("incremental_streaming", "web_search"),
    val approval: ApprovalDecision? = null,
    val projectId: String? = null,
    val rootNamespaceId: String? = null
)

data class PlainTextResponse(
    val content: String,
    val error: String? = null
)

data class ActionResponsePayload(
    val requestID: String,
    val plainTextResponse: PlainTextResponse
)

data class CheckpointData(
    val status: WorkflowStatus,
    val content: String? = null,
    val rawJson: String? = null
) {
    companion object {
        fun fromCheckpointJson(status: WorkflowStatus, checkpointJson: String?): String? {
            if (checkpointJson == null) return null
            try {
                val json = JSONObject(checkpointJson)
                val channelValues = json.optJSONObject("channel_values")
                val uiChatLog = channelValues?.optJSONArray("ui_chat_log")
                if (uiChatLog != null && uiChatLog.length() > 0) {
                    // Get the last message
                    val lastEntry = uiChatLog.getJSONObject(uiChatLog.length() - 1)
                    return lastEntry.optString("content")
                }
            } catch (e: Exception) {
                // Ignore parsing errors
            }
            return null
        }
    }
}

data class RunMCPTool(
    val name: String,
    val arguments: Map<String, Any?>
)

data class RunHTTPRequest(
    val url: String,
    val method: String,
    val headers: Map<String, String>? = null,
    val body: String? = null
)

class WorkflowAction(
    val newCheckpoint: CheckpointData? = null,
    val runMCPTool: RunMCPTool? = null,
    val runHTTPRequest: RunHTTPRequest? = null,
    val requestID: String? = null,
    val unsupportedToolName: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): WorkflowAction {
            val requestID = json.optString("requestID").takeIf { it.isNotEmpty() }

            val newCheckpoint = json.optJSONObject("newCheckpoint")?.let { cp ->
                val checkpointJson = cp.optString("checkpoint").takeIf { it.isNotEmpty() }
                val status = WorkflowStatus.fromString(cp.optString("status"))
                CheckpointData(
                    status = status,
                    content = CheckpointData.fromCheckpointJson(status, checkpointJson),
                    rawJson = checkpointJson
                )
            }

            val runMCPTool = json.optJSONObject("run_mcp_tool")?.let { mcp ->
                val args = mutableMapOf<String, Any?>()
                mcp.optJSONObject("arguments")?.let { argObj ->
                    argObj.keys().forEach { key ->
                        args[key] = argObj.get(key)
                    }
                }
                RunMCPTool(
                    name = mcp.optString("name"),
                    arguments = args
                )
            }

            val runHTTPRequest = json.optJSONObject("run_http_request")?.let { http ->
                val headers = mutableMapOf<String, String>()
                http.optJSONObject("headers")?.let { hObj ->
                    hObj.keys().forEach { key ->
                        headers[key] = hObj.getString(key)
                    }
                }
                RunHTTPRequest(
                    url = http.optString("url"),
                    method = http.optString("method"),
                    headers = headers,
                    body = http.optString("body").takeIf { it.isNotEmpty() }
                )
            }

            val unsupportedToolName =
                json.optString("unsupported_tool_name").takeIf { it.isNotEmpty() }

            return WorkflowAction(
                newCheckpoint = newCheckpoint,
                runMCPTool = runMCPTool,
                runHTTPRequest = runHTTPRequest,
                requestID = requestID,
                unsupportedToolName = unsupportedToolName
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Events
// ---------------------------------------------------------------------------

sealed class WorkflowClientEvent {
    data class Checkpoint(val checkpoint: CheckpointData) : WorkflowClientEvent()
    data class ToolRequest(val requestID: String, val tool: RunMCPTool) : WorkflowClientEvent()
    data class HttpRequest(val requestID: String, val request: RunHTTPRequest) :
        WorkflowClientEvent()

    data class UnsupportedToolRequest(val requestID: String, val toolName: String) :
        WorkflowClientEvent()

    data class ApprovalRequired(val tools: List<String>) : WorkflowClientEvent()
    object Completed : WorkflowClientEvent()
    data class Failed(val error: Throwable) : WorkflowClientEvent()
    data class Closed(val code: Int, val reason: String) : WorkflowClientEvent()
}

sealed class ClientEvent {
    data class Start(val request: StartRequest) : ClientEvent()
    data class ActionResp(val payload: ActionResponsePayload) : ClientEvent()
    object Stop : ClientEvent()
    object HeartbeatEv : ClientEvent()

    fun toJson(): JSONObject {
        val json = JSONObject()
        when (this) {
            is Start -> {
                val startRequest = JSONObject()
                startRequest.put("workflowID", request.workflowID)
                startRequest.put("clientVersion", request.clientVersion)
                startRequest.put("workflowDefinition", request.workflowDefinition)
                request.workflowMetadata?.let { startRequest.put("workflowMetadata", it) }

                val capabilities = org.json.JSONArray()
                request.clientCapabilities.forEach { capabilities.put(it) }
                startRequest.put("clientCapabilities", capabilities)

                startRequest.put("goal", request.goal)

                if (request.approval != null) {
                    val approvalObj = JSONObject()
                    approvalObj.put("decision", request.approval.name.lowercase())
                    startRequest.put("approval", approvalObj)
                } else {
                    startRequest.put("approval", JSONObject())
                }

                startRequest.put("useOrbit", false)

                request.additional_context?.let { contextList ->
                    val contextArray = org.json.JSONArray()
                    contextList.forEach { contextMap ->
                        val contextObj = JSONObject()
                        contextMap.forEach { (k, v) -> contextObj.put(k, v) }
                        contextArray.put(contextObj)
                    }
                    startRequest.put("additional_context", contextArray)
                }

                json.put("startRequest", startRequest)
            }

            is ActionResp -> {
                val actionResponse = JSONObject()
                actionResponse.put("requestID", this.payload.requestID)
                val resp = JSONObject()
                resp.put("response", this.payload.plainTextResponse.content)
                this.payload.plainTextResponse.error?.let { resp.put("error", it) }
                actionResponse.put("plainTextResponse", resp)
                json.put("actionResponse", actionResponse)
            }

            is Stop -> {
                val stopWorkflow = JSONObject()
                stopWorkflow.put("reason", STOP_REASON_USER)
                json.put("stopWorkflow", stopWorkflow)
            }

            is HeartbeatEv -> {
                val heartbeat = JSONObject()
                heartbeat.put("timestamp", System.currentTimeMillis())
                json.put("heartbeat", heartbeat)
            }
        }
        return json
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

fun extractApprovalTools(checkpoint: CheckpointData): List<String> {
    if (checkpoint.rawJson == null) return emptyList()
    try {
        val json = JSONObject(checkpoint.rawJson)
        val channelValues = json.optJSONObject("channel_values") ?: return emptyList()
        val toolsToApprove = channelValues.optJSONArray("tools_to_approve") ?: return emptyList()

        val tools = mutableListOf<String>()
        for (i in 0 until toolsToApprove.length()) {
            val tool = toolsToApprove.optJSONObject(i)
            tool?.optString("name")?.let { tools.add(it) }
        }
        return tools
    } catch (e: Exception) {
        return emptyList()
    }
}
