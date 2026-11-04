package com.rescue.flutter_720yun.adoption.models

import com.google.gson.*
import java.lang.reflect.Type

/** Both iOS-supported V2 response shapes: flat fields and an `application` object. */
class ApplicationStateAdapter : JsonDeserializer<ApplicationState> {
    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): ApplicationState {
        val root = json.asJsonObject.deepCopy()
        val application = root.get("application")?.takeIf { it.isJsonObject }?.asJsonObject ?: root
        mapOf("application_id" to "application_id", "application_status" to "status", "application_result" to "result", "application_version" to "version").forEach { (target,fallback) ->
            if (!root.has(target) || root[target].isJsonNull) {
                (application.get(target) ?: if (target == "application_version" && application === root) null else application.get(fallback))?.let { root.add(target,it) }
            }
        }
        return Gson().fromJson(root,ApplicationState::class.java)
    }
}
class AdoptionApplicationAdapter : JsonDeserializer<AdoptionApplication> {
    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): AdoptionApplication {
        val root = json.asJsonObject
        val value = root.get("application")?.takeIf { it.isJsonObject } ?: root
        // Delegate via a Gson without this adapter; retain the topic preview compatibility adapter.
        return GsonBuilder().registerTypeAdapter(TopicSummary::class.java,PreviewImageAdapter()).create().fromJson(value,AdoptionApplication::class.java)
    }
}
