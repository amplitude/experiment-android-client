package com.amplitude.experiment.util

import android.util.Log
import com.amplitude.experiment.Experiment
import com.amplitude.experiment.ExperimentUser
import org.jetbrains.annotations.TestOnly
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
        json.put("dma", city)
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

internal fun ExperimentUser.merge(other: ExperimentUser?, overwrite: Boolean = false): ExperimentUser {
    return copyToBuilder()
        .userId(userId.takeOrOverwrite(other?.userId, overwrite))
        .deviceId(deviceId.takeOrOverwrite(other?.deviceId, overwrite))
        .country(country.takeOrOverwrite(other?.country, overwrite))
        .region(region.takeOrOverwrite(other?.region, overwrite))
        .dma(dma.takeOrOverwrite(other?.dma, overwrite))
        .city(city.takeOrOverwrite(other?.city, overwrite))
        .language(language.takeOrOverwrite(other?.language, overwrite))
        .platform(platform.takeOrOverwrite(other?.platform, overwrite))
        .version(version.takeOrOverwrite(other?.version, overwrite))
        .os(os.takeOrOverwrite(other?.os, overwrite))
        .deviceManufacturer(
            deviceManufacturer.takeOrOverwrite(other?.deviceManufacturer, overwrite)
        )
        .deviceBrand(deviceBrand.takeOrOverwrite(other?.deviceBrand, overwrite))
        .deviceModel(deviceModel.takeOrOverwrite(other?.deviceModel, overwrite))
        .carrier(carrier.takeOrOverwrite(other?.carrier, overwrite))
        .library(library.takeOrOverwrite(other?.library, overwrite))
        .userProperties(userProperties.takeOrOverwrite(other?.userProperties, overwrite))
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