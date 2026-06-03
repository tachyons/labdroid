package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MarkdownUpload(
    val id: Long? = null,
    val alt: String,
    val url: String,
    val fullPath: String? = null,
    val markdown: String
)
