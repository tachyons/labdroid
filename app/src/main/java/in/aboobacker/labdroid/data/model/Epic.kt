package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Epic(
    val id: Long,
    val iid: Long,
    @SerialName("group_id")
    val groupId: Long,
    val title: String,
    val description: String? = null,
    val state: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("start_date")
    val startDate: String? = null,
    @SerialName("end_date")
    val endDate: String? = null,
    val labels: List<Label> = emptyList(),
    val author: User? = null,
    @SerialName("web_url")
    val webUrl: String
)

@Serializable
data class EpicIssuesStatistics(
    val opened: Int,
    val closed: Int,
    val total: Int
)
