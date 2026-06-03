package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val id: Long? = null,
    @SerialName("project_id")
    val projectId: Long,
    @SerialName("action_name")
    val actionName: String,
    @SerialName("target_id")
    val targetId: Long? = null,
    @SerialName("target_iid")
    val targetIid: Long? = null,
    @SerialName("target_type")
    val targetType: String? = null,
    @SerialName("target_title")
    val targetTitle: String? = null,
    @SerialName("note")
    val note: Note? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("author_username")
    val authorUsername: String? = null,
    val author: User? = null,
    @SerialName("push_data")
    val pushData: PushData? = null
)

@Serializable
data class PushData(
    @SerialName("commit_count")
    val commitCount: Int,
    @SerialName("action")
    val action: String,
    @SerialName("ref_type")
    val refType: String,
    @SerialName("ref")
    val ref: String
)
