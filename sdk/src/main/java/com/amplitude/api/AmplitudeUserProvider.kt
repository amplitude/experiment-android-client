package com.amplitude.api

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import com.amplitude.experiment.ExperimentUser
import com.amplitude.experiment.ExperimentUserProvider
import com.amplitude.experiment.util.AmpLogger
import java.util.Locale

@Deprecated(
    "Update your version of the amplitude analytics SDK to 2.36.0+ and for seamless " +
        "integration with the amplitude analytics SDK",
)
class AmplitudeUserProvider(private val amplitudeClient: AmplitudeClient) : ExperimentUserProvider {
    private var initialized = false
    private var version: String? = null
    private var carrier: String? = null

    private fun waitForAmplitudeInitialized() {
        if (initialized) {
            return
        }
        val start = System.nanoTime()
        while (!amplitudeClient.initialized) {
            try {
                Thread.sleep(20)
            } catch (e: InterruptedException) {
                // pass
            }
        }
        cacheVersion()
        cacheCarrier()
        initialized = true
        val end = System.nanoTime()
        AmpLogger.debug(
            String.format(
                "Waited %.3f ms for Amplitude SDK initialization",
                (end - start) / 1000000.0,
            ),
        )
    }

    private fun cacheVersion() {
        if (amplitudeClient.context != null) {
            val packageInfo: PackageInfo
            try {
                packageInfo =
                    amplitudeClient.context.packageManager.getPackageInfo(
                        amplitudeClient.context.packageName, 0,
                    )
                version = packageInfo.versionName
            } catch (ignored: PackageManager.NameNotFoundException) {
            } catch (ignored: Exception) {
            }
        }
    }

    private fun cacheCarrier() {
        if (amplitudeClient.context != null) {
            try {
                val manager =
                    amplitudeClient.context
                        .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                carrier = manager.networkOperatorName
            } catch (e: Exception) {
                // Failed to get network operator name from network
            }
        }
    }

    private fun getUserId(): String? {
        waitForAmplitudeInitialized()
        return amplitudeClient.getUserId()
    }

    private fun getDeviceId(): String? {
        waitForAmplitudeInitialized()
        return amplitudeClient.getDeviceId()
    }

    private val platform = Constants.PLATFORM
    private val language: String = Locale.getDefault().language
    private val os: String = DeviceInfo.OS_NAME + " " + Build.VERSION.RELEASE
    private val brand: String = Build.BRAND
    private val manufacturer: String = Build.MANUFACTURER
    private val model: String = Build.MODEL

    override fun getUser(): ExperimentUser {
        return ExperimentUser.builder()
            .userId(getUserId())
            .deviceId(getDeviceId())
            .version(version)
            .platform(platform)
            .language(language)
            .os(os)
            .deviceBrand(brand)
            .deviceManufacturer(manufacturer)
            .deviceModel(model)
            .carrier(carrier)
            .build()
    }
}
