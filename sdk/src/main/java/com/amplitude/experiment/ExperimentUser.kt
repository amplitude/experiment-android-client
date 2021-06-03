package com.amplitude.experiment

/**
 * The user to fetch experiment/flag variants for. This is an immutable object
 * that can be created using an [ExperimentUser.Builder]. Example usage:
 *
 * ```
 * ExperimentUser.builder().userId("user@company.com").build()
 * ```
 *
 * You can copy and modify a user using [copyToBuilder].
 *
 * ```
 * val user = ExperimentUser.builder()
 *     .userId("user@company.com")
 *     .build()
 * val newUser = user.copyToBuilder()
 *     .userProperty("username", "bumblebee")
 *     .build()
 * ```
 */
class ExperimentUser internal constructor(
    @JvmField val userId: String? = null,
    @JvmField val deviceId: String? = null,
    @JvmField val country: String? = null,
    @JvmField val region: String? = null,
    @JvmField val dma: String? = null,
    @JvmField val city: String? = null,
    @JvmField val language: String? = null,
    @JvmField val platform: String? = null,
    @JvmField val version: String? = null,
    @JvmField val os: String? = null,
    @JvmField val deviceManufacturer: String? = null,
    @JvmField val deviceBrand: String? = null,
    @JvmField val deviceModel: String? = null,
    @JvmField val carrier: String? = null,
    @JvmField val library: String? = null,
    @JvmField val userProperties: Map<String, Any?>? = null,
) {

    /**
     * Construct an empty [ExperimentUser].
     */
    constructor() : this(userId = null)

    fun copyToBuilder(): Builder {
        return builder()
            .userId(this.userId)
            .deviceId(this.deviceId)
            .country(this.country)
            .region(this.region)
            .dma(this.dma)
            .city(this.city)
            .language(this.language)
            .platform(this.platform)
            .version(this.version)
            .os(this.os)
            .deviceManufacturer(this.deviceManufacturer)
            .deviceBrand(this.deviceBrand)
            .deviceModel(this.deviceModel)
            .carrier(this.carrier)
            .library(this.library)
            .userProperties(this.userProperties)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExperimentUser

        if (userId != other.userId) return false
        if (deviceId != other.deviceId) return false
        if (country != other.country) return false
        if (region != other.region) return false
        if (dma != other.dma) return false
        if (city != other.city) return false
        if (language != other.language) return false
        if (platform != other.platform) return false
        if (version != other.version) return false
        if (os != other.os) return false
        if (deviceManufacturer != other.deviceManufacturer) return false
        if (deviceBrand != other.deviceBrand) return false
        if (deviceModel != other.deviceModel) return false
        if (carrier != other.carrier) return false
        if (library != other.library) return false
        if (userProperties != other.userProperties) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId?.hashCode() ?: 0
        result = 31 * result + (deviceId?.hashCode() ?: 0)
        result = 31 * result + (country?.hashCode() ?: 0)
        result = 31 * result + (region?.hashCode() ?: 0)
        result = 31 * result + (dma?.hashCode() ?: 0)
        result = 31 * result + (city?.hashCode() ?: 0)
        result = 31 * result + (language?.hashCode() ?: 0)
        result = 31 * result + (platform?.hashCode() ?: 0)
        result = 31 * result + (version?.hashCode() ?: 0)
        result = 31 * result + (os?.hashCode() ?: 0)
        result = 31 * result + (deviceManufacturer?.hashCode() ?: 0)
        result = 31 * result + (deviceBrand?.hashCode() ?: 0)
        result = 31 * result + (deviceModel?.hashCode() ?: 0)
        result = 31 * result + (carrier?.hashCode() ?: 0)
        result = 31 * result + (library?.hashCode() ?: 0)
        result = 31 * result + (userProperties?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ExperimentUser(userId=$userId, deviceId=$deviceId, country=$country, " +
            "region=$region, dma=$dma, city=$city, language=$language, platform=$platform, " +
            "version=$version, os=$os, deviceManufacturer=$deviceManufacturer, " +
            "deviceBrand=$deviceBrand, deviceModel=$deviceModel, carrier=$carrier, " +
            "library=$library, userProperties=$userProperties)"
    }

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }

    class Builder {
        private var userId: String? = null
        private var deviceId: String? = null
        private var country: String? = null
        private var region: String? = null
        private var dma: String? = null
        private var city: String? = null
        private var language: String? = null
        private var platform: String? = null
        private var version: String? = null
        private var os: String? = null
        private var deviceManufacturer: String? = null
        private var deviceBrand: String? = null
        private var deviceModel: String? = null
        private var carrier: String? = null
        private var library: String? = null
        private var userProperties: MutableMap<String, Any?>? = null

        fun userId(userId: String?) = apply { this.userId = userId }
        fun deviceId(deviceId: String?) = apply { this.deviceId = deviceId }
        fun country(country: String?) = apply { this.country = country }
        fun region(region: String?) = apply { this.region = region }
        fun dma(dma: String?) = apply { this.dma = dma }
        fun city(city: String?) = apply { this.city = city }
        fun language(language: String?) = apply { this.language = language }
        fun platform(platform: String?) = apply { this.platform = platform }
        fun version(version: String?) = apply { this.version = version }
        fun os(os: String?) = apply { this.os = os }
        fun deviceManufacturer(deviceManufacturer: String?) = apply {
            this.deviceManufacturer = deviceManufacturer
        }
        fun deviceBrand(deviceBrand: String?) = apply { this.deviceBrand = deviceBrand }
        fun deviceModel(deviceModel: String?) = apply { this.deviceModel = deviceModel }
        fun carrier(carrier: String?) = apply { this.carrier = carrier }
        fun library(library: String?) = apply { this.library = library }
        fun userProperties(userProperties: Map<String, Any?>?) = apply {
            this.userProperties = userProperties?.toMutableMap()
        }
        fun userProperty(key: String, value: Any?) = apply {
            userProperties = (userProperties ?: mutableMapOf()).apply {
                this[key] = value
            }
        }

        fun build(): ExperimentUser {
            return ExperimentUser(
                userId = userId,
                deviceId = deviceId,
                country = country,
                region = region,
                dma = dma,
                city = city,
                language = language,
                platform = platform,
                version = version,
                os = os,
                deviceManufacturer = deviceManufacturer,
                deviceBrand = deviceBrand,
                deviceModel = deviceModel,
                carrier = carrier,
                library = library,
                userProperties = userProperties,
            )
        }
    }
}
