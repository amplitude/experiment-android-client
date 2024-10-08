package com.amplitude.experiment.evaluation

import kotlinx.serialization.Serializable

@Serializable
internal class EvaluationContext : MutableMap<String, Any?> by LinkedHashMap(), Selectable {
    override fun select(selector: String): Any? = this[selector]
}
