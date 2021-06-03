package com.amplitude.experiment.util

import com.amplitude.experiment.ExperimentUser
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
            JSONObject(userProperties?.toMutableMap() ?: mutableMapOf<String, Any?>())
        )
    } catch (e: JSONException) {
        Logger.w("Error converting SkylabUser to JSONObject", e)
    }
    return json.toString()
}

internal fun ExperimentUser?.merge(other: ExperimentUser?, overwrite: Boolean = false): ExperimentUser {
    val user = this ?: ExperimentUser()
    return user.copyToBuilder()
        .userId(user.userId.takeOrOverwrite(other?.userId, overwrite))
        .deviceId(user.deviceId.takeOrOverwrite(other?.deviceId, overwrite))
        .country(user.country.takeOrOverwrite(other?.country, overwrite))
        .region(user.region.takeOrOverwrite(other?.region, overwrite))
        .dma(user.dma.takeOrOverwrite(other?.dma, overwrite))
        .city(user.city.takeOrOverwrite(other?.city, overwrite))
        .language(user.language.takeOrOverwrite(other?.language, overwrite))
        .platform(user.platform.takeOrOverwrite(other?.platform, overwrite))
        .version(user.version.takeOrOverwrite(other?.version, overwrite))
        .os(user.os.takeOrOverwrite(other?.os, overwrite))
        .deviceManufacturer(
            user.deviceManufacturer.takeOrOverwrite(other?.deviceManufacturer, overwrite)
        )
        .deviceBrand(user.deviceBrand.takeOrOverwrite(other?.deviceBrand, overwrite))
        .deviceModel(user.deviceModel.takeOrOverwrite(other?.deviceModel, overwrite))
        .carrier(user.carrier.takeOrOverwrite(other?.carrier, overwrite))
        .library(user.library.takeOrOverwrite(other?.library, overwrite))
        .userProperties(user.userProperties.takeOrOverwrite(other?.userProperties, overwrite))
        .build()
}

// Private Helpers

private fun <T> T?.takeOrOverwrite(other: T?, overwrite: Boolean): T? {
    return when {
        this == null -> other
        other == null -> this
        overwrite -> other
        else -> this
    }
}
