package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RepositoryItem(
    val id: String,
    val name: String,
    val type: String,
    val path: String,
    val mode: String
)
