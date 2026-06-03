package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IssueRequest(
    val title: String? = null,
    val description: String? = null,
    @SerialName("state_event")
    val stateEvent: String? = null, // "close" or "reopen"
    val labels: String? = null,
    @SerialName("assignee_ids")
    val assigneeIds: List<Long>? = null,
    @SerialName("milestone_id")
    val milestoneId: Long? = null,
    val weight: Int? = null,
    val confidential: Boolean? = null
)
