package com.example.smartride_dbd

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log

class SpeedTracker(private val context: Context) {

    private val locationManager: LocationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    
    private var currentSpeed = 0.0f // Speed in meters per second
    private var lastLocation: Location? = null
    private var speedListener: SpeedUpdateListener? = null
    private var speedReadings = FloatArray(SPEED_HISTORY_SIZE) { 0f }
    private var readingIndex = 0
    private var isMoving = false
    
    interface SpeedUpdateListener {
        fun onSpeedUpdate(speedKmh: Float)
    }
    
    // Location listener for GPS updates
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Only process locations with good accuracy
            if (location.hasAccuracy() && location.accuracy <= MAX_ACCURACY_THRESHOLD_M) {
                calculateSpeed(location)
            } else {
                Log.d(TAG, "Ignoring location with poor accuracy: ${location.accuracy}m")
            }
        }
        
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            // Not used in newer Android versions, but needed for the interface
        }
        
        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "Location provider enabled: $provider")
        }
        
        override fun onProviderDisabled(provider: String) {
            Log.d(TAG, "Location provider disabled: $provider")
            currentSpeed = 0.0f
            isMoving = false
            resetSpeedHistory()
            notifySpeedUpdate()
        }
    }
    
    private fun resetSpeedHistory() {
        for (i in speedReadings.indices) {
            speedReadings[i] = 0f
        }
        readingIndex = 0
    }
    
    fun setSpeedListener(listener: SpeedUpdateListener) {
        speedListener = listener
    }

    @SuppressLint("MissingPermission") // Permission check is done before calling this
    fun startTracking() {
        try {
            // Request location updates with shorter interval for better accuracy
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                UPDATE_INTERVAL_MS,
                MIN_DISTANCE_M,
                locationListener
            )
            
            // Also use network provider as fallback
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    UPDATE_INTERVAL_MS,
                    MIN_DISTANCE_M,
                    locationListener
                )
            } catch (e: Exception) {
                Log.e(TAG, "Network provider not available: ${e.message}")
            }
            
            Log.d(TAG, "Started speed tracking")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speed tracking: ${e.message}")
        }
    }
    
    fun stopTracking() {
        try {
            locationManager.removeUpdates(locationListener)
            currentSpeed = 0.0f
            lastLocation = null
            resetSpeedHistory()
            Log.d(TAG, "Stopped speed tracking")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speed tracking: ${e.message}")
        }
    }
    
    private fun calculateSpeed(location: Location) {
        val previous = lastLocation
        
        if (previous != null) {
            // Calculate time difference
            val timeElapsedMs = location.time - previous.time
            
            // Only process if sufficient time has passed
            if (timeElapsedMs >= MIN_TIME_BETWEEN_UPDATES_MS) {
                // Calculate distance
                val distanceMeters = location.distanceTo(previous)
                
                // Calculate speed (meters per second)
                if (timeElapsedMs > 0) {
                    // Convert to seconds for m/s calculation
                    val timeElapsedSeconds = timeElapsedMs / 1000.0
                    val calculatedSpeed = (distanceMeters / timeElapsedSeconds).toFloat()
                    
                    // Add to historical readings for averaging
                    speedReadings[readingIndex] = calculatedSpeed
                    readingIndex = (readingIndex + 1) % SPEED_HISTORY_SIZE
                    
                    // Calculate median-filtered speed (better for removing outliers)
                    val filteredSpeed = calculateMedianFilteredSpeed()
                    
                    // Apply movement threshold to prevent jitter
                    val kmhSpeed = filteredSpeed * 3.6f
                    
                    if (kmhSpeed < STATIONARY_THRESHOLD_KMH) {
                        // If below threshold, consider stationary
                        if (isMoving) {
                            // Only log when transitioning from moving to stationary
                            Log.d(TAG, "Speed below threshold (${kmhSpeed.toInt()} km/h), considering stationary")
                            isMoving = false
                        }
                        currentSpeed = 0f
                    } else {
                        // Above threshold, consider moving
                        if (!isMoving) {
                            // Only log when transitioning from stationary to moving
                            Log.d(TAG, "Speed above threshold (${kmhSpeed.toInt()} km/h), considering moving")
                            isMoving = true
                        }
                        currentSpeed = filteredSpeed
                    }
                    
                    // Log and notify
                    Log.d(TAG, "Raw speed: ${calculatedSpeed * 3.6f} km/h, Filtered: ${getSpeedKmh()} km/h")
                    notifySpeedUpdate()
                }
            }
        }
        
        // Update the last location
        lastLocation = location
    }
    
    private fun calculateMedianFilteredSpeed(): Float {
        // Copy current readings to temporary array
        val sortedSpeeds = speedReadings.copyOf()
        sortedSpeeds.sort()
        
        // Use median value (middle of sorted array) to eliminate outliers
        return if (sortedSpeeds.size % 2 == 0) {
            // Even number of elements - average the two middle values
            val middle1 = sortedSpeeds[sortedSpeeds.size / 2 - 1]
            val middle2 = sortedSpeeds[sortedSpeeds.size / 2]
            (middle1 + middle2) / 2f
        } else {
            // Odd number of elements - use the middle value
            sortedSpeeds[sortedSpeeds.size / 2]
        }
    }
    
    fun getCurrentSpeed(): Float {
        return currentSpeed
    }
    
    fun getSpeedKmh(): Float {
        // Convert m/s to km/h
        return currentSpeed * 3.6f
    }
    
    private fun notifySpeedUpdate() {
        speedListener?.onSpeedUpdate(getSpeedKmh())
    }
    
    companion object {
        private const val TAG = "SpeedTracker"
        private const val UPDATE_INTERVAL_MS = 500L // Half second update for better response
        private const val MIN_DISTANCE_M = 0f // Minimum distance in meters
        private const val MIN_TIME_BETWEEN_UPDATES_MS = 200L // Minimum time between calculations
        private const val MAX_ACCURACY_THRESHOLD_M = 10f // Better accuracy threshold (was 20)
        private const val STATIONARY_THRESHOLD_KMH = 3.0f // Below this speed (km/h), consider stationary
        private const val SPEED_HISTORY_SIZE = 5 // Number of speed readings to keep for filtering
    }
} 