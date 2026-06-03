package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupDetail(
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
    @SerialName("parent_id")
    val parentId: Long? = null,
    val statistics: GroupStatistics? = null
)

@Serializable
data class GroupStatistics(
    @SerialName("storage_size")
    val storageSize: Long = 0,
    @SerialName("repository_size")
    val repositorySize: Long = 0,
    @SerialName("wiki_size")
    val wikiSize: Long = 0,
    @SerialName("lfs_objects_size")
    val lfsObjectsSize: Long = 0,
    @SerialName("job_artifacts_size")
    val jobArtifactsSize: Long = 0,
    @SerialName("pipeline_artifacts_size")
    val pipelineArtifactsSize: Long = 0,
    @SerialName("packages_size")
    val packagesSize: Long = 0,
    @SerialName("snippets_size")
    val snippetsSize: Long = 0,
    @SerialName("uploads_size")
    val uploadsSize: Long = 0
)

@Serializable
data class GroupMember(
    val id: Long,
    val username: String,
    val name: String,
    val state: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("web_url")
    val webUrl: String,
    @SerialName("access_level")
    val accessLevel: Int,
    @SerialName("expires_at")
    val expiresAt: String? = null
) {
    val accessLevelName: String
        get() = when (accessLevel) {
            0 -> "None"
            5 -> "Minimal Access"
            10 -> "Guest"
            20 -> "Reporter"
            30 -> "Developer"
            40 -> "Maintainer"
            50 -> "Owner"
            else -> "Unknown"
        }
}
