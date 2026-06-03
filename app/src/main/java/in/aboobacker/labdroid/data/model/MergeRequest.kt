package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MergeRequest(
    val id: Long,
    val iid: Long,
    @SerialName("project_id")
    val projectId: Long,
    val title: String,
    val description: String? = null,
    val state: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    val author: User? = null,
    @SerialName("web_url")
    val webUrl: String,
    @SerialName("source_branch")
    val sourceBranch: String,
    @SerialName("target_branch")
    val targetBranch: String,
    @SerialName("merge_status")
    val mergeStatus: String? = null,
    @SerialName("has_conflicts")
    val hasConflicts: Boolean = false,
    val draft: Boolean = false,
    @SerialName("user_notes_count")
    val userNotesCount: Int = 0,
    val labels: List<Label> = emptyList(),
    val assignees: List<User> = emptyList(),
    val milestone: Milestone? = null,
    @SerialName("head_pipeline")
    val headPipeline: Pipeline? = null
)

@Serializable
data class MergeRequestChanges(
    val iid: Long,
    val changes: List<Change>
)

@Serializable
data class Change(
    @SerialName("old_path")
    val oldPath: String,
    @SerialName("new_path")
    val newPath: String,
    val diff: String,
    @SerialName("new_file")
    val newFile: Boolean = false,
    @SerialName("renamed_file")
    val renamedFile: Boolean = false,
    @SerialName("deleted_file")
    val deletedFile: Boolean = false
)
