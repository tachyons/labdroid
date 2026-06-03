package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: Long,
    val name: String,
    val path: String,
    val description: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("web_url")
    val webUrl: String,
    @SerialName("full_name")
    val fullName: String,
    @SerialName("full_path")
    val fullPath: String,
    @SerialName("access_level")
    val accessLevel: Int? = null,
    val visibility: String? = null,
    val permissions: GroupPermissionsWrapper? = null
)

@Serializable
data class GroupPermissionsWrapper(
    @SerialName("user_permissions")
    val userPermissions: GroupPermissions? = null,
    @SerialName("max_access_level")
    val maxAccessLevel: Int? = null
)

@Serializable
data class GroupPermissions(
    val readGroup: Boolean = false,
    val createProjects: Boolean = false,
    val changeGroup: Boolean = false,
    val removeGroup: Boolean = false
)
