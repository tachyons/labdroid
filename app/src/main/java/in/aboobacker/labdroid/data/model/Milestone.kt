package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Milestone(
    val id: Long,
    val iid: Long,
    @SerialName("project_id")
    val projectId: Long? = null,
    @SerialName("group_id")
    val groupId: Long? = null,
    val title: String,
    val description: String? = null,
    val state: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("due_date")
    val dueDate: String? = null,
    @SerialName("start_date")
    val startDate: String? = null
)
