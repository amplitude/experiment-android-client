package com.amplitude.experiment.util

import com.amplitude.experiment.Variant
import org.json.JSONException
import org.json.JSONObject

internal fun Variant.toJson(): String {
    val jsonObject = JSONObject()
    try {
        jsonObject.put("value", value)
        if (payload != null) {
            jsonObject.put("payload", payload)
        }
        if (expKey != null) {
            jsonObject.put("expKey", expKey)
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
        val key = when {
            has("key") -> getString("key")
            else -> return null
        }
        val value = when {
            has("value") -> getString("value")
            else -> null
        }
        val payload = when {
            has("payload") -> getString("payload")
            else -> null
        }
        val expKey = when {
            has("expKey") -> getString("expKey")
            else -> null
        }
        val metadata = when {
            has("metadata") -> getJSONObject("metadata").toMap()
            else -> emptyMap()
        }
        Variant(key, value, payload, expKey, metadata)
    } catch (e: JSONException) {
        Logger.w("Error parsing Variant from json string $this")
        return null
    }
}
