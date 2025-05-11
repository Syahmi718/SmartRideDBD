package com.example.smartride_dbd

import android.app.Notification
import android.app.Service
import android.content.pm.ServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

class PredictionForegroundService : Service() {

    private lateinit var uiViewModel: UiViewModel
    private var aggressiveCount = 0
    private var normalCount = 0
    private var startTime: Long = 0L
    private lateinit var databaseHelper: DrivingDatabaseHelper
    private lateinit var predictionObserver: Observer<String>

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
        Log.d("PredictionService", "Service started. Driving session initialized.")

        val notification = createServiceNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }

        predictionObserver = Observer { prediction ->
            when (prediction) {
                "Aggressive" -> handleAggressivePrediction()
                "Normal" -> normalCount++
            }
        }
        uiViewModel.predictionResult.observeForever(predictionObserver)
    }

    private fun handleAggressivePrediction() {
        aggressiveCount++
        Log.d("PredictionService", "Aggressive prediction count: $aggressiveCount")

        if (aggressiveCount >= 5) {
            val advice = getRandomAdvice()
            uiViewModel.updateAdviceForAlert(advice)
            sendAggressiveNotification(advice)
            saveSessionToPreferences()
            aggressiveCount = 0
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
        val message = "Aggressive Driving: $advice"

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

        NotificationUtils.sendNotification(
            context = this,
            title = "Aggressive Driving Alert",
            message = advice
        )
    }

    private fun saveSessionToPreferences() {
        val preferences = getSharedPreferences("smart_ride_prefs", Context.MODE_PRIVATE)
        with(preferences.edit()) {
            putLong("startTime", startTime)
            putInt("normalCount", normalCount)
            putInt("aggressiveCount", aggressiveCount)
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
        saveSessionToPreferences()
        uiViewModel.predictionResult.removeObserver(predictionObserver)
        Log.d("PredictionService", "Service destroyed. Final session data saved.")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
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
