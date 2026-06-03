package `in`.aboobacker.labdroid.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable(with = LabelSerializer::class)
data class Label(
    val id: Long = 0,
    val name: String,
    val color: String = "#808080",
    val description: String? = null,
    @SerialName("text_color")
    val textColor: String? = null
)

object LabelSerializer : KSerializer<Label> {
    @Serializable
    private data class LabelSurrogate(
        val id: Long = 0,
        val name: String,
        val color: String = "#808080",
        val description: String? = null,
        @SerialName("text_color")
        val textColor: String? = null
    )

    override val descriptor: SerialDescriptor = LabelSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): Label {
        val input = decoder as? JsonDecoder ?: throw SerializationException("Expected JsonDecoder")
        val element = input.decodeJsonElement()
        return if (element is JsonPrimitive) {
            Label(name = element.content)
        } else {
            val surrogate = input.json.decodeFromJsonElement<LabelSurrogate>(element)
            Label(
                id = surrogate.id,
                name = surrogate.name,
                color = surrogate.color,
                description = surrogate.description,
                textColor = surrogate.textColor
            )
        }
    }

    override fun serialize(encoder: Encoder, value: Label) {
        val surrogate = LabelSurrogate(
            id = value.id,
            name = value.name,
            color = value.color,
            description = value.description,
            textColor = value.textColor
        )
        encoder.encodeSerializableValue(LabelSurrogate.serializer(), surrogate)
    }
}
