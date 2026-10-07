package com.rescue.flutter_720yun.adoption.models

import com.google.gson.*
import java.lang.reflect.Type
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Some deployed APIs return a comma-separated preview, others an array. */
class PreviewImageAdapter : JsonDeserializer<TopicSummary> {
    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): TopicSummary {
        val normalized=json.asJsonObject.deepCopy()
        val preview=normalized.get("preview_img")
        val first=if(preview?.isJsonArray==true) preview.asJsonArray.mapNotNull {
            runCatching { it.asString.trim().takeIf(String::isNotEmpty) }.getOrNull()
        }.firstOrNull() else runCatching { preview?.asString?.split(',')?.firstOrNull { it.trim().isNotEmpty() }?.trim() }.getOrNull()
        normalized.add("preview_img", first?.let(::JsonPrimitive) ?: JsonNull.INSTANCE)
        return Gson().fromJson(normalized, TopicSummary::class.java)
    }
}
object AdoptionTime {
    fun display(value: String?): String = runCatching {
        OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }.getOrNull() ?: value.orEmpty()
}
