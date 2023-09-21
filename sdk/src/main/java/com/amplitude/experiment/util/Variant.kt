package com.amplitude.experiment.util

import com.amplitude.experiment.Variant
import org.json.JSONException
import org.json.JSONObject

internal fun Variant.toJson(): String {
    val jsonObject = JSONObject()
    try {
        jsonObject.put("key", key)
        if (value != null) {
            jsonObject.put("value", value)
        }
        if (payload != null) {
            jsonObject.put("payload", payload)
        }
        if (expKey != null) {
            jsonObject.put("expKey", expKey)
        }
        if (metadata != null) {
            jsonObject.put("metadata", metadata)
        }
    } catch (e: JSONException) {
        Logger.w("Error converting Variant to json string", e)
    }
    return jsonObject.toString()
}

internal fun String?.toVariant(): Variant? {
    return if (this == null) {
        return null
    } else {
        JSONObject(this).toVariant()
    }
}

internal fun JSONObject?.toVariant(): Variant? {
    return if (this == null) {
        return null
    } else try {
        var key = when {
            has("key") -> getString("key")
            else -> null
        }
        val value = when {
            has("value") -> getString("value")
            else -> null
        }

        if (key == null && value == null) {
            return null
        }
        if (key == null && value != null) {
            key = value
        }

        val payload = when {
            has("payload") -> get("payload")
            else -> null
        }
        var expKey = when {
            has("expKey") -> getString("expKey")
            else -> null
        }
        var metadata = when {
            has("metadata") -> getJSONObject("metadata").toMap()
            else -> null
        }?.toMutableMap()
        if (metadata != null && metadata["experimentKey"] != null) {
            expKey = metadata["experimentKey"] as? String
        } else if (expKey != null) {
            metadata = metadata ?: HashMap()
            metadata["experimentKey"] = expKey
        }

        Variant(value, payload, expKey, key, metadata)
    } catch (e: JSONException) {
        Logger.w("Error parsing Variant from json string $this")
        return null
    }
}
