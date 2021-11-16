@file:JvmName("JSONUtil")

package com.amplitude.core.util

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

fun JSONObject.toUpdateUserPropertiesMap(): Map<String, Map<String, Any?>> {
    val map = mutableMapOf<String, Map<String, Any?>>()
    for (key in this.keys()) {
        map[key] = when (val value = this[key]) {
            is JSONObject -> value.toMap()
            is JSONArray -> mapOf()
            JSONObject.NULL -> mapOf()
            else -> mapOf()
        }
    }
    return map
}

internal fun JSONObject.toMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    for (key in this.keys()) {
        map[key] = this[key].fromJSON()
    }
    return map
}

internal fun JSONArray.toList(): List<Any?> {
    val list = mutableListOf<Any?>()
    for (i in 0 until this.length()) {
        val value = this[i].fromJSON()
        list.add(value)
    }
    return list
}

private fun Any?.fromJSON(): Any? {
    return when (this) {
        is JSONObject -> this.toMap()
        is JSONArray -> this.toList()
        // org.json uses BigDecimal for doubles and floats; normalize to double
        // to make testing for equality easier.
        is BigDecimal -> this.toDouble()
        JSONObject.NULL -> null
        else -> this
    }
}
