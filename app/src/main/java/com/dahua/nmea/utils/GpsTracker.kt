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

class GpsTracker(
    private val context: Context,
    private val onLocationUpdate: (Location) -> Unit
) {
    
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var isTracking = false
    
    init {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    
    fun startTracking() {
        if (isTracking) return
        
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "GPS permission not granted")
            return
        }
        
        Log.d(TAG, "Starting GPS tracking with native LocationManager...")
        
        // Try to get last known location first
        try {
            val lastKnownLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            lastKnownLocation?.let {
                Log.d(TAG, "Got last known location: ${it.latitude}, ${it.longitude}")
                onLocationUpdate(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last known location: ${e.message}")
        }
        
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val currentTime = System.currentTimeMillis()
                val locationTime = location.time
                val timeDiff = currentTime - locationTime
                Log.d(TAG, "GPS update: lat=${location.latitude}, lon=${location.longitude}, accuracy=${location.accuracy}m")
                Log.d(TAG, "GPS time: locationTime=${java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date(locationTime))}, currentTime=${java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date(currentTime))}, diff=${timeDiff}ms")
                onLocationUpdate(location)
            }
            
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Log.d(TAG, "GPS status changed: $provider, status: $status")
            }
            
            override fun onProviderEnabled(provider: String) {
                Log.d(TAG, "GPS provider enabled: $provider")
            }
            
            override fun onProviderDisabled(provider: String) {
                Log.w(TAG, "GPS provider disabled: $provider")
            }
        }
        
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500, // Update every 0.5 seconds
                0f,   // No minimum distance
                locationListener!!
            )
            Log.d(TAG, "GPS tracking started successfully with 500ms interval")
            isTracking = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start GPS tracking: ${e.message}")
        }
    }
    
    companion object {
        private const val TAG = "GpsTracker"
    }
    
    fun stopTracking() {
        if (!isTracking) return
        
        locationListener?.let { listener ->
            locationManager?.removeUpdates(listener)
        }
        
        isTracking = false
    }
    
    fun isTracking(): Boolean = isTracking
}
