package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RepositoryFile(
    @SerialName("file_name")
    val fileName: String,
    @SerialName("file_path")
    val filePath: String,
    val size: Int,
    val encoding: String,
    val content: String,
    val ref: String,
    @SerialName("blob_id")
    val blobId: String,
    @SerialName("commit_id")
    val commitId: String,
    @SerialName("last_commit_id")
    val lastCommitId: String
)
