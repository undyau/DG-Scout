package com.undy.dgscout.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.os.SystemClock
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.undy.dgscout.model.DistanceUnit
import com.undy.dgscout.model.isUnitedStates
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

const val METERS_PER_FOOT = 0.3048
private const val FEET_PER_METER = 1.0 / METERS_PER_FOOT

fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

/** Fixes older than this are treated as stale and ignored. */
private const val MAX_FIX_AGE_MS = 5_000L

/**
 * A continuous stream of high-accuracy fixes, one roughly every second, for as long as it is
 * collected. Keeping the GPS running (rather than asking for one-off fixes) is what lets it lock
 * onto satellites and converge to a tight accuracy. One-shot requests return the first fix
 * available, which is often a coarse Wi-Fi/cell or cached position. Emits nothing if permission
 * is missing.
 */
fun locationUpdates(context: Context): Flow<Location> = callbackFlow {
    if (!hasLocationPermission(context)) {
        close()
        return@callbackFlow
    }
    val client = LocationServices.getFusedLocationProviderClient(context)
    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
        .setMinUpdateIntervalMillis(500L)
        .setMaxUpdateAgeMillis(0L)
        .setWaitForAccurateLocation(true)
        .build()
    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations
                .filter(::isFresh)
                .forEach { trySend(it) }
        }
    }
    try {
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    } catch (e: SecurityException) {
        close()
        return@callbackFlow
    }
    awaitClose { client.removeLocationUpdates(callback) }
}

/**
 * Watches the GPS for up to [timeoutMs] and returns the most accurate fix seen, stopping early once
 * a fix reaches [targetAccuracyMeters]. Use this when recording a position, so the stored point is
 * the best the GPS can manage rather than whatever arrived first. Returns null if no fix came in.
 */
suspend fun getAccurateLocation(
    context: Context,
    targetAccuracyMeters: Float = 4f,
    timeoutMs: Long = 10_000L
): Location? {
    var best: Location? = null
    withTimeoutOrNull(timeoutMs) {
        locationUpdates(context)
            .onEach { fix -> if (best.let { it == null || fix.accuracy < it.accuracy }) best = fix }
            .first { it.accuracy <= targetAccuracyMeters }
    }
    return best
}

fun isFresh(location: Location): Boolean =
    (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L <= MAX_FIX_AGE_MS

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

/**
 * The error in a distance between two GPS fixes is driven by both endpoints' own uncertainty.
 * Combine them in quadrature (assuming independent errors) rather than just picking one.
 */
fun combinedAccuracy(a: Float?, b: Float?): Float? = when {
    a != null && b != null -> sqrt(a * a + b * b)
    a != null -> a
    b != null -> b
    else -> null
}
