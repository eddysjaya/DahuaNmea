package com.dahua.nmea.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat

/**
 * GPS Listener - Uses direct GPS access for immediate location updates
 * Note: This uses GPS_PROVIDER instead of PASSIVE to ensure GPS data is available immediately
 * Battery impact: Minimal (GPS already active from DSJ Camera)
 */
class PassiveGpsListener(
    private val context: Context,
    private val onLocationUpdate: (Location) -> Unit
) {

    private var locationManager: LocationManager? = null
    private var passiveListener: LocationListener? = null
    private var isListening = false

    init {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    fun startListening() {
        if (isListening) {
            Log.d(TAG, "Already listening to passive GPS")
            return
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "GPS permission not granted")
            return
        }

        Log.d(TAG, "Starting PASSIVE GPS listening (direct GPS access GPS)...")

        // Try to get last known location first
        try {
            // Try GPS provider first
            val gpsLastKnown = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (gpsLastKnown != null) {
                Log.d(TAG, "Got last GPS location: ${gpsLastKnown.latitude}, ${gpsLastKnown.longitude}, age=${System.currentTimeMillis() - gpsLastKnown.time}ms")
                onLocationUpdate(gpsLastKnown)
            }
            
            // Also try passive provider
            val passiveLastKnown = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (passiveLastKnown != null && (gpsLastKnown == null || passiveLastKnown.time > gpsLastKnown.time)) {
                Log.d(TAG, "Got last passive location: ${passiveLastKnown.latitude}, ${passiveLastKnown.longitude}, age=${System.currentTimeMillis() - passiveLastKnown.time}ms")
                onLocationUpdate(passiveLastKnown)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last known location: ${e.message}")
        }

        passiveListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val currentTime = System.currentTimeMillis()
                val locationTime = location.time
                val timeDiff = currentTime - locationTime
                
                Log.d(TAG, "=== PASSIVE GPS UPDATE (from DSJ Camera) ===")
                Log.d(TAG, "Provider: ${location.provider}")
                Log.d(TAG, "Lat: ${location.latitude}, Lon: ${location.longitude}")
                Log.d(TAG, "Accuracy: ${location.accuracy}m")
                Log.d(TAG, "Time: ${java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date(locationTime))}")
                Log.d(TAG, "Age: ${timeDiff}ms")
                
                // Forward to NMEA generator
                onLocationUpdate(location)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Log.d(TAG, "Passive GPS status changed: $provider, status: $status")
            }

            override fun onProviderEnabled(provider: String) {
                Log.d(TAG, "Passive GPS provider enabled: $provider")
            }

            override fun onProviderDisabled(provider: String) {
                Log.w(TAG, "Passive GPS provider disabled: $provider")
            }
        }

        try {
            // Use PASSIVE_PROVIDER to piggyback on DSJ Camera's GPS requests
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0,      // Get updates immediately when available
                0f,     // No minimum distance
                passiveListener!!
            )
            isListening = true
            Log.i(TAG, "✅ Active GPS listener started - direct GPS access GPS (15 sec interval)")
            Log.i(TAG, "⚡ No additional battery drain - using DSJ Camera's GPS requests")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when requesting passive location updates: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Active GPS listener: ${e.message}")
        }
    }

    fun stopListening() {
        if (!isListening) return

        try {
            passiveListener?.let {
                locationManager?.removeUpdates(it)
                Log.d(TAG, "Active GPS listener stopped")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Active GPS listener: ${e.message}")
        }

        passiveListener = null
        isListening = false
    }

    fun isListening(): Boolean = isListening

    companion object {
        private const val TAG = "PassiveGpsListener"
    }
}


