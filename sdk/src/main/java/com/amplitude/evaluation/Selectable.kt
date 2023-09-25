package com.amplitude.experiment.evaluation

internal class SelectableMap(private val map: Map<*, *>) : Selectable {
    override fun select(selector: String): Any? = map[selector]
}

internal interface Selectable {
    fun select(selector: String): Any?

    fun select(selector: List<String?>?): Any? {
        if (selector == null || selector.isEmpty()) {
            return null
        }
        var selectable = this
        for (i in 0 until selector.size - 1) {
            val selectorElement = selector[i] ?: return null
            val value = selectable.select(selectorElement)
            selectable = when (value) {
                is Selectable -> value
                is Map<*, *> -> SelectableMap(value)
                else -> return null
            }
        }
        val lastSelector = selector[selector.size - 1] ?: return null
        return selectable.select(lastSelector)
    }
}
