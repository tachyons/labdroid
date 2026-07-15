package `in`.aboobacker.labdroid.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import dagger.hilt.android.AndroidEntryPoint
import `in`.aboobacker.labdroid.BuildConfig
import `in`.aboobacker.labdroid.GitLabWorkflowClient
import `in`.aboobacker.labdroid.StartRequest
import `in`.aboobacker.labdroid.WorkflowWebSocketOptions
import `in`.aboobacker.labdroid.data.local.AuthPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DuoReplyReceiver : BroadcastReceiver() {

    @Inject
    lateinit var workflowClient: GitLabWorkflowClient

    @Inject
    lateinit var authPreferences: AuthPreferences

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence(NotificationHelper.KEY_TEXT_REPLY)?.toString()

        if (replyText != null) {
            // Update notification to show it's sending
            notificationHelper.showDuoChatNotification("Sending: $replyText")

            scope.launch {
                try {
                    val token = authPreferences.getAccessToken() ?: return@launch
                    val instanceUrl = BuildConfig.GITLAB_BASE_URL
                    val headers = mapOf("Authorization" to "Bearer $token")

                    // We need a workflow ID. For simplicity in this demo/impl, 
                    // we'll create a new one if not connected, but ideally we'd persist the current one.
                    // Given Duo Chat's nature, a new workflow for a quick reply from notification is often acceptable
                    // if the session isn't easily restorable here.
                    
                    val workflowId = workflowClient.createWorkflow(
                        instanceUrl = instanceUrl,
                        headers = headers,
                        goal = replyText,
                        projectId = null, // Could be persisted/passed in intent
                        namespaceId = null
                    )

                    val options = WorkflowWebSocketOptions(
                        instanceUrl = instanceUrl,
                        headers = headers
                    )

                    workflowClient.connect(options) { _ ->
                        // Handle events if needed, but for reply we mostly care about starting it
                    }

                    workflowClient.sendStartRequest(
                        StartRequest(
                            workflowID = workflowId,
                            goal = replyText,
                            workflowDefinition = "chat"
                        )
                    )
                } catch (e: Exception) {
                    notificationHelper.showDuoChatNotification("Error sending reply: ${e.message}")
                }
            }
        }
    }
}
