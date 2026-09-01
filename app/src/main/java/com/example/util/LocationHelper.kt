package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class UserGpsLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 0f,
    val locationName: String = "Position GPS"
)

object LocationHelper {
    private const val TAG = "LocationHelper"

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context, timeoutMs: Long = 5000L): UserGpsLocation? {
        if (!hasLocationPermission(context)) {
            Log.d(TAG, "Location permission not granted")
            return null
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        // 1. Try to get best last known location immediately
        val lastKnownGps = try { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (e: Exception) { null }
        val lastKnownNet = try { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (e: Exception) { null }
        val lastKnownPassive = try { locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } catch (e: Exception) { null }

        val bestLastKnown = listOfNotNull(lastKnownGps, lastKnownNet, lastKnownPassive)
            .maxByOrNull { it.time }

        if (bestLastKnown != null && System.currentTimeMillis() - bestLastKnown.time < 15 * 60 * 1000) {
            val label = determineLocationLabel(bestLastKnown.latitude, bestLastKnown.longitude)
            return UserGpsLocation(
                latitude = bestLastKnown.latitude,
                longitude = bestLastKnown.longitude,
                accuracyMeters = bestLastKnown.accuracy,
                locationName = label
            )
        }

        // 2. Request single fresh update with timeout
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        try {
                            locationManager.removeUpdates(this)
                        } catch (e: Exception) {
                            Log.w(TAG, "Error removing location updates: ${e.message}")
                        }
                        if (continuation.isActive) {
                            val label = determineLocationLabel(location.latitude, location.longitude)
                            continuation.resume(
                                UserGpsLocation(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    accuracyMeters = location.accuracy,
                                    locationName = label
                                )
                            )
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                try {
                    val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    val isNetEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                    if (isGpsEnabled) {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            0L,
                            0f,
                            listener
                        )
                    } else if (isNetEnabled) {
                        locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            0L,
                            0f,
                            listener
                        )
                    } else {
                        if (continuation.isActive) {
                            if (bestLastKnown != null) {
                                val label = determineLocationLabel(bestLastKnown.latitude, bestLastKnown.longitude)
                                continuation.resume(
                                    UserGpsLocation(
                                        latitude = bestLastKnown.latitude,
                                        longitude = bestLastKnown.longitude,
                                        accuracyMeters = bestLastKnown.accuracy,
                                        locationName = label
                                    )
                                )
                            } else {
                                continuation.resume(null)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to request location updates: ${e.message}")
                    if (continuation.isActive) continuation.resume(null)
                }

                continuation.invokeOnCancellation {
                    try {
                        locationManager.removeUpdates(listener)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        } ?: bestLastKnown?.let {
            val label = determineLocationLabel(it.latitude, it.longitude)
            UserGpsLocation(it.latitude, it.longitude, it.accuracy, label)
        }
    }

    fun determineLocationLabel(lat: Double, lon: Double): String {
        // Approximate distance to Cameroonian sites
        val distFiguil = distanceKm(lat, lon, 9.7569, 13.9634)
        val distBonaberi = distanceKm(lat, lon, 4.0722, 9.6885)
        val distNomayos = distanceKm(lat, lon, 3.7744, 11.4506)
        val distYaounde = distanceKm(lat, lon, 3.8480, 11.5021)
        val distDouala = distanceKm(lat, lon, 4.0511, 9.7679)
        val distGaroua = distanceKm(lat, lon, 9.3000, 13.4000)

        return when {
            distFiguil < 25.0 -> "Figuil (Proximité Usine)"
            distBonaberi < 20.0 -> "Bonabéri / Douala"
            distNomayos < 20.0 -> "Nomayos (Usine)"
            distDouala < 35.0 -> "Douala (Littoral)"
            distYaounde < 35.0 -> "Yaoundé (Centre)"
            distGaroua < 50.0 -> "Région Nord (Garoua)"
            else -> String.format("Position GPS (%.2f°N, %.2f°E)", lat, lon)
        }
    }

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
