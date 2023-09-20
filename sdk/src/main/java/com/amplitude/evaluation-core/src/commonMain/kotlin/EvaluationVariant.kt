package com.amplitude.experiment.evaluation

import kotlinx.serialization.Serializable

@Serializable
data class EvaluationVariant(
    val key: String,
    val value: Any? = null,
    val payload: Any? = null,
    val metadata: Map<String, Any?>? = null,
) : Selectable {
    override fun select(selector: String): Any? {
        return when (selector) {
            "key" -> key
            "value" -> value
            "metadata" -> metadata
            else -> null
        }
    }
}
