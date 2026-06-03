package `in`.aboobacker.labdroid

import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// ---------------------------------------------------------------------------
// GitLabWorkflowClient
// ---------------------------------------------------------------------------

/**
 * WebSocket client for the GitLab Duo Agent Platform (DWS).
 *
 * Mirrors the TypeScript GitLabWorkflowClient from gitlab-ai-provider v6.8.0.
 *
 * Dual keepalive:
 *  - OkHttp ws ping every 45 s  (TCP keepalive through proxies)
 *  - JSON heartbeat every 60 s  (prevents DWS workflow timeout)
 *
 * No automatic reconnection on drop — matches gitlab-lsp behaviour.
 *
 * Usage:
 * ```kotlin
 * val client = GitLabWorkflowClient()
 * client.connect(options) { event ->
 *     when (event) {
 *         is WorkflowClientEvent.Checkpoint       -> { … }
 *         is WorkflowClientEvent.ToolRequest      -> client.sendActionResponse(event.requestID, result)
 *         is WorkflowClientEvent.BuiltinToolRequest -> { … }
 *         is WorkflowClientEvent.ApprovalRequired -> { … }
 *         is WorkflowClientEvent.Completed        -> client.close()
 *         is WorkflowClientEvent.Failed           -> { … }
 *         is WorkflowClientEvent.Closed           -> { … }
 *     }
 * }
 * client.sendStartRequest(StartRequest(workflowID = "…", goal = "…"))
 * ```
 */
class GitLabWorkflowClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().build(),
) {
    private val TAG = "GitLabWorkflowClient"
    private var socket: WebSocket? = null
    private var eventCallback: ((WorkflowClientEvent) -> Unit)? = null

    private val closed = AtomicBoolean(false)
    private val cleanedUp = AtomicBoolean(false)
    private var lastSendTime = 0L

    var currentWorkflowId: String? = null
        private set

    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "gitlab-workflow-timer").apply { isDaemon = true }
    }
    private var keepaliveFuture: ScheduledFuture<*>? = null
    private var heartbeatFuture: ScheduledFuture<*>? = null

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Create a new workflow on the GitLab instance.
     */
    suspend fun createWorkflow(
        instanceUrl: String,
        headers: Map<String, String>,
        goal: String,
        projectId: String?,
        namespaceId: String?,
    ): String = suspendCancellableCoroutine { cont ->
        val url = "$instanceUrl/api/v4/ai/duo_workflows/workflows"
        val json = JSONObject().apply {
            put("goal", goal)
            projectId?.let { put("project_id", it) }
            namespaceId?.let { put("namespace_id", it) }
            put("workflow_definition", "chat")
        }

        @Suppress("DEPRECATION")
        val requestBody = okhttp3.RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .apply {
                headers.forEach { (k, v) -> addHeader(k, v) }
            }
            .build()

        okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                cont.resumeWithException(e)
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (!response.isSuccessful) {
                    cont.resumeWithException(Exception("Failed to create workflow: ${response.code}"))
                    return
                }
                val body = response.body?.string() ?: ""
                try {
                    val id = JSONObject(body).getString("id")
                    cont.resume(id)
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            }
        })
    }

    /**
     * Connect to the DWS WebSocket. Suspends until the WS handshake completes.
     * Throws if the connection fails before the open event fires.
     */
    suspend fun connect(
        options: WorkflowWebSocketOptions,
        onEvent: (WorkflowClientEvent) -> Unit,
    ) {
        Log.d(TAG, "Connecting to Duo Workflow at ${options.instanceUrl}")
        validateOptions(options)
        eventCallback = onEvent
        closed.set(false)
        cleanedUp.set(false)

        val wsUrl = buildWebSocketUrl(options)
        Log.d(TAG, "Raw WS URL: $wsUrl")
        val request = buildRequest(options, wsUrl)
        Log.d(TAG, "OkHttp Request URL: ${request.url}")
        Log.d(TAG, "OkHttp Request Scheme: ${request.url.scheme}")

        suspendCancellableCoroutine { cont ->
            var resolved = false

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.i(TAG, "WebSocket Opened: ${response.code} ${response.message}")
                    socket = webSocket
                    resolved = true
                    startKeepalive()
                    startHeartbeat()
                    cont.resume(Unit)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d(TAG, "WS Message (text): $text")
                    handleMessage(text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    val text = bytes.utf8()
                    Log.d(TAG, "WS Message (bytes): $text")
                    handleMessage(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket Failure: ${t.message}", t)
                    response?.let {
                        Log.e(TAG, "WS Failure Response: ${it.code} ${it.message}")
                        try {
                            val body = it.peekBody(1024).string()
                            Log.e(TAG, "WS Failure Body: $body")
                        } catch (e: Exception) {
                            Log.e(TAG, "Could not read error body: ${e.message}")
                        }
                    }
                    cleanup()
                    if (!resolved) {
                        cont.resumeWithException(t)
                    } else {
                        emit(WorkflowClientEvent.Failed(t))
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.w(TAG, "WebSocket Closing: $code / $reason")
                    webSocket.close(code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.w(TAG, "WebSocket Closed: $code / $reason")
                    cleanup()
                    if (!resolved) {
                        cont.resumeWithException(
                            IllegalStateException(
                                "WebSocket closed before open: code=$code reason=$reason"
                            )
                        )
                        return
                    }
                    if (!closed.get()) {
                        emit(WorkflowClientEvent.Closed(code, reason))
                    }
                }
            }

            val ws = okHttpClient.newWebSocket(request, listener)
            cont.invokeOnCancellation {
                Log.d(TAG, "Connection cancelled, closing WS")
                ws.cancel()
                cleanup()
            }
        }
    }

    /**
     * Send a [StartRequest] to begin the workflow.
     * Call after [connect] returns successfully.
     */
    fun sendStartRequest(request: StartRequest) {
        currentWorkflowId = request.workflowID
        send(ClientEvent.Start(request))
    }

    /**
     * Return a tool result to DWS.
     *
     * @param requestID  From [WorkflowClientEvent.ToolRequest] or [WorkflowClientEvent.BuiltinToolRequest]
     * @param response   Serialized result string
     * @param error      Optional error message if the tool failed
     */
    fun sendActionResponse(requestID: String, response: String, error: String? = null) {
        sendHeartbeatIfNeeded()
        send(
            ClientEvent.ActionResp(
                ActionResponsePayload(
                    requestID = requestID,
                    plainTextResponse = PlainTextResponse(response, error),
                )
            )
        )
    }

    /**
     * Send an [ApprovalDecision] back to DWS for a pending tool-call approval.
     * Wraps the decision into a new [StartRequest] on the existing workflow.
     *
     * @param workflowID  The same workflowID used in the original [StartRequest]
     * @param decision    Approval or rejection
     */
    fun sendApprovalDecision(workflowID: String, decision: ApprovalDecision) {
        send(
            ClientEvent.Start(
                StartRequest(
                    workflowID = workflowID,
                    goal = "",
                    approval = decision
                )
            )
        )
    }

    /**
     * Signal the server to stop the workflow gracefully.
     */
    fun stop() {
        send(ClientEvent.Stop)
        closed.set(true)
    }

    /**
     * Close the WebSocket connection. Idempotent — safe to call multiple times.
     */
    fun close() {
        if (!closed.compareAndSet(false, true)) return
        cleanup()
        socket?.close(1000, "Client closing")
        socket = null
    }

    val isConnected: Boolean
        get() = socket != null

    // -----------------------------------------------------------------------
    // Private — validation & request building
    // -----------------------------------------------------------------------

    private fun validateOptions(options: WorkflowWebSocketOptions) {
        require(options.instanceUrl.isNotBlank()) { "instanceUrl is required" }
        val parsed = java.net.URL(options.instanceUrl)
        require(parsed.protocol == "https" || parsed.protocol == "http") {
            "Invalid instanceUrl protocol: ${parsed.protocol}"
        }
        require(parsed.userInfo == null) {
            "instanceUrl must not contain authentication credentials"
        }
        require(options.headers.isNotEmpty()) { "headers are required" }
    }

    private fun buildRequest(options: WorkflowWebSocketOptions, wsUrl: String): Request {
        val builder = Request.Builder().url(wsUrl)

        val h = options.headers.mapKeys { it.key.lowercase() }.toMutableMap()
        h.remove("content-type")

        h["x-gitlab-client-type"] = "duo-workflow-android"
        h["x-gitlab-client-name"] = "gitlab-ai-provider-android"
        h["x-gitlab-duo-workflow-client-type"] = "android-websocket"
        h["x-gitlab-language-server-version"] = CLIENT_VERSION
        h["x-gitlab-duo-workflow-client-version"] = CLIENT_VERSION

        val parsed = java.net.URL(options.instanceUrl)
        h["origin"] = "${parsed.protocol}://${parsed.host}" +
                if (parsed.port != -1) ":${parsed.port}" else ""

        options.requestId?.let { h["x-request-id"] = it }
        options.projectId?.let { h["x-gitlab-project-id"] = it }
        options.namespaceId?.let { h["x-gitlab-namespace-id"] = it }
        options.rootNamespaceId?.let { h["x-gitlab-root-namespace-id"] = it }

        if (!h.containsKey("user-agent")) {
            h["user-agent"] = "GitLab-Duo-Workflow-Android/${CLIENT_VERSION}"
        }

        h.forEach { (k, v) -> builder.addHeader(k, v) }
        return builder.build()
    }

    private fun buildWebSocketUrl(options: WorkflowWebSocketOptions): String {
        val base = options.instanceUrl.trim().trimEnd('/')
        var wsUrl = "$base/api/v4/ai/duo_workflows/ws"

        if (wsUrl.startsWith("https://", ignoreCase = true)) {
            wsUrl = "wss://" + wsUrl.substring(8)
        } else if (wsUrl.startsWith("http://", ignoreCase = true)) {
            wsUrl = "ws://" + wsUrl.substring(7)
        }

        val params = buildList {
            if (options.modelRef.isNotBlank() && options.modelRef != "default")
                add("user_selected_model_identifier=${encode(options.modelRef)}")
            options.aiCatalogItemVersionId?.let { add("ai_catalog_item_version_id=$it") }
            options.workflowDefinition?.let { add("workflow_definition=${encode(it)}") }
            options.projectId?.let { add("project_id=$it") }
            options.namespaceId?.let { add("namespace_id=$it") }
            options.rootNamespaceId?.let { add("root_namespace_id=$it") }
        }

        return if (params.isEmpty()) wsUrl else "$wsUrl?${params.joinToString("&")}"
    }

    private fun encode(v: String) = java.net.URLEncoder.encode(v, "UTF-8")

    // -----------------------------------------------------------------------
    // Private — message handling
    // -----------------------------------------------------------------------

    private fun handleMessage(text: String) {
        try {
            val action = WorkflowAction.fromJson(JSONObject(text))
            handleAction(action)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message: ${e.message}", e)
            emit(WorkflowClientEvent.Failed(e))
        }
    }

    private fun handleAction(action: WorkflowAction) {
        action.newCheckpoint?.let { cp ->
            emit(WorkflowClientEvent.Checkpoint(cp))
            when (cp.status) {
                WorkflowStatus.FINISHED,
                WorkflowStatus.COMPLETED -> { /* emit(WorkflowClientEvent.Completed) */
                }

                WorkflowStatus.FAILED -> emit(
                    WorkflowClientEvent.Failed(
                        Exception(cp.content ?: "Workflow failed")
                    )
                )

                WorkflowStatus.STOPPED,
                WorkflowStatus.CANCELLED -> { /* emit(WorkflowClientEvent.Completed) */
                }

                WorkflowStatus.TOOL_CALL_APPROVAL_REQUIRED ->
                    emit(WorkflowClientEvent.ApprovalRequired(extractApprovalTools(cp)))

                WorkflowStatus.PLAN_APPROVAL_REQUIRED -> { /* emit(WorkflowClientEvent.Completed) */
                }

                else -> Unit  // CREATED / RUNNING / PAUSED — no extra event
            }
            return
        }

        if (action.runMCPTool != null && action.requestID != null) {
            emit(WorkflowClientEvent.ToolRequest(action.requestID, action.runMCPTool))
            return
        }

        if (action.runHTTPRequest != null && action.requestID != null) {
            emit(WorkflowClientEvent.HttpRequest(action.requestID, action.runHTTPRequest))
            return
        }

        // Unsupported built-in tool (file I/O, shell, git) — auto-reject so DWS doesn't hang,
        // then surface the event so the app can log or warn the user.
        if (action.unsupportedToolName != null && action.requestID != null) {
            sendActionResponse(
                requestID = action.requestID,
                response = "",
                error = "Tool '${action.unsupportedToolName}' is not supported on mobile clients.",
            )
            emit(
                WorkflowClientEvent.UnsupportedToolRequest(
                    action.requestID,
                    action.unsupportedToolName
                )
            )
        }
    }

    // -----------------------------------------------------------------------
    // Private — sending
    // -----------------------------------------------------------------------

    private fun send(event: ClientEvent) {
        val json = event.toJson().toString()
        Log.d(TAG, "WS Sending: $json")
        socket?.send(json)
        lastSendTime = System.currentTimeMillis()
    }

    private fun sendHeartbeatIfNeeded() {
        if (System.currentTimeMillis() - lastSendTime >= WS_HEARTBEAT_INTERVAL_MS / 2) {
            send(ClientEvent.HeartbeatEv)
        }
    }

    private fun emit(event: WorkflowClientEvent) = eventCallback?.invoke(event)

    // -----------------------------------------------------------------------
    // Private — keepalive & heartbeat
    // -----------------------------------------------------------------------

    /**
     * OkHttp-level WebSocket ping every 45 s.
     * Keeps the TCP connection alive through proxies / load balancers.
     *
     * Note: the cleanest way is to set pingInterval on the OkHttpClient directly:
     *   OkHttpClient.Builder().pingInterval(45, TimeUnit.SECONDS)
     * We schedule it manually here so the interval follows the same source-of-truth
     * constant (WS_KEEPALIVE_PING_INTERVAL_MS) as the TS original.
     */
    private fun startKeepalive() {
        keepaliveFuture = scheduler.scheduleWithFixedDelay(
            {
                try {
                    // Sending an empty (0-byte) binary frame triggers OkHttp's WS ping
                    socket?.send(ByteString.EMPTY)
                } catch (_: Exception) {
                }
            },
            WS_KEEPALIVE_PING_INTERVAL_MS,
            WS_KEEPALIVE_PING_INTERVAL_MS,
            TimeUnit.MILLISECONDS,
        )
    }

    /**
     * Application-level JSON heartbeat every 60 s.
     * Prevents DWS from timing out the workflow on the server side.
     */
    private fun startHeartbeat() {
        heartbeatFuture = scheduler.scheduleWithFixedDelay(
            { send(ClientEvent.HeartbeatEv) },
            WS_HEARTBEAT_INTERVAL_MS,
            WS_HEARTBEAT_INTERVAL_MS,
            TimeUnit.MILLISECONDS,
        )
    }

    private fun cleanup() {
        if (!cleanedUp.compareAndSet(false, true)) return
        keepaliveFuture?.cancel(false)
        heartbeatFuture?.cancel(false)
        keepaliveFuture = null
        heartbeatFuture = null
    }
}
