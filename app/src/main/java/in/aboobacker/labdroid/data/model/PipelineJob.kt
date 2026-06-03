package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PipelineJob(
    val id: Long,
    val status: String,
    val stage: String,
    val name: String,
    val ref: String? = null,
    val tag: Boolean? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("started_at")
    val startedAt: String? = null,
    @SerialName("finished_at")
    val finishedAt: String? = null,
    val duration: Double? = null,
    val user: User? = null,
    @SerialName("web_url")
    val webUrl: String? = null
)
