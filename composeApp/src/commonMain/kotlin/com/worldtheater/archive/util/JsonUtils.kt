package com.worldtheater.archive.util

import com.worldtheater.archive.data.local.Note
import com.worldtheater.archive.util.log.L
import kotlinx.serialization.json.*

object JsonUtils {

    private const val TAG = "JsonUtils"
    private val json = Json { ignoreUnknownKeys = true }

    class JsonParseException(message: String, cause: Throwable? = null) :
        RuntimeException(message, cause)

    fun toJson(value: Any?): String {
        return when (value) {
            is Note -> json.encodeToString(value)
            else -> throw IllegalArgumentException("Unsupported JSON serialization type: ${value?.let { it::class.simpleName } ?: "null"}")
        }
    }

    fun parseJsonArrayOfObjects(jsonString: String): List<Map<String, Any?>> {
        val root = try {
            json.parseToJsonElement(jsonString)
        } catch (e: Exception) {
            L.e(TAG, "parseJsonArrayOfObjects failed", e)
            throw JsonParseException("Invalid JSON", e)
        }
        val array = root as? JsonArray ?: throw JsonParseException("Not a JSON Array")
        return array.mapNotNull { entry ->
            val obj = entry as? JsonObject ?: return@mapNotNull null
            obj.mapValues { (_, v) -> v.toAny() }
        }
    }

    fun getString(obj: Map<String, Any?>, key: String): String? {
        val value = obj[key] ?: return null
        return when (value) {
            is String -> value
            else -> value.toString()
        }
    }

    fun getInt(obj: Map<String, Any?>, key: String): Int? {
        val value = obj[key] ?: return null
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    fun getLong(obj: Map<String, Any?>, key: String): Long? {
        val value = obj[key] ?: return null
        return when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }
    }

    fun getBoolean(obj: Map<String, Any?>, key: String): Boolean? {
        val value = obj[key] ?: return null
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> when (value.lowercase()) {
                "true", "1", "yes", "y" -> true
                "false", "0", "no", "n" -> false
                else -> null
            }

            else -> null
        }
    }

    fun keys(obj: Map<String, Any?>): Set<String> = obj.keys

    fun value(obj: Map<String, Any?>, key: String): Any? = obj[key]

    private fun JsonElement.toAny(): Any? {
        return when (this) {
            JsonNull -> null
            is JsonPrimitive -> {
                booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
            }

            is JsonObject -> this.mapValues { (_, v) -> v.toAny() }
            is JsonArray -> this.map { it.toAny() }
        }
    }
}
