package com.amplitude.experiment.util

import com.amplitude.experiment.evaluation.EvaluationFlag
import com.amplitude.experiment.evaluation.EvaluationSegment
import com.amplitude.experiment.evaluation.EvaluationVariant
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
        val variants = getJSONObject("variants").toMap() as Map<String, EvaluationVariant>

        val segments = getJSONArray("segments").toList() as List<EvaluationSegment>

        val dependencies = when {
            has("dependencies") -> getJSONArray("dependencies").toList() as Set<String>
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
