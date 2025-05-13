package com.example.smartride_dbd

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

/**
 * A class to collect accelerometer and gyroscope data directly from device sensors
 * and process it for driving behavior prediction
 */
class SensorDataCollector(private val context: Context, private val uiViewModel: UiViewModel) {
    
    // Sensor components
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    
    // Sensor data containers
    private var lastAccelerometerData: FloatArray? = null
    private var lastGyroscopeData: FloatArray? = null
    private var previousMagnitude: Float? = null
    
    // Status flag
    private var isCollecting = false
    
    // Sampling rate - SENSOR_DELAY_GAME provides ~50Hz which is good for driving behavior
    private val samplingPeriod = SensorManager.SENSOR_DELAY_GAME
    
    // Sensor listener for both accelerometer and gyroscope
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    // Copy sensor data to avoid mutations
                    val accData = floatArrayOf(event.values[0], event.values[1], event.values[2])
                    lastAccelerometerData = accData
                    Log.d(TAG, "Accelerometer: x=${accData[0]}, y=${accData[1]}, z=${accData[2]}")
                    
                    // Try to process data if we have both accelerometer and gyroscope readings
                    processDataIfComplete()
                }
                Sensor.TYPE_GYROSCOPE -> {
                    // Copy sensor data to avoid mutations
                    val gyroData = floatArrayOf(event.values[0], event.values[1], event.values[2])
                    lastGyroscopeData = gyroData
                    Log.d(TAG, "Gyroscope: x=${gyroData[0]}, y=${gyroData[1]}, z=${gyroData[2]}")
                    
                    // Try to process data if we have both accelerometer and gyroscope readings
                    processDataIfComplete()
                }
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Not needed for this implementation
        }
    }
    
    /**
     * Initializes the sensors and starts data collection
     */
    fun startCollection() {
        if (isCollecting) {
            Log.d(TAG, "Data collection already started")
            return
        }
        
        // Initialize sensor manager if not already done
        if (sensorManager == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        }
        
        // Register accelerometer listener
        accelerometer?.let {
            val accelSuccess = sensorManager?.registerListener(
                sensorListener,
                it,
                samplingPeriod
            ) ?: false
            
            if (!accelSuccess) {
                Log.e(TAG, "Failed to register accelerometer listener")
            }
        } ?: Log.e(TAG, "Accelerometer sensor not available on this device")
        
        // Register gyroscope listener
        gyroscope?.let {
            val gyroSuccess = sensorManager?.registerListener(
                sensorListener,
                it,
                samplingPeriod
            ) ?: false
            
            if (!gyroSuccess) {
                Log.e(TAG, "Failed to register gyroscope listener")
            }
        } ?: Log.e(TAG, "Gyroscope sensor not available on this device")
        
        isCollecting = true
        Log.d(TAG, "Sensor data collection started")
    }
    
    /**
     * Stops the sensor data collection
     */
    fun stopCollection() {
        if (!isCollecting) {
            return
        }
        
        sensorManager?.unregisterListener(sensorListener)
        isCollecting = false
        lastAccelerometerData = null
        lastGyroscopeData = null
        previousMagnitude = null
        Log.d(TAG, "Sensor data collection stopped")
    }
    
    /**
     * Processes the sensor data if both accelerometer and gyroscope data are available
     */
    private fun processDataIfComplete() {
        val accData = lastAccelerometerData ?: return
        val gyroData = lastGyroscopeData ?: return
        
        // Calculate additional features
        val magnitude = calculateMagnitude(accData)
        val jerk = calculateJerk(magnitude)
        
        // Combine all features into a single array for model input
        val combinedData = accData + gyroData + floatArrayOf(magnitude, jerk)
        
        // Log the processed data
        Log.d(TAG, "Processed Sensor Data: ${combinedData.joinToString()}")
        
        // Update the ViewModel with the new sensor data
        uiViewModel.updateSensorData(combinedData)
    }
    
    /**
     * Calculates the magnitude of acceleration
     */
    private fun calculateMagnitude(accData: FloatArray): Float {
        // Magnitude = sqrt(x^2 + y^2 + z^2)
        return sqrt(accData[0] * accData[0] + accData[1] * accData[1] + accData[2] * accData[2])
    }
    
    /**
     * Calculates the jerk (rate of change of acceleration)
     */
    private fun calculateJerk(currentMagnitude: Float): Float {
        // Jerk = Difference in Magnitude
        val jerk = if (previousMagnitude != null) {
            currentMagnitude - previousMagnitude!!
        } else {
            0f
        }
        previousMagnitude = currentMagnitude
        return jerk
    }
    
    companion object {
        private const val TAG = "SensorDataCollector"
    }
} 