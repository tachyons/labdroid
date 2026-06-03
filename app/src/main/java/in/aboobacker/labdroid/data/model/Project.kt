package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id: Long,
    val name: String,
    @SerialName("name_with_namespace")
    val nameWithNamespace: String,
    val path: String? = null,
    @SerialName("path_with_namespace")
    val pathWithNamespace: String? = null,
    val description: String? = null,
    @SerialName("web_url")
    val webUrl: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("star_count")
    val starCount: Int = 0,
    @SerialName("forks_count")
    val forksCount: Int = 0,
    @SerialName("last_activity_at")
    val lastActivityAt: String? = null,
    @SerialName("default_branch")
    val defaultBranch: String? = null,
    val visibility: String? = null,
    val namespace: Namespace? = null,
    @SerialName("issues_enabled")
    val issuesEnabled: Boolean = true,
    @SerialName("merge_requests_enabled")
    val mergeRequestsEnabled: Boolean = true,
    @SerialName("wiki_enabled")
    val wikiEnabled: Boolean = true,
    @SerialName("jobs_enabled")
    val jobsEnabled: Boolean = true,
    @SerialName("snippets_enabled")
    val snippetsEnabled: Boolean = true,
    @SerialName("open_issues_count")
    val openIssuesCount: Int = 0,
    val permissions: Permissions? = null
)

@Serializable
data class Permissions(
    @SerialName("project_access")
    val projectAccess: ProjectAccess? = null,
    @SerialName("group_access")
    val groupAccess: ProjectAccess? = null,
    @SerialName("user_permissions")
    val userPermissions: UserPermissions? = null,
    @SerialName("max_access_level")
    val maxAccessLevel: Int? = null
)

@Serializable
data class ProjectAccess(
    @SerialName("access_level")
    val accessLevel: Int,
    @SerialName("notification_level")
    val notificationLevel: Int? = null
)

@Serializable
data class UserPermissions(
    val pushCode: Boolean = false,
    val downloadCode: Boolean = false,
    val adminProject: Boolean = false,
    val adminIssue: Boolean = false,
    val createIssue: Boolean = false,
    val createMergeRequestIn: Boolean = false
)

@Serializable
data class Namespace(
    val id: Long,
    val name: String,
    val path: String,
    val kind: String,
    @SerialName("full_path")
    val fullPath: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("web_url")
    val webUrl: String? = null
)
