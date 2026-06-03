package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Commit(
    val id: String,
    @SerialName("short_id")
    val shortId: String,
    val title: String,
    val message: String? = null,
    @SerialName("author_name")
    val authorName: String,
    @SerialName("author_email")
    val authorEmail: String,
    @SerialName("authored_date")
    val authoredDate: String,
    @SerialName("committer_name")
    val committerName: String,
    @SerialName("committer_email")
    val committerEmail: String,
    @SerialName("committed_date")
    val committedDate: String,
    val stats: CommitStats? = null
)

@Serializable
data class CommitStats(
    val additions: Int,
    val deletions: Int,
    val total: Int
)
