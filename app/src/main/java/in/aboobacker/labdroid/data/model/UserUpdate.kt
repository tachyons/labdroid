package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserUpdate(
    val name: String? = null,
    val bio: String? = null,
    val job_title: String? = null,
    val organization: String? = null,
    val location: String? = null,
    val website_url: String? = null,
    val public_email: String? = null
)
