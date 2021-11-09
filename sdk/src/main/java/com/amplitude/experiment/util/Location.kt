package com.amplitude.experiment.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.telephony.TelephonyManager
import java.io.IOException
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.lang.NullPointerException
import java.util.Locale

// This should not be called on the main thread.
internal fun Context.getCountry(): String? {
    // Prioritize reverse geocode, but until we have a result from that,
    // we try to grab the country from the network, and finally the locale
    var country = getCountryFromLocation()
    if (!country.isNullOrEmpty()) {
        return country
    }
    country = getCountryFromNetwork()
    return if (!country.isNullOrEmpty()) {
        country
    } else getCountryFromLocale()
}

private fun Context.getCountryFromLocation(): String? {
    val recent = getMostRecentLocation() ?: return null
    try {
        if (Geocoder.isPresent()) {
            val geocoder: Geocoder = getGeocoder()
            val addresses = geocoder.getFromLocation(
                recent.latitude,
                recent.longitude, 1
            ) ?: return null
            for (address in addresses) {
                if (address != null) {
                    return address.countryCode
                }
            }
        }
    } catch (e: IOException) {
        // Failed to reverse geocode location
    } catch (e: NullPointerException) {
        // Failed to reverse geocode location
    } catch (e: NoSuchMethodError) {
        // failed to fetch geocoder
    } catch (e: IllegalArgumentException) {
        // Bad lat / lon values can cause Geocoder to throw IllegalArgumentExceptions
    } catch (e: IllegalStateException) {
        // sometimes the location manager is unavailable
    }
    return null
}

@SuppressLint("MissingPermission")
fun Context.getMostRecentLocation(): Location? {
    if (!checkLocationPermissionAllowed()) {
        return null
    }
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = try {
        locationManager.getProviders(true)
    } catch (e: Exception) {
        return null
    }
    val locations = mutableListOf<Location>()
    for (provider in providers) {
        try {
            val location = locationManager.getLastKnownLocation(provider)
            if (location != null) {
                locations.add(location)
            }
        } catch (e: Exception) {
            Logger.w("Failed to get most recent location")
        }
    }
    var maximumTimestamp: Long = -1
    var bestLocation: Location? = null
    for (location in locations) {
        if (location.time > maximumTimestamp) {
            maximumTimestamp = location.time
            bestLocation = location
        }
    }
    return bestLocation
}

private fun Context.getCountryFromNetwork(): String? {
    try {
        val manager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return null
        if (manager.phoneType != TelephonyManager.PHONE_TYPE_CDMA) {
            val country = manager.networkCountryIso
            if (country != null) {
                return country.toUpperCase(Locale.US)
            }
        }
    } catch (e: Exception) {
        // Failed to get country from network
    }
    return null
}

private fun getCountryFromLocale(): String? {
    return Locale.getDefault().country
}

fun Context.getGeocoder(): Geocoder {
    return Geocoder(this, Locale.ENGLISH)
}


// Permissions

private fun Context.checkLocationPermissionAllowed(): Boolean {
    return checkPermissionAllowed(Manifest.permission.ACCESS_COARSE_LOCATION) ||
        checkPermissionAllowed(Manifest.permission.ACCESS_FINE_LOCATION)
}

private fun Context.checkPermissionAllowed(permission: String): Boolean {
    // ANDROID 6.0 AND UP!
    return if (Build.VERSION.SDK_INT >= 23) {
        var hasPermission = false
        try {
            // Invoke checkSelfPermission method from Android 6 (API 23 and UP)
            val methodCheckPermission = Activity::class.java.getMethod("checkSelfPermission", String::class.java)
            val resultObj = methodCheckPermission.invoke(this, permission)
            val result = resultObj.toString().toInt()
            hasPermission = result == PackageManager.PERMISSION_GRANTED
        } catch (ex: Exception) {
        }
        hasPermission
    } else {
        true
    }
}