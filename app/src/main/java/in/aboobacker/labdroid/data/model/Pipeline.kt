package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Pipeline(
    val id: Long,
    val iid: Long? = null,
    @SerialName("project_id")
    val projectId: Long,
    val status: String,
    val source: String? = null,
    val ref: String,
    val sha: String,
    @SerialName("web_url")
    val webUrl: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    val duration: Double? = null
)
