package com.amplitude.experiment.evaluation

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray

internal interface EvaluationEngine {
    fun evaluate(
        context: EvaluationContext,
        flags: List<EvaluationFlag>
    ): Map<String, EvaluationVariant>
}

internal class EvaluationEngineImpl(private val log: Logger? = DefaultLogger()) : EvaluationEngine {

    data class EvaluationTarget(
        val context: EvaluationContext,
        val result: MutableMap<String, EvaluationVariant>
    ) : Selectable {
        override fun select(selector: String): Any? {
            return when (selector) {
                "context" -> context
                "result" -> result
                else -> null
            }
        }
    }

    override fun evaluate(
        context: EvaluationContext,
        flags: List<EvaluationFlag>
    ): Map<String, EvaluationVariant> {
        log?.debug { "Evaluating flags ${flags.map { it.key }} with context $context." }
        val results: MutableMap<String, EvaluationVariant> = mutableMapOf()
        val target = EvaluationTarget(context, results)
        for (flag in flags) {
            // Evaluate flag and update results.
            val variant = evaluateFlag(target, flag)
            if (variant != null) {
                results[flag.key] = variant
            } else {
                log?.debug { "Flag ${flag.key} evaluation returned a null result." }
            }
        }
        log?.debug { "Evaluation completed. $results" }
        return results
    }

    private fun evaluateFlag(target: EvaluationTarget, flag: EvaluationFlag): EvaluationVariant? {
        log?.verbose { "Evaluating flag $flag with target $target." }
        var result: EvaluationVariant? = null
        for (segment in flag.segments) {
            result = evaluateSegment(target, flag, segment)
            if (result != null) {
                // Merge all metadata into the result
                val metadata = mergeMetadata(flag.metadata, segment.metadata, result.metadata)
                result = EvaluationVariant(result.key, result.value, result.payload, metadata)
                log?.verbose { "Flag evaluation returned result $result on segment $segment." }
                break
            }
        }
        return result
    }

    private fun evaluateSegment(
        target: EvaluationTarget,
        flag: EvaluationFlag,
        segment: EvaluationSegment
    ): EvaluationVariant? {
        log?.verbose { "Evaluating segment $segment with target $target." }
        if (segment.conditions == null) {
            log?.verbose { "Segment conditions are null, bucketing target." }
            // Null conditions always match
            val variantKey = bucket(target, segment)
            return flag.variants[variantKey]
        }
        // Outer list logic is "or" (||)
        for (conditions in segment.conditions) {
            var match = true
            // Inner list logic is "and" (&&)
            for (condition in conditions) {
                match = matchCondition(target, condition)
                if (!match) {
                    log?.verbose { "Segment condition $condition did not match target." }
                    break
                } else {
                    log?.verbose { "Segment condition $condition matched target." }
                }
            }
            // On match bucket the user.
            if (match) {
                log?.verbose { "Segment conditions matched, bucketing target." }
                val variantKey = bucket(target, segment)
                return flag.variants[variantKey]
            }
        }
        return null
    }

    private fun matchCondition(target: EvaluationTarget, condition: EvaluationCondition): Boolean {
        val propValue = target.select(condition.selector)
        // We need special matching for null properties and set type prop values
        // and operators. All other values are matched as strings, since the
        // filter values are always strings.
        if (propValue == null) {
            return matchNull(condition.op, condition.values)
        } else if (isSetOperator(condition.op)) {
            val propValueStringList = coerceStringList(propValue) ?: return false
            return matchSet(propValueStringList, condition.op, condition.values)
        } else {
            val propValueString = coerceString(propValue) ?: return false
            return matchString(propValueString, condition.op, condition.values)
        }
    }

    private fun getHash(key: String): Long {
        // hash32x86 returns a number that can't fit in a signed 32-bit java integer.
        // Source: https://stackoverflow.com/a/24090718/2322146
        val data = key.encodeToByteArray()
        val value = Murmur3.hash32x86(data, data.size, 0)
        return value.toLong() and 0xffffffffL
    }

    private fun bucket(target: EvaluationTarget, segment: EvaluationSegment): String? {
        log?.verbose { "Bucketing segment $segment with target $target" }
        if (segment.bucket == null) {
            // A null bucket means the segment is fully rolled out. Select the default variant.
            log?.verbose { "Segment bucket is null, returning default variant ${segment.variant}." }
            return segment.variant
        }
        // Select the bucketing value.
        val bucketingValue = coerceString(target.select(segment.bucket.selector))
        log?.verbose { "Selected bucketing value $bucketingValue from target." }
        if (bucketingValue == null || bucketingValue.isEmpty()) {
            // A null or empty bucketing value cannot be bucketed. Select the default variant.
            log?.verbose { "Selected bucketing value is null or empty." }
            return segment.variant
        }
        // Salt and hash the value, and compute the allocation and distribution values.
        val keyToHash = "${segment.bucket.salt}/$bucketingValue"
        val hash = getHash(keyToHash)
        val allocationValue = hash % 100
        val distributionValue = hash.floorDiv(100)
        // Iterate over allocations. If the value falls within the range, check the distribution.
        for (allocation in segment.bucket.allocations) {
            val allocationStart = allocation.range[0]
            val allocationEnd = allocation.range[1]
            if (allocationValue in allocationStart until allocationEnd) {
                for (distribution in allocation.distributions) {
                    val distributionStart = distribution.range[0]
                    val distributionEnd = distribution.range[1]
                    if (distributionValue in distributionStart until distributionEnd) {
                        log?.verbose { "Bucketing hit allocation and distribution, returning variant ${distribution.variant}." }
                        return distribution.variant
                    }
                }
            }
        }
        // No allocation and distribution match. Select the default variant.
        return segment.variant
    }

    private fun mergeMetadata(vararg metadata: Map<String, Any?>?): Map<String, Any?>? {
        val mergedMetadata = mutableMapOf<String, Any?>()
        for (metadataElement in metadata) {
            if (metadataElement != null) {
                mergedMetadata.putAll(metadataElement)
            }
        }
        return if (mergedMetadata.isEmpty()) {
            null
        } else {
            mergedMetadata
        }
    }

    private fun matchNull(op: String, filterValues: Set<String>): Boolean {
        val containsNone = containsNone(filterValues)
        return when (op) {
            EvaluationOperator.IS, EvaluationOperator.CONTAINS, EvaluationOperator.LESS_THAN,
            EvaluationOperator.LESS_THAN_EQUALS, EvaluationOperator.GREATER_THAN,
            EvaluationOperator.GREATER_THAN_EQUALS, EvaluationOperator.VERSION_LESS_THAN,
            EvaluationOperator.VERSION_LESS_THAN_EQUALS, EvaluationOperator.VERSION_GREATER_THAN,
            EvaluationOperator.VERSION_GREATER_THAN_EQUALS, EvaluationOperator.SET_IS,
            EvaluationOperator.SET_CONTAINS, EvaluationOperator.SET_CONTAINS_ANY -> containsNone

            EvaluationOperator.IS_NOT, EvaluationOperator.DOES_NOT_CONTAIN,
            EvaluationOperator.SET_DOES_NOT_CONTAIN, EvaluationOperator.SET_DOES_NOT_CONTAIN_ANY -> !containsNone

            EvaluationOperator.REGEX_MATCH -> false
            EvaluationOperator.REGEX_DOES_NOT_MATCH, EvaluationOperator.SET_IS_NOT -> true
            else -> false
        }
    }

    private fun matchSet(propValues: Set<String>, op: String, filterValues: Set<String>): Boolean {
        return when (op) {
            EvaluationOperator.SET_IS -> propValues == filterValues
            EvaluationOperator.SET_IS_NOT -> propValues != filterValues
            EvaluationOperator.SET_CONTAINS -> matchesSetContainsAll(propValues, filterValues)
            EvaluationOperator.SET_DOES_NOT_CONTAIN -> !matchesSetContainsAll(propValues, filterValues)
            EvaluationOperator.SET_CONTAINS_ANY -> matchesSetContainsAny(propValues, filterValues)
            EvaluationOperator.SET_DOES_NOT_CONTAIN_ANY -> !matchesSetContainsAny(propValues, filterValues)
            else -> false
        }
    }

    private fun matchString(propValue: String, op: String, filterValues: Set<String>): Boolean {
        return when (op) {
            EvaluationOperator.IS -> matchesIs(propValue, filterValues)
            EvaluationOperator.IS_NOT -> !matchesIs(propValue, filterValues)
            EvaluationOperator.CONTAINS -> matchesContains(propValue, filterValues)
            EvaluationOperator.DOES_NOT_CONTAIN -> !matchesContains(propValue, filterValues)
            EvaluationOperator.LESS_THAN, EvaluationOperator.LESS_THAN_EQUALS,
            EvaluationOperator.GREATER_THAN, EvaluationOperator.GREATER_THAN_EQUALS ->
                matchesComparable(propValue, op, filterValues) { value -> parseDouble(value) }

            EvaluationOperator.VERSION_LESS_THAN, EvaluationOperator.VERSION_LESS_THAN_EQUALS,
            EvaluationOperator.VERSION_GREATER_THAN, EvaluationOperator.VERSION_GREATER_THAN_EQUALS ->
                matchesComparable(propValue, op, filterValues) { value -> SemanticVersion.parse(value) }

            EvaluationOperator.REGEX_MATCH -> matchesRegex(propValue, filterValues)
            EvaluationOperator.REGEX_DOES_NOT_MATCH -> !matchesRegex(propValue, filterValues)
            else -> false
        }
    }

    private fun matchesIs(propValue: String, filterValues: Set<String>): Boolean {
        if (containsBooleans(filterValues)) {
            val lower: String = propValue.lowercase()
            if (lower == "true" || lower == "false") {
                return filterValues.any { it.lowercase() == lower }
            }
        }
        return filterValues.contains(propValue)
    }

    private fun matchesContains(propValue: String, filterValues: Set<String>): Boolean {
        for (filterValue in filterValues) {
            if (propValue.lowercase().contains(filterValue.lowercase())) {
                return true
            }
        }
        return false
    }

    private fun matchesSetContainsAll(propValues: Set<String>, filterValues: Set<String>): Boolean {
        if (propValues.size < filterValues.size) {
            return false
        }
        for (filterValue in filterValues) {
            if (!matchesIs(filterValue, propValues)) {
                return false
            }
        }
        return true
    }

    private fun matchesSetContainsAny(propValues: Set<String>, filterValues: Set<String>): Boolean {
        for (filterValue in filterValues) {
            if (matchesIs(filterValue, propValues)) {
                return true
            }
        }
        return false
    }

    private fun <T : Comparable<T>> matchesComparable(
        propValue: String,
        op: String,
        filterValues: Set<String>,
        transformer: (String) -> T?,
    ): Boolean {
        val propValueTransformed: T? = transformer.invoke(propValue)
        val filterValuesTransformed: Set<T> = filterValues.mapNotNull(transformer).toSet()
        return if (propValueTransformed == null || filterValuesTransformed.isEmpty()) {
            // If the prop value or none of the filter values transform, fall
            // back on string comparison.
            filterValues.any { filterValue ->
                matchesComparable(propValue, op, filterValue)
            }
        } else {
            // Match only transformed filter values.
            filterValuesTransformed.any { filterValueTransformed ->
                matchesComparable(propValueTransformed, op, filterValueTransformed)
            }
        }
    }

    private fun <T> matchesComparable(propValue: Comparable<T>, op: String, filterValue: T): Boolean {
        val compareTo = propValue.compareTo(filterValue)
        return when (op) {
            EvaluationOperator.LESS_THAN, EvaluationOperator.VERSION_LESS_THAN -> compareTo < 0
            EvaluationOperator.LESS_THAN_EQUALS, EvaluationOperator.VERSION_LESS_THAN_EQUALS -> compareTo <= 0
            EvaluationOperator.GREATER_THAN, EvaluationOperator.VERSION_GREATER_THAN -> compareTo > 0
            EvaluationOperator.GREATER_THAN_EQUALS, EvaluationOperator.VERSION_GREATER_THAN_EQUALS -> compareTo >= 0
            else -> throw IllegalArgumentException("Unexpected comparison operator $op")
        }
    }

    private fun matchesRegex(propValue: String, filterValues: Set<String>): Boolean {
        return filterValues.any { filterValue -> Regex(filterValue).matches(propValue) }
    }

    private fun containsNone(filterValues: Set<String>): Boolean {
        return filterValues.contains("(none)")
    }

    private fun containsBooleans(filterValues: Set<String>): Boolean {
        return filterValues.any { filterValue ->
            when (filterValue.lowercase()) {
                "true", "false" -> true
                else -> false
            }
        }
    }

    private fun parseDouble(value: String): Double? {
        return try {
            value.toDouble()
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun coerceString(value: Any?): String? {
        return when (value) {
            null -> null
            is Map<*, *> -> json.encodeToString(value.toJsonObject())
            is Collection<*> -> json.encodeToString(value.toJsonArray())
            else -> value.toString()
        }
    }

    private fun coerceStringList(value: Any): Set<String>? {
        // Convert collections to a list of strings
        if (value is Collection<*>) {
            return value.mapNotNull { coerceString(it) }.toSet()
        }
        // Parse a string as json array and convert to list of strings, or
        // return null if the string could not be parsed as a json array.
        val stringValue = value.toString()
        val jsonArray = try {
            json.decodeFromString<JsonArray>(stringValue)
        } catch (e: SerializationException) {
            return null
        }
        return jsonArray.toList().mapNotNull { coerceString(it) }.toSet()
    }

    private fun isSetOperator(op: String): Boolean {
        return when (op) {
            EvaluationOperator.SET_IS,
            EvaluationOperator.SET_IS_NOT,
            EvaluationOperator.SET_CONTAINS,
            EvaluationOperator.SET_DOES_NOT_CONTAIN,
            EvaluationOperator.SET_CONTAINS_ANY,
            EvaluationOperator.SET_DOES_NOT_CONTAIN_ANY -> true

            else -> false
        }
    }
}
