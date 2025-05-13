package com.example.smartride_dbd

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import android.view.View

class DrivingHistoryLogActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DrivingDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driving_history_log)

        // Initialize UI components
        val clearHistoryButton: Button = findViewById(R.id.clear_history_button)
        val recyclerView: RecyclerView = findViewById(R.id.history_log_recycler_view)
        val emptyStateView: View = findViewById(R.id.empty_state)

        // Initialize database helper
        databaseHelper = DrivingDatabaseHelper(this)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Display driving history
        displayDrivingHistory(recyclerView, emptyStateView)

        // Handle "Clear History" button click
        clearHistoryButton.setOnClickListener {
            // Show confirmation dialog before clearing
            AlertDialog.Builder(this)
                .setTitle("Clear Eco Behaviour Log")
                .setMessage("Are you sure you want to clear all your eco-driving records? This action cannot be undone.")
                .setPositiveButton("Clear") { _, _ ->
                    databaseHelper.clearAllDrivingSessions()
                    displayDrivingHistory(recyclerView, emptyStateView)
                    Toast.makeText(this, "Eco behaviour log cleared!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun displayDrivingHistory(recyclerView: RecyclerView, emptyStateView: View) {
        val drivingHistory = databaseHelper.getAllDrivingSessions()

        if (drivingHistory.isNotEmpty()) {
            val adapter = HistoryLogAdapter(drivingHistory)
            recyclerView.adapter = adapter
            emptyStateView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.GONE
            emptyStateView.visibility = View.VISIBLE
            Toast.makeText(this, "No eco-driving records found.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun promptForDetailsAndSave() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_driver_details, null)
        val driveNameInput = dialogView.findViewById<android.widget.EditText>(R.id.input_drive_name)
        val driverNameInput = dialogView.findViewById<android.widget.EditText>(R.id.input_driver_name)

        AlertDialog.Builder(this)
            .setTitle("End Drive Session")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val driveName = driveNameInput.text.toString()
                val driverName = driverNameInput.text.toString()
                if (driveName.isNotBlank() && driverName.isNotBlank()) {
                    saveAndShowMostRecentLog(driveName, driverName)
                } else {
                    Toast.makeText(this, "Please provide both Drive Name and Driver Name.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun saveAndShowMostRecentLog(driveName: String, driverName: String) {
        Log.d("DrivingHistory", "saveAndShowMostRecentLog called")

        // Save the current session
        saveDrivingData(driveName, driverName)

        // Fetch the most recent session
        val recentSession = databaseHelper.getMostRecentSession()
        if (recentSession != null) {
            val summary = """
                Drive Name: ${recentSession[DrivingDatabaseHelper.COLUMN_DRIVE_NAME]}
                Driver Name: ${recentSession[DrivingDatabaseHelper.COLUMN_DRIVER_NAME]}
                Date: ${formatDateTime(recentSession[DrivingDatabaseHelper.COLUMN_DATE])}
                Time: ${formatDateTime(recentSession[DrivingDatabaseHelper.COLUMN_TIME])}
                Driving Time: ${recentSession[DrivingDatabaseHelper.COLUMN_DRIVING_TIME]}
                Normal Predictions: ${recentSession[DrivingDatabaseHelper.COLUMN_NORMAL_COUNT]}
                Aggressive Predictions: ${recentSession[DrivingDatabaseHelper.COLUMN_AGGRESSIVE_COUNT]}
                Aggressiveness Percentage: ${recentSession[DrivingDatabaseHelper.COLUMN_AGGRESSIVENESS_PERCENT]}%
                Performance Loss: ${recentSession[DrivingDatabaseHelper.COLUMN_PERFORMANCE_LOSS]}%
            """.trimIndent()

            // Show summary dialog
            AlertDialog.Builder(this)
                .setTitle("Driving Session Summary")
                .setMessage(summary)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        } else {
            Toast.makeText(this, "No recent session found!", Toast.LENGTH_SHORT).show()
            Log.e("DrivingHistory", "Failed to fetch the most recent session")
        }
    }

    private fun saveDrivingData(driveName: String, driverName: String) {
        val sessionData = loadSessionFromPreferences()
        val startTime = sessionData["startTime"] as Long? ?: 0L
        val currentTime = System.currentTimeMillis()
        val drivingTimeInMillis = currentTime - startTime
        val drivingTime = formatDrivingTime(drivingTimeInMillis) // Call formatDrivingTime here
        val normalCount = sessionData["normalCount"] as Int
        val aggressiveCount = sessionData["aggressiveCount"] as Int

        // Calculate performance loss
        val performanceLoss = calculatePerformanceLoss(aggressiveCount, normalCount, drivingTimeInMillis)

        // Save session data to the database
        databaseHelper.insertDrivingSession(
            date = sessionData["date"] as String,
            time = sessionData["time"] as String,
            drivingTime = drivingTime,
            normalCount = normalCount,
            aggressiveCount = aggressiveCount,
            performanceLoss = performanceLoss,
            driveName = driveName,
            driverName = driverName
        )
        Log.d("DrivingHistory", """
        Driving session data saved:
        Drive Name: $driveName
        Driver Name: $driverName
        Date: ${sessionData["date"]}
        Time: ${sessionData["time"]}
        Driving Time: $drivingTime
        Normal Predictions: $normalCount
        Aggressive Predictions: $aggressiveCount
        Performance Loss: $performanceLoss%
    """.trimIndent())
    }

    private fun loadSessionFromPreferences(): Map<String, Any?> {
        val preferences = getSharedPreferences("smart_ride_prefs", Context.MODE_PRIVATE)
        return mapOf(
            "startTime" to preferences.getLong("startTime", 0L),
            "normalCount" to preferences.getInt("normalCount", 0),
            "aggressiveCount" to preferences.getInt("aggressiveCount", 0),
            "date" to preferences.getString("date", "Unknown"),
            "time" to preferences.getString("time", "Unknown")
        )
    }

    private fun calculatePerformanceLoss(
        aggressiveCount: Int,
        normalCount: Int,
        drivingTimeInMillis: Long
    ): Double {
        val totalPredictions = aggressiveCount + normalCount
        if (totalPredictions == 0) return 0.0

        val drivingTimeInMinutes = drivingTimeInMillis / (1000 * 60)
        val thresholdTime = 120.0 // Example threshold in minutes

        // Weights for factors
        val aggressiveWeight = 0.7
        val timeWeight = 0.3

        // Factors
        val aggressiveScore = aggressiveCount.toDouble() / totalPredictions
        
        // Avoid divide by zero with thresholdTime
        val timePenalty = if (thresholdTime > 0) {
            drivingTimeInMinutes / thresholdTime
        } else {
            0.0
        }

        // Calculate weighted performance loss
        return (aggressiveScore * aggressiveWeight + timePenalty * timeWeight) * 100
    }

    private fun formatDateTime(dateTime: String?): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault())
            val parsedDate = parser.parse(dateTime ?: "")
            if (parsedDate != null) formatter.format(parsedDate) else dateTime ?: "Unknown"
        } catch (e: Exception) {
            dateTime ?: "Unknown"
        }
    }
    private fun formatDrivingTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = millis / (1000 * 60 * 60)
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds)
    }

}
