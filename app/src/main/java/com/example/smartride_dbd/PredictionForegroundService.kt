package com.example.smartride_dbd

import android.app.Notification
import android.app.Service
import android.content.pm.ServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

class PredictionForegroundService : Service(), SpeedTracker.SpeedUpdateListener {

    private lateinit var uiViewModel: UiViewModel
    private var consecutiveAggressiveCount = 0
    private var normalCount = 0
    private var startTime: Long = 0L
    private lateinit var databaseHelper: DrivingDatabaseHelper
    private lateinit var predictionObserver: Observer<String>
    private val mainHandler = Handler(Looper.getMainLooper())
    private val binder = LocalBinder()
    private lateinit var speedTracker: SpeedTracker
    private var currentSpeed = 0f
    private var isDrivingSessionActive = false

    // For testing purposes - only enabled manually
    private var simulationActive = false
    private val simulationRunnable = object : Runnable {
        override fun run() {
            // Only process simulations if driving session is active
            if (!isDrivingSessionActive) {
                return
            }
            
            // Increase consecutive aggressive count
            consecutiveAggressiveCount++
            Log.d("PredictionService", "SIMULATION: Consecutive aggressive count increased to $consecutiveAggressiveCount")
            
            if (consecutiveAggressiveCount >= 5) {
                val advice = getRandomAdvice()
                Log.d("PredictionService", "SIMULATION: Triggering alert with advice: $advice")
                uiViewModel.updateAdviceForAlert(advice)
                sendAggressiveNotification(advice)
                consecutiveAggressiveCount = 0
            }
            
            if (simulationActive) {
                mainHandler.postDelayed(this, 3000) // Every 3 seconds
            }
        }
    }

    // Binder class for client binding
    inner class LocalBinder : Binder() {
        fun getService(): PredictionForegroundService = this@PredictionForegroundService
    }

    private val adviceList = listOf(
        "Let's have a cup of coffee, shall we?",
        "Take a deep breath, think of your loved ones, and relax while driving.",
        "Nothing to hurry for, sit back and drive safely!",
        "It's a beautiful day! Enjoy it with calm driving.",
        "Stay safe! Aggressive driving isn't worth it.",
        "Relax, the destination isn't running away!"
    )
    private var lastAdviceIndex = -1

    override fun onCreate() {
        super.onCreate()

        uiViewModel = (application as MyApplication).uiViewModel
        databaseHelper = DrivingDatabaseHelper(this)
        startTime = System.currentTimeMillis()
        isDrivingSessionActive = true
        Log.d("PredictionService", "Service started. Driving session initialized.")

        // Initialize speed tracker
        speedTracker = SpeedTracker(this)
        speedTracker.setSpeedListener(this)

        val notification = createServiceNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }

        predictionObserver = Observer { prediction ->
            // Only process predictions if driving session is active
            if (isDrivingSessionActive) {
                Log.d("PredictionService", "New prediction: $prediction")
                when (prediction) {
                    "Aggressive" -> handleAggressivePrediction()
                    "Normal" -> handleNormalPrediction()
                }
            }
        }
        uiViewModel.predictionResult.observeForever(predictionObserver)
        
        // Start speed tracking
        speedTracker.startTracking()
    }
    
    override fun onSpeedUpdate(speedKmh: Float) {
        // Update current speed value
        currentSpeed = speedKmh
        
        // Update UI ViewModel with speed data
        uiViewModel.updateCurrentSpeed(speedKmh)
        
        // Log current speed
        Log.d("PredictionService", "Current speed: $speedKmh km/h")
    }
    
    override fun onBind(intent: Intent?): IBinder {
        Log.d("PredictionService", "Service bound")
        return binder
    }

    fun startSimulation() {
        Log.d("PredictionService", "Starting simulation mode for testing notifications")
        simulationActive = true
        mainHandler.post(simulationRunnable)
    }
    
    fun stopSimulation() {
        simulationActive = false
        mainHandler.removeCallbacks(simulationRunnable)
        Log.d("PredictionService", "Simulation mode stopped")
    }

    private fun handleAggressivePrediction() {
        // Only process if driving session is active
        if (!isDrivingSessionActive) {
            return
        }
        
        consecutiveAggressiveCount++
        Log.d("PredictionService", "Consecutive aggressive count: $consecutiveAggressiveCount")

        if (consecutiveAggressiveCount >= 5) {
            val advice = getRandomAdvice()
            Log.d("PredictionService", "5 consecutive aggressive predictions. Triggering alert with advice: $advice")
            
            // Send the advice to the UI ViewModel to show dialog
            uiViewModel.updateAdviceForAlert(advice)
            
            // Send notification
            sendAggressiveNotification(advice)
            
            // Save session data
            saveSessionToPreferences()
            
            // Reset the counter
            consecutiveAggressiveCount = 0
        }
    }
    
    private fun handleNormalPrediction() {
        // Only process if driving session is active
        if (!isDrivingSessionActive) {
            return
        }
        
        normalCount++
        // Reset consecutive aggressive count when normal driving is detected
        if (consecutiveAggressiveCount > 0) {
            Log.d("PredictionService", "Normal driving detected, resetting consecutive aggressive count from $consecutiveAggressiveCount to 0")
            consecutiveAggressiveCount = 0
        }
    }

    private fun getRandomAdvice(): String {
        var randomIndex: Int
        do {
            randomIndex = Random.nextInt(adviceList.size)
        } while (randomIndex == lastAdviceIndex)
        lastAdviceIndex = randomIndex
        return adviceList[randomIndex]
    }

    private fun sendAggressiveNotification(advice: String) {
        // Only send notifications if driving session is active
        if (!isDrivingSessionActive) {
            return
        }
        
        val message = "Aggressive Driving: $advice"
        Log.d("PredictionService", "Sending aggressive driving notification: $message")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://10.131.77.62:8082/sendNotification")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val payload = """{"message":"$message"}"""
                OutputStreamWriter(connection.outputStream).use { it.write(payload) }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("PredictionService", "Notification sent successfully to Flutter.")
                } else {
                    Log.e("PredictionService", "Failed to send notification. Response code: $responseCode")
                }
            } catch (e: Exception) {
                Log.e("PredictionService", "Error sending notification: ${e.message}")
            }
        }

        // Don't send a local notification since we're already showing a custom dialog through the ViewModel
        // The ViewModel will trigger the dialog in MainActivity
    }

    private fun saveSessionToPreferences() {
        val preferences = getSharedPreferences("smart_ride_prefs", Context.MODE_PRIVATE)
        with(preferences.edit()) {
            putLong("startTime", startTime)
            putInt("normalCount", normalCount)
            putInt("aggressiveCount", consecutiveAggressiveCount)
            putFloat("maxSpeed", uiViewModel.maxSpeed.value ?: 0f)
            putFloat("currentSpeed", currentSpeed)
            putString("date", getCurrentDate())
            putString("time", getCurrentTime())
            apply()
        }
        Log.d("PredictionService", "Session data saved to preferences")
    }

    private fun createServiceNotification(): Notification {
        return NotificationUtils.createForegroundServiceNotification(
            context = this,
            title = "Smart Ride Active",
            message = "Monitoring your driving behavior in the background."
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        isDrivingSessionActive = false
        stopSimulation()
        speedTracker.stopTracking()
        saveSessionToPreferences()
        uiViewModel.predictionResult.removeObserver(predictionObserver)
        Log.d("PredictionService", "Service destroyed. Final session data saved.")
    }

    companion object {
        fun getCurrentDate(): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            return sdf.format(java.util.Date())
        }

        fun getCurrentTime(): String {
            val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            return sdf.format(java.util.Date())
        }
    }
}
