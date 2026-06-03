package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val id: Long,
    val title: String? = null,
    val name: String? = null,
    val description: String? = null,
    @SerialName("web_url")
    val webUrl: String? = null,
    val type: String? = null,
    @SerialName("project_id")
    val projectId: Long? = null,
    val iid: Long? = null,
    val state: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val username: String? = null
)
