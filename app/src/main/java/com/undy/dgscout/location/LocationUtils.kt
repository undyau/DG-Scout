package com.undy.dgscout.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine

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

fun distanceInFeet(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Double {
    val results = FloatArray(1)
    Location.distanceBetween(fromLat, fromLon, toLat, toLon, results)
    return results[0] * FEET_PER_METER
}
