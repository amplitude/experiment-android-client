package com.amplitude.experiment

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import com.amplitude.api.Constants
import com.amplitude.api.DeviceInfo
import java.util.Locale

class DefaultUserProvider(
    context: Context,
    private val userId: String? = null,
    private val deviceId: String? = null,

) : ExperimentUserProvider {

    constructor(context: Context) : this (context, null, null)

    private val version = context.getAppVersion()
    private val carrier = context.getCarrier()

    private val platform = Constants.PLATFORM
    private val language: String = Locale.getDefault().language
    private val os: String = DeviceInfo.OS_NAME + " " + Build.VERSION.RELEASE
    private val brand: String = Build.BRAND
    private val manufacturer: String = Build.MANUFACTURER
    private val model: String = Build.MODEL

    override fun getUser(): ExperimentUser {
        return ExperimentUser.builder()
            .deviceId(deviceId)
            .userId(userId)
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

private fun Context.getAppVersion(): String? {
    return try {
        packageManager?.getPackageInfo(packageName, 0)?.versionName
    } catch (ignored: Exception) {
        null
    }
}

private fun Context.getCarrier(): String? {
    return try {
        val manager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        manager?.networkOperatorName
    } catch (e: Exception) {
        null
    }
}