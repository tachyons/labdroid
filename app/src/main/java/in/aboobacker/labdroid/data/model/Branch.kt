package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Branch(
    val name: String,
    val merged: Boolean,
    @SerialName("protected")
    val isProtected: Boolean,
    @SerialName("default")
    val isDefault: Boolean,
    @SerialName("web_url")
    val webUrl: String
)
