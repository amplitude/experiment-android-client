package com.amplitude.experiment

import android.os.Build
import com.amplitude.api.Constants
import com.amplitude.api.DeviceInfo
import java.util.*

class DefaultUserProvider(
    private val userId: String? = null,
    private val deviceId: String? = null,
) : ExperimentUserProvider {

    constructor() : this (null, null)

    private val platform = Constants.PLATFORM
    private val language: String = Locale.getDefault().language
    private val os: String = DeviceInfo.OS_NAME + " " + Build.VERSION.RELEASE
    private val brand: String = Build.BRAND
    private val manufacturer: String = Build.MANUFACTURER
    private val model: String = Build.MODEL

    override fun getUser(): ExperimentUser {
        return ExperimentUser.builder()
            .platform(platform)
            .language(language)
            .os(os)
            .deviceBrand(brand)
            .deviceManufacturer(manufacturer)
            .deviceModel(model)
            .build()
    }
}