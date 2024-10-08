package com.amplitude.experiment.util

import com.amplitude.experiment.ExperimentUser
import com.amplitude.experiment.evaluation.EvaluationContext
import org.json.JSONException
import org.json.JSONObject

internal fun ExperimentUser.toJson(): String {
    val json = JSONObject()
    try {
        json.put("user_id", userId)
        json.put("device_id", deviceId)
        json.put("country", country)
        json.put("city", city)
        json.put("region", region)
        json.put("dma", dma)
        json.put("language", language)
        json.put("platform", platform)
        json.put("version", version)
        json.put("os", os)
        json.put("device_brand", deviceBrand)
        json.put("device_manufacturer", deviceManufacturer)
        json.put("device_model", deviceModel)
        json.put("carrier", carrier)
        json.put("library", library)
        json.put(
            "user_properties",
            JSONObject(userProperties?.toMutableMap() ?: mutableMapOf<String, Any?>()),
        )
        json.put(
            "groups",
            groups?.toJSONObject(),
        )
        json.put(
            "group_properties",
            groupProperties?.toJSONObject(),
        )
    } catch (e: JSONException) {
        Logger.w("Error converting SkylabUser to JSONObject", e)
    }
    return json.toString()
}

internal fun ExperimentUser.toEvaluationContext(): EvaluationContext {
    val context = EvaluationContext()
    val groups = mutableMapOf<String, Map<String, Any>>()
    if (!this.groups.isNullOrEmpty()) {
        for (entry in this.groups) {
            val groupType = entry.key
            val groupNames = entry.value
            if (groupNames.isNotEmpty()) {
                val groupName = groupNames.first()
                val groupNameMap = mutableMapOf<String, Any>().apply { put("group_name", groupName) }
                val groupProperties = this.groupProperties?.get(groupType)?.get(groupName)
                if (!groupProperties.isNullOrEmpty()) {
                    groupNameMap["group_properties"] = groupProperties
                }
                groups[groupType] = groupNameMap
            }
        }
        context["groups"] = groups
    }
    val userMap = this.toMap().toMutableMap()
    userMap.remove("groups")
    userMap.remove("group_properties")
    context["user"] = userMap
    return context
}

internal fun ExperimentUser.toMap(): Map<String, Any?> {
    return mapOf(
        "user_id" to userId,
        "device_id" to deviceId,
        "country" to country,
        "region" to region,
        "dma" to dma,
        "city" to city,
        "language" to language,
        "platform" to platform,
        "version" to version,
        "os" to os,
        "device_manufacturer" to deviceManufacturer,
        "device_brand" to deviceBrand,
        "device_model" to deviceModel,
        "carrier" to carrier,
        "library" to library,
        "user_properties" to userProperties,
        "groups" to groups,
        "group_properties" to groupProperties,
    ).filterValues { it != null }
}

internal fun ExperimentUser?.merge(other: ExperimentUser?): ExperimentUser {
    val user = this ?: ExperimentUser()
    val mergedUserProperties = this?.userProperties.merge(other?.userProperties) { t, o -> o + t }
    val mergedGroups = this?.groups.merge(other?.groups) { t, o -> o + t }
    val mergedGroupProperties: Map<String, Map<String, Map<String, Any?>>>? =
        this?.groupProperties.mergeMapValues(other?.groupProperties) { thisGroupName, otherGroupName ->
            thisGroupName.mergeMapValues(otherGroupName) { thisGroupProperties, otherGroupProperties ->
                otherGroupProperties + thisGroupProperties
            }
        }

    return user.copyToBuilder()
        .userId(user.userId.merge(other?.userId))
        .deviceId(user.deviceId.merge(other?.deviceId))
        .country(user.country.merge(other?.country))
        .region(user.region.merge(other?.region))
        .dma(user.dma.merge(other?.dma))
        .city(user.city.merge(other?.city))
        .language(user.language.merge(other?.language))
        .platform(user.platform.merge(other?.platform))
        .version(user.version.merge(other?.version))
        .os(user.os.merge(other?.os))
        .deviceManufacturer(
            user.deviceManufacturer.merge(other?.deviceManufacturer),
        )
        .deviceBrand(user.deviceBrand.merge(other?.deviceBrand))
        .deviceModel(user.deviceModel.merge(other?.deviceModel))
        .carrier(user.carrier.merge(other?.carrier))
        .library(user.library.merge(other?.library))
        .userProperties(mergedUserProperties)
        .groups(mergedGroups)
        .groupProperties(mergedGroupProperties)
        .build()
}

// Private Helpers

private fun <T> Map<String, T>?.mergeMapValues(
    other: Map<String, T>?,
    merger: (T, T) -> T?,
): Map<String, T>? {
    return when {
        this == null -> other
        other == null -> this
        else -> {
            val result = mutableMapOf<String, T>()
            for ((thisKey, thisValue) in this.entries) {
                val otherValue = other[thisKey]
                val value =
                    if (otherValue != null) {
                        merger(thisValue, otherValue)
                    } else {
                        thisValue
                    }
                if (value != null) {
                    result[thisKey] = value
                }
            }
            for ((otherKey, otherValue) in other.entries) {
                if (!result.contains(otherKey)) {
                    result[otherKey] = otherValue
                }
            }
            result
        }
    }
}

private fun <T> T?.merge(
    other: T?,
    merger: (T, T) -> T = { t, o -> t },
): T? {
    return when {
        this == null -> other
        other == null -> this
        else -> merger(this, other)
    }
}
