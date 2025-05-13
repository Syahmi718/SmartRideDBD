package com.example.smartride_dbd

import android.app.Application
import android.util.Log

/**
 * Custom application class for SmartRide app.
 * 
 * This application now collects sensor data directly from device sensors,
 * without requiring the SensorLogger app.
 */
class MyApplication : Application() {
    val TAG = "MyApplication"
    var notificationsEnabled: Boolean = true
    val uiViewModel: UiViewModel by lazy { UiViewModel() }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MyApplication initialized")
    }
}
