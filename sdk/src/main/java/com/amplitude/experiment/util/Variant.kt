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
        val value = when {
            has("value") -> getString("value")
            has("key") -> getString("key")
            else -> return null
        }
        val payload = when {
            has("payload") -> get("payload")
            else -> null
        }
        Variant(value, payload)
    } catch (e: JSONException) {
        Logger.w("Error parsing Variant from json string $this")
        return null
    }
}
