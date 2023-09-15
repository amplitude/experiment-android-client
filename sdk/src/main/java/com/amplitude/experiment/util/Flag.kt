package com.amplitude.experiment.util

import com.amplitude.experiment.EvaluationFlag
import com.amplitude.experiment.EvaluationSegment
import com.amplitude.experiment.EvaluationVariant
import org.json.JSONException
import org.json.JSONObject

internal fun JSONObject?.toFlag(): EvaluationFlag? {
    return if (this == null) {
        return null
    } else try {
        val key = when {
            has("key") -> getString("key")
            else -> return null
        }
        val variants = when {
            has("variants") -> getJSONObject("variants").toMap() as Map<String, EvaluationVariant>?
            else -> null
        }
        val segments = when {
            has("segments") -> getJSONArray("segments").toList() as List<EvaluationSegment>?
            else -> null
        }
        val dependencies = when {
            has("dependencies") -> getJSONArray("dependencies").toList() as List<String>?
            else -> null
        }
        val metadata = when {
            has("metadata") -> getJSONObject("metadata").toMap()
            else -> null
        }
        EvaluationFlag(key, variants, segments, dependencies, metadata)
    } catch (e: JSONException) {
        Logger.w("Error parsing Flag from json string $this")
        return null
    }
}
