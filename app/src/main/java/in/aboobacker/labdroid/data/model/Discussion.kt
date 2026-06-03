package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Discussion(
    val id: String,
    @SerialName("individual_note")
    val individualNote: Boolean,
    val notes: List<Note>
)
