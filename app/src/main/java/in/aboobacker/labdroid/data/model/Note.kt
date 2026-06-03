package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class AwardEmoji(
    val id: Long,
    val name: String,
    val user: User,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("awardable_id")
    val awardableId: Long? = null,
    @SerialName("awardable_type")
    val awardableType: String? = null
)

@Serializable
data class Note @OptIn(ExperimentalTime::class) constructor(
    val id: Long,
    val body: String,
    val attachment: String? = null,
    val author: User,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("updated_at")
    val updatedAt: Instant,
    val system: Boolean,
    @SerialName("noteable_id")
    val noteableId: Long,
    @SerialName("noteable_type")
    val noteableType: String,
    @SerialName("noteable_iid")
    val noteableIid: Long? = null,
    @SerialName("discussion_id")
    val discussionId: String? = null,
    val type: String? = null,
    val confidential: Boolean = false,
    val internal: Boolean = false,
    val resolvable: Boolean = false,
    val resolved: Boolean = false,
    @SerialName("award_emoji")
    val awardEmoji: List<AwardEmoji> = emptyList()
)
