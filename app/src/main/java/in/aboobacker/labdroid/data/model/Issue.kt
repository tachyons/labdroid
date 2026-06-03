package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Issue(
    val id: Long,
    val iid: Long,
    @SerialName("project_id")
    val projectId: Long,
    val title: String,
    val description: String? = null,
    val state: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    val labels: List<Label> = emptyList(),
    val author: User? = null,
    val assignees: List<User> = emptyList(),
    @SerialName("web_url")
    val webUrl: String,
    val milestone: Milestone? = null,
    val weight: Int? = null,
    val confidential: Boolean = false,
    @SerialName("user_notes_count")
    val userNotesCount: Int = 0
)
