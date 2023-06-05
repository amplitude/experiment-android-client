package com.amplitude.experiment.util

import com.amplitude.experiment.ExperimentConfig
import org.json.JSONObject

fun String?.toExperimentConfig(): ExperimentConfig? {
    return if (this == null) {
        return null
    } else {
        JSONObject(this).toExperimentConfig()
    }
}

fun JSONObject.toExperimentConfig(): ExperimentConfig {
    val configBuilder = ExperimentConfig.builder()
    if (has("debug")) {
        configBuilder.debug(this.getBoolean("debug"))
    }
    if (has("instanceName")) {
        configBuilder.instanceName(this.getString("instanceName"))
    }
    if (has("serverUrl")) {
        configBuilder.serverUrl(this.getString("serverUrl"))
    }
    if (has("fetchTimeoutMillis")) {
        configBuilder.fetchTimeoutMillis(this.getLong("fetchTimeoutMillis"))
    }
    return configBuilder.build()
}
