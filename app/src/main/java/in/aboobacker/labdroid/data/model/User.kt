package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val name: String,
    val username: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("web_url")
    val webUrl: String? = null,
    val bio: String? = null,
    @SerialName("job_title")
    val jobTitle: String? = null,
    val organization: String? = null,
    val location: String? = null,
    @SerialName("website_url")
    val websiteUrl: String? = null,
    @SerialName("public_email")
    val publicEmail: String? = null,
    val followers: Int = 0,
    val following: Int = 0,
    @SerialName("star_count")
    val starCount: Int = 0,
    @SerialName("is_followed")
    val isFollowed: Boolean? = null,
    @SerialName("has_duo_access")
    val hasDuoAccess: Boolean = false,
    @SerialName("duo_classic_chat_available")
    val duoClassicChatAvailable: Boolean = false,
    @SerialName("duo_chat_available_features")
    val duoChatAvailableFeatures: List<String> = emptyList()
)
