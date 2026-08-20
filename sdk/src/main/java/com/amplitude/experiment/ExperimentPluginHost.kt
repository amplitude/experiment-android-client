@file:JvmName("ExperimentPluginHost")

package com.amplitude.experiment

import com.amplitude.core.platform.PluginHost
import com.amplitude.core.platform.plugins

/**
 * Returns the Experiment client registered for [deploymentKey], or `null` when none is attached.
 */
fun PluginHost.experiment(deploymentKey: String?): ExperimentClient? =
    (plugin(AmplitudeExperimentPlugin.pluginName(deploymentKey)) as? AmplitudeExperimentPlugin)
        ?.experimentClient

/**
 * Returns the Experiment client when exactly one Experiment plugin is attached.
 *
 * Use [experiment(deploymentKey)][experiment] when multiple Experiment plugins are registered.
 */
val PluginHost.experiment: ExperimentClient?
    get() = plugins<AmplitudeExperimentPlugin>().singleOrNull()?.experimentClient
