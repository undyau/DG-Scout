package com.undy.dgscout.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.undy.dgscout.model.DistanceUnit
import com.undy.dgscout.model.isUnitedStates
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.math.roundToInt

const val METERS_PER_FOOT = 0.3048
private const val FEET_PER_METER = 1.0 / METERS_PER_FOOT

fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

/** Fetches one fresh location fix, or null if permission is missing or no fix could be obtained. */
suspend fun getCurrentLocation(context: Context): Location? {
    if (!hasLocationPermission(context)) return null
    val client = LocationServices.getFusedLocationProviderClient(context)
    val cancellationSource = CancellationTokenSource()
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancellationSource.cancel() }
        try {
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()
            client.getCurrentLocation(request, cancellationSource.token)
                .addOnSuccessListener { location -> continuation.resumeWith(Result.success(location)) }
                .addOnFailureListener { continuation.resumeWith(Result.success(null)) }
        } catch (e: SecurityException) {
            continuation.resumeWith(Result.success(null))
        }
    }
}

/**
 * The device's region *format* setting (Locale.getDefault().country) often doesn't match where
 * the phone actually is - many phones ship set to US regardless of the owner's country. The
 * cellular network's country is a much better signal of physical location, so prefer that and
 * only fall back to the locale when there's no telephony radio to ask (e.g. a WiFi-only tablet).
 */
fun deviceDefaultDistanceUnit(context: Context): DistanceUnit {
    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    val networkCountry = telephonyManager?.networkCountryIso?.takeIf { it.isNotBlank() }
    val simCountry = telephonyManager?.simCountryIso?.takeIf { it.isNotBlank() }
    val countryCode = networkCountry ?: simCountry ?: Locale.getDefault().country
    return if (isUnitedStates(countryCode)) DistanceUnit.FEET else DistanceUnit.METERS
}

fun distanceInMeters(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Double {
    val results = FloatArray(1)
    Location.distanceBetween(fromLat, fromLon, toLat, toLon, results)
    return results[0].toDouble()
}

fun formatDistance(meters: Double, unit: DistanceUnit): String = when (unit) {
    DistanceUnit.FEET -> "${(meters * FEET_PER_METER).roundToInt()} ft"
    DistanceUnit.METERS -> "${meters.roundToInt()} m"
}

fun formatAccuracy(accuracyMeters: Float, unit: DistanceUnit): String = when (unit) {
    DistanceUnit.FEET -> "±${(accuracyMeters * FEET_PER_METER).roundToInt()} ft"
    DistanceUnit.METERS -> "±${accuracyMeters.roundToInt()} m"
}
