package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Todo(
    val id: Long,
    val project: Project? = null,
    val author: User? = null,
    @SerialName("action_name")
    val actionName: String? = null,
    @SerialName("target_type")
    val targetType: String? = null,
    val target: TodoTarget? = null,
    @SerialName("target_url")
    val targetUrl: String? = null,
    val body: String? = null,
    val state: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class TodoTarget(
    val id: Long? = null,
    val iid: Long? = null,
    @SerialName("project_id")
    val projectId: Long? = null,
    val title: String? = null,
    @SerialName("web_url")
    val webUrl: String? = null
)
