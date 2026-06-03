package `in`.aboobacker.labdroid.util

import android.net.Uri

sealed class GitLabLink {
    data class Issue(val projectId: Long? = null, val projectPath: String? = null, val iid: Long) :
        GitLabLink()

    data class MergeRequest(
        val projectId: Long? = null,
        val projectPath: String? = null,
        val iid: Long
    ) : GitLabLink()

    data class User(val username: String) : GitLabLink()
    data class Epic(val groupPath: String, val iid: Long) : GitLabLink()
    data class Commit(
        val projectId: Long? = null,
        val projectPath: String? = null,
        val sha: String
    ) : GitLabLink()

    data class External(val url: String) : GitLabLink()
}

object LinkUtils {
    private val ISSUE_REF_REGEX = Regex("(([\\w.-]+/[\\w.-]+)#(\\d+))|(#(\\d+))")
    private val MR_REF_REGEX = Regex("(([\\w.-]+/[\\w.-]+)!(\\d+))|(!(\\d+))")
    private val USER_REF_REGEX = Regex("@([\\w.-]+)")
    private val EPIC_REF_REGEX = Regex("(([\\w.-]+/[\\w.-]+)&(\\d+))|(&(\\d+))")

    fun parseReference(ref: String, currentProjectId: Long? = null): GitLabLink? {
        return when {
            ref.startsWith("#") || (ref.contains("#") && !ref.startsWith("http")) -> {
                ISSUE_REF_REGEX.find(ref)?.let { match ->
                    val projectPath = match.groups[2]?.value
                    val iid = match.groups[3]?.value?.toLongOrNull()
                        ?: match.groups[5]?.value?.toLongOrNull()
                    if (iid != null) {
                        GitLabLink.Issue(
                            projectId = if (projectPath == null) currentProjectId else null,
                            projectPath = projectPath,
                            iid = iid
                        )
                    } else null
                }
            }

            ref.startsWith("!") || (ref.contains("!") && !ref.startsWith("http")) -> {
                MR_REF_REGEX.find(ref)?.let { match ->
                    val projectPath = match.groups[2]?.value
                    val iid = match.groups[3]?.value?.toLongOrNull()
                        ?: match.groups[5]?.value?.toLongOrNull()
                    if (iid != null) {
                        GitLabLink.MergeRequest(
                            projectId = if (projectPath == null) currentProjectId else null,
                            projectPath = projectPath,
                            iid = iid
                        )
                    } else null
                }
            }

            ref.startsWith("@") -> {
                USER_REF_REGEX.find(ref)?.let { match ->
                    GitLabLink.User(match.groups[1]!!.value)
                }
            }

            ref.startsWith("&") || (ref.contains("&") && !ref.startsWith("http")) -> {
                EPIC_REF_REGEX.find(ref)?.let { match ->
                    val groupPath =
                        match.groups[2]?.value ?: "" // This is tricky without more context
                    val iid = match.groups[3]?.value?.toLongOrNull()
                        ?: match.groups[5]?.value?.toLongOrNull()
                    if (iid != null) {
                        GitLabLink.Epic(groupPath, iid)
                    } else null
                }
            }

            else -> null
        }
    }

    fun parseUrl(url: String): GitLabLink {
        val uri = Uri.parse(url)
        val pathSegments = uri.pathSegments

        // Patterns:
        // /group/project/-/issues/123
        // /group/project/-/merge_requests/123
        // /users/username
        // /username

        if (pathSegments.size >= 4 && pathSegments.contains("-")) {
            val dashIndex = pathSegments.indexOf("-")
            val projectPath = pathSegments.subList(0, dashIndex).joinToString("/")
            val type = pathSegments[dashIndex + 1]
            val iid = pathSegments.getOrNull(dashIndex + 2)?.toLongOrNull()

            if (iid != null) {
                return when (type) {
                    "issues" -> GitLabLink.Issue(projectPath = projectPath, iid = iid)
                    "merge_requests" -> GitLabLink.MergeRequest(
                        projectPath = projectPath,
                        iid = iid
                    )

                    "epics" -> GitLabLink.Epic(groupPath = projectPath, iid = iid)
                    else -> GitLabLink.External(url)
                }
            }
        }

        if (pathSegments.size == 2 && pathSegments[0] == "users") {
            return GitLabLink.User(pathSegments[1])
        }

        if (pathSegments.size == 1) {
            return GitLabLink.User(pathSegments[0])
        }

        return GitLabLink.External(url)
    }
}
