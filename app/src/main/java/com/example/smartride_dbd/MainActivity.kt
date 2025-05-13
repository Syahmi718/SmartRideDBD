package com.example.smartride_dbd

import android.Manifest
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Pair
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import androidx.lifecycle.Observer
import android.widget.EditText
import java.text.SimpleDateFormat
import java.util.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import android.widget.ImageButton

class MainActivity : AppCompatActivity() {

    private lateinit var backgroundServerManager: BackgroundServerManager
    private lateinit var simulationButton: Button
    private lateinit var notificationStatusTextView: TextView
    private lateinit var speedometerAnimation: LottieAnimationView
    private lateinit var speedValueTextView: TextView
    private lateinit var speedLimitStatusTextView: TextView
    private lateinit var uiViewModel: UiViewModel
    private var isSimulationActive = false
    private var foregroundService: PredictionForegroundService? = null
    private var serviceBound = false
    
    // Add bell button
    private lateinit var notificationBellButton: ImageButton
    
    // Add driving session variables
    private lateinit var startStopDrivingButton: Button
    private lateinit var drivingSessionStatusText: TextView
    private var isDrivingSessionActive = false
    private var currentSessionId: Long = -1L
    private lateinit var drivingSessionHelper: DrivingSessionHelper
    
    // Add monitor prediction button to control its visibility
    private lateinit var monitorPredictionButton: Button
    
    // Add these variables for storing driver and drive names
    private var driverName: String = ""
    private var driveName: String = ""
    
    // Service connection for binding to the foreground service
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? PredictionForegroundService.LocalBinder
            foregroundService = binder?.getService()
            serviceBound = true
            updateSimulationButtonState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            foregroundService = null
            serviceBound = false
            updateSimulationButtonState()
        }
    }
    
    // Request permission launcher for notifications
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Permission granted, we can now safely start the service
            startForegroundServiceWithPermissions()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the ViewModel - using either application instance or creating a new one
        try {
            uiViewModel = (application as? MyApplication)?.uiViewModel ?: UiViewModel()
        } catch (e: Exception) {
            // If casting fails, create a new instance
            uiViewModel = UiViewModel()
        }
        
        // Initialize driving session helper
        drivingSessionHelper = DrivingSessionHelper(this)
        
        // Set up an observer for advice alerts from the service
        uiViewModel.adviceForAlert.observe(this, Observer { advice ->
            if (advice.isNotEmpty() && uiViewModel.shouldShowAlert()) {
                showAggressiveDrivingAlert(advice)
                uiViewModel.markAlertAsShown()
            }
        })
        
        // Set up observer for speed data
        uiViewModel.currentSpeed.observe(this, Observer { speed ->
            updateSpeedDisplay(speed)
        })
        
        // Set up observer for speeding state
        uiViewModel.isSpeedingState.observe(this, Observer { isSpeeding ->
            updateSpeedingStatus(isSpeeding)
        })
        
        // Set up observer for speed limit changes
        uiViewModel.speedLimit.observe(this, Observer { limit ->
            speedLimitStatusTextView.text = "Speed Limit: $limit km/h"
        })

        // Initialize UI components
        speedometerAnimation = findViewById(R.id.speedometer)
        simulationButton = findViewById(R.id.simulation_button)
        speedValueTextView = findViewById(R.id.speed_value)
        speedLimitStatusTextView = findViewById(R.id.speed_limit_status)
        
        // Initialize driving session controls
        startStopDrivingButton = findViewById(R.id.start_stop_driving_button)
        drivingSessionStatusText = findViewById(R.id.driving_session_status)
        
        // Get reference to the monitor prediction button
        monitorPredictionButton = findViewById(R.id.monitor_prediction_button)
        
        // Initially hide the monitor prediction button
        monitorPredictionButton.visibility = View.GONE
        
        // Set up bottom navigation
        val bottomNavigationView = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on home, do nothing
                    true
                }
                R.id.nav_history -> {
                    // Navigate to Eco Behaviour Log
                    val intent = Intent(this, DrivingHistoryLogActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        
        // Configure Lottie animation - using the non-deprecated approach
        speedometerAnimation.setAnimation(R.raw.speedometer_animation)
        speedometerAnimation.repeatCount = com.airbnb.lottie.LottieDrawable.INFINITE
        speedometerAnimation.playAnimation()

        // Create notification channels
        NotificationUtils.createNotificationChannels(this)
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Check and request permissions for foreground service
        // We no longer start the service automatically here
        if (!hasRequiredPermissions()) {
            requestRequiredPermissions()
        }

        // Initialize and start background server
        backgroundServerManager = BackgroundServerManager(this)
        backgroundServerManager.start()

        // Set up simulation button for testing
        simulationButton.setOnClickListener {
            toggleSimulationMode()
        }
        
        // Set initial button state
        updateSimulationButtonState()
        
        // Set up the START/STOP DRIVING button click listener
        startStopDrivingButton.setOnClickListener {
            toggleDrivingSession()
        }

        val welcomeText: TextView = findViewById(R.id.welcome_text)

        monitorPredictionButton.setOnClickListener {
            val intent = Intent(this, PredictionActivity::class.java)
            
            // Create animation pairs for shared elements
            val options = ActivityOptions.makeSceneTransitionAnimation(
                this,
                Pair(monitorPredictionButton as View, "shared_button_transition"),
                Pair(welcomeText as View, "title_transition")
            )
            
            // Start activity with transition
            startActivity(intent, options.toBundle())
        }

        // Get reference to the notification bell button
        notificationBellButton = findViewById(R.id.notification_bell_button)
        
        // Set up notification bell button click listener
        notificationBellButton.setOnClickListener {
            showConfirmationDialog()
        }
        
        // Update notification bell icon state
        updateNotificationState()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check if there's a pending alert when returning to this activity
        val advice = uiViewModel.adviceForAlert.value
        if (!advice.isNullOrEmpty() && uiViewModel.shouldShowAlert()) {
            showAggressiveDrivingAlert(advice)
            uiViewModel.markAlertAsShown()
        }
    }
    
    private fun updateSimulationButtonState() {
        simulationButton.isEnabled = serviceBound && foregroundService != null
        simulationButton.text = if (isSimulationActive) "Stop Simulation" else "Start Simulation"
    }
    
    // Toggle the simulation mode for testing notifications
    private fun toggleSimulationMode() {
        val service = foregroundService
        if (service == null) {
            Toast.makeText(this, "Service not connected", Toast.LENGTH_SHORT).show()
            return
        }
        
        isSimulationActive = !isSimulationActive
        
        if (isSimulationActive) {
            service.startSimulation()
            simulationButton.text = "Stop Simulation"
            Toast.makeText(this, "Simulation started - will generate notifications", Toast.LENGTH_SHORT).show()
        } else {
            service.stopSimulation()
            simulationButton.text = "Start Simulation"
            Toast.makeText(this, "Simulation stopped", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showAggressiveDrivingAlert(advice: String) {
        // Create custom dialog
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.custom_alert_dialog, null)
        dialogBuilder.setView(dialogView)
        
        // Set the title and message
        val titleTextView = dialogView.findViewById<TextView>(R.id.alertTitleText)
        val messageTextView = dialogView.findViewById<TextView>(R.id.alertMessageText)
        val dismissButton = dialogView.findViewById<Button>(R.id.alertDismissButton)
        
        titleTextView.text = "Aggressive Driving Alert"
        messageTextView.text = advice
        
        // Create and show the dialog
        val alertDialog = dialogBuilder.create()
        alertDialog.setCancelable(false)
        
        // Set button click listener
        dismissButton.setOnClickListener {
            alertDialog.dismiss()
        }
        
        alertDialog.show()
    }

    private fun hasRequiredPermissions(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val fgServiceLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.FOREGROUND_SERVICE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return fineLocationGranted && fgServiceLocationGranted
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }

        locationPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startForegroundServiceWithPermissions() {
        val serviceIntent = Intent(this, PredictionForegroundService::class.java)
        
        // Start and bind to the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE)
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Location permissions are required to start the foreground service for driving behavior prediction.")
            .setPositiveButton("Retry") { _, _ -> requestRequiredPermissions() }
            .setNegativeButton("Exit") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun showConfirmationDialog() {
        val isCurrentlyEnabled = NotificationUtils.isNotificationEnabled(this)
        val dialogTitle = if (isCurrentlyEnabled) "Disable Notifications" else "Enable Notifications"
        val dialogMessage = if (isCurrentlyEnabled) {
            "Disabling notifications will prevent the app from alerting you about aggressive driving. Are you sure you want to disable notifications?"
        } else {
            "Enabling notifications will allow the app to alert you about aggressive driving. Are you sure you want to enable notifications?"
        }

        AlertDialog.Builder(this)
            .setTitle(dialogTitle)
            .setMessage(dialogMessage)
            .setPositiveButton("Confirm") { _, _ ->
                NotificationUtils.setNotificationEnabled(this, !isCurrentlyEnabled)
                updateNotificationState()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }

    private fun updateNotificationState() {
        val isEnabled = NotificationUtils.isNotificationEnabled(this)
        
        // Update bell icon based on notification status
        notificationBellButton.setImageResource(
            if (isEnabled) R.drawable.ic_bell_enabled else R.drawable.ic_bell_disabled
        )
        
        // Set content description for accessibility
        notificationBellButton.contentDescription = 
            if (isEnabled) "Notifications enabled (tap to disable)" else "Notifications disabled (tap to enable)"
    }
    
    override fun onStart() {
        super.onStart()
        // Bind to the service if it's running
        if (isServiceRunning()) {
            val serviceIntent = Intent(this, PredictionForegroundService::class.java)
            bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE)
        }
    }
    
    override fun onStop() {
        super.onStop()
        // Unbind from the service
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // End the driving session if active
        if (isDrivingSessionActive) {
            endDrivingSession()
        }
        
        // Stop background services
        backgroundServerManager.stop()
        
        // Unbind service if bound
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }
    
    // Check if the service is running
    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (PredictionForegroundService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun updateSpeedDisplay(speed: Float) {
        // Update the speed text
        if (speed < 0.5f) {
            // Show "Stationary" if speed is very low
            speedValueTextView.text = "Stationary"
            speedValueTextView.textSize = 24f // Slightly smaller font for text
        } else {
            // Show numeric speed with one decimal place when moving
            speedValueTextView.text = String.format("%.1f", speed)
            speedValueTextView.textSize = 36f // Original size for numbers
        }
        
        // Adjust speedometer animation based on speed
        val animationSpeed = calculateAnimationSpeed(speed)
        speedometerAnimation.speed = animationSpeed
    }
    
    private fun calculateAnimationSpeed(speedKmh: Float): Float {
        // Map speed to animation speed (adjust as needed)
        // This creates a non-linear scaling that accelerates with speed
        return when {
            speedKmh <= 0 -> 0.5f
            speedKmh <= 30 -> 0.8f
            speedKmh <= 60 -> 1.0f
            speedKmh <= 90 -> 1.5f
            else -> 2.0f
        }
    }
    
    private fun updateSpeedingStatus(isSpeeding: Boolean) {
        // Update UI to show if user is speeding
        if (isSpeeding) {
            speedLimitStatusTextView.setTextColor(getColor(R.color.red))
            speedLimitStatusTextView.text = "Exceeding Speed Limit!"
        } else {
            speedLimitStatusTextView.setTextColor(getColor(R.color.textOnPrimary))
            speedLimitStatusTextView.text = "Speed Limit: ${uiViewModel.speedLimit.value} km/h"
        }
    }

    // Add new method to toggle driving session state
    private fun toggleDrivingSession() {
        if (isDrivingSessionActive) {
            // Stop the driving session
            endDrivingSession()
        } else {
            // Start the driving session
            startDrivingSession()
        }
    }
    
    private fun startDrivingSession() {
        // Ensure we have permissions before starting
        if (!hasRequiredPermissions()) {
            requestRequiredPermissions()
            return
        }
        
        // Show dialog to get driver and drive name
        showDriverDetailsDialog()
    }
    
    private fun showDriverDetailsDialog() {
        // Inflate the dialog with custom view
        val dialogView = layoutInflater.inflate(R.layout.dialog_driver_details, null)
        
        // Get references to EditText fields
        val driveNameInput = dialogView.findViewById<EditText>(R.id.input_drive_name)
        val driverNameInput = dialogView.findViewById<EditText>(R.id.input_driver_name)
        
        // Build the dialog
        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle("Enter Session Details")
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Start Session") { _, _ ->
                // Get the text inputs
                driveName = driveNameInput.text.toString().trim()
                driverName = driverNameInput.text.toString().trim()
                
                if (driveName.isEmpty() || driverName.isEmpty()) {
                    Toast.makeText(this, "Please enter both drive name and driver name", Toast.LENGTH_SHORT).show()
                    showDriverDetailsDialog() // Show the dialog again if fields are empty
                } else {
                    // Continue with starting the driving session
                    startDrivingSessionWithDetails()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        
        // Show the dialog
        dialogBuilder.create().show()
    }
    
    private fun startDrivingSessionWithDetails() {
        // Record the session start time
        val currentTimeMillis = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentDate = dateFormat.format(Date(currentTimeMillis))
        val currentTime = timeFormat.format(Date(currentTimeMillis))
        
        // Save start time and current date/time to preferences
        getSharedPreferences("smart_ride_prefs", MODE_PRIVATE).edit().apply {
            putLong("startTime", currentTimeMillis)
            putString("date", currentDate)
            putString("time", currentTime)
            // Reset counters for new session
            putInt("aggressiveCount", 0)
            putInt("normalCount", 0)
            apply()
        }
        
        // Start a new driving session in the database
        currentSessionId = drivingSessionHelper.startDrivingSession()
        
        // Start the foreground service
        startForegroundServiceWithPermissions()
        
        // Update UI
        isDrivingSessionActive = true
        drivingSessionStatusText.text = "Driving Session: Active\nDriver: $driverName"
        startStopDrivingButton.text = "STOP DRIVING"
        startStopDrivingButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.red)
        
        // Show the monitor prediction button when driving session is active
        monitorPredictionButton.visibility = View.VISIBLE
        
        // Reset speed counter
        uiViewModel.resetSpeedData()
        
        // Start collecting sensor data for Eco Score calculation
        startAccelerometerCollection()
        
        Toast.makeText(this, "Driving session started. Drive safely!", Toast.LENGTH_SHORT).show()
    }
    
    // Acceleration sensor variables
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private val accelerometerListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER && isDrivingSessionActive) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                drivingSessionHelper.addAccelerationData(x, y, z)
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Not needed for this implementation
        }
    }
    
    private fun startAccelerometerCollection() {
        // Initialize sensor manager if not already done
        if (sensorManager == null) {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
        
        // Register listener
        accelerometer?.let {
            sensorManager?.registerListener(
                accelerometerListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }
    
    private fun stopAccelerometerCollection() {
        // Unregister listener
        sensorManager?.unregisterListener(accelerometerListener)
    }
    
    private fun endDrivingSession() {
        // Only proceed if we have an active session
        if (currentSessionId != -1L) {
            val maxSpeed = uiViewModel.maxSpeed.value ?: 0f
            val currentSpeed = uiViewModel.currentSpeed.value ?: 0f
            
            // Get counters
            val aggressiveCount = getSharedPreferences("smart_ride_prefs", MODE_PRIVATE)
                .getInt("aggressiveCount", 0)
            val normalCount = getSharedPreferences("smart_ride_prefs", MODE_PRIVATE)
                .getInt("normalCount", 0)
            
            // End the session in the database
            drivingSessionHelper.endDrivingSession(
                sessionId = currentSessionId,
                maxSpeed = maxSpeed,
                avgSpeed = currentSpeed, // Using current speed as a simple proxy
                aggressiveCount = aggressiveCount,
                normalCount = normalCount
            )
            
            // Stop collecting sensor data
            stopAccelerometerCollection()
            
            // Calculate Eco Score before saving to DrivingDatabaseHelper
            val ecoScore = calculateEcoScore(aggressiveCount, normalCount)
            
            // Save the session details to the driving database
            saveDrivingSessionToDatabaseHelper(maxSpeed, ecoScore)
            
            // Stop the foreground service
            if (serviceBound && foregroundService != null) {
                unbindService(serviceConnection)
                serviceBound = false
            }
            
            val serviceIntent = Intent(this, PredictionForegroundService::class.java)
            stopService(serviceIntent)
            
            // Update UI
            isDrivingSessionActive = false
            drivingSessionStatusText.text = "Driving Session: Not Started"
            startStopDrivingButton.text = "START DRIVING"
            startStopDrivingButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.green)
            
            // Hide the monitor prediction button when driving session is inactive
            monitorPredictionButton.visibility = View.GONE
            
            // Reset the session ID and names
            currentSessionId = -1L
            driverName = ""
            driveName = ""
            
            // Show Eco Score result
            showEcoScoreDialog(ecoScore)
            
            Toast.makeText(this, "Driving session completed and saved", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showEcoScoreDialog(ecoScore: Int) {
        // Create dialog to display Eco Score with a visual representation
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_eco_score, null)
        dialogBuilder.setView(dialogView)
        
        // Set up the Eco Score display
        val scoreTextView = dialogView.findViewById<TextView>(R.id.eco_score_value)
        val scoreDescription = dialogView.findViewById<TextView>(R.id.eco_score_description)
        val dismissButton = dialogView.findViewById<Button>(R.id.eco_score_dismiss_button)
        
        // Set the score
        scoreTextView.text = "$ecoScore"
        
        // Set the description based on score
        val description = when {
            ecoScore >= 80 -> "Great eco-driving! You're helping reduce carbon emissions."
            ecoScore >= 50 -> "Good eco-driving. Some room for improvement."
            else -> "Your driving could be more eco-friendly. Try to drive more smoothly."
        }
        scoreDescription.text = description
        
        // Set the color based on score
        val color = when {
            ecoScore >= 80 -> getColor(R.color.green)
            ecoScore >= 50 -> getColor(R.color.colorSecondary)
            else -> getColor(R.color.error)
        }
        scoreTextView.setTextColor(color)
        
        // Create and show the dialog
        val alertDialog = dialogBuilder.create()
        
        // Set button click listener
        dismissButton.setOnClickListener {
            alertDialog.dismiss()
        }
        
        alertDialog.show()
    }
    
    private fun calculateEcoScore(aggressiveCount: Int, normalCount: Int): Int {
        // In MainActivity, we only use the simplified calculation
        // The full calculation happens in DrivingSessionHelper using sensor data
        return EcoScoreCalculator.calculateSimplifiedEcoScore(
            aggressivePredictions = aggressiveCount,
            totalPredictions = aggressiveCount + normalCount
        )
    }
    
    private fun saveDrivingSessionToDatabaseHelper(maxSpeed: Float, ecoScore: Int) {
        // Get current date and time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val currentTime = timeFormat.format(Date())
        
        // Calculate actual driving time based on session start time
        val startTime = getSharedPreferences("smart_ride_prefs", MODE_PRIVATE)
            .getLong("startTime", System.currentTimeMillis())
        val drivingTimeInMillis = System.currentTimeMillis() - startTime
        val drivingTime = formatDrivingTime(drivingTimeInMillis)
        
        // Get aggressive and normal counts
        val aggressiveCount = getSharedPreferences("smart_ride_prefs", MODE_PRIVATE)
            .getInt("aggressiveCount", 0)
        val normalCount = getSharedPreferences("smart_ride_prefs", MODE_PRIVATE)
            .getInt("normalCount", 0)
        
        // Calculate performance loss based on aggressive ratio (simplified example)
        val totalCount = aggressiveCount + normalCount
        val performanceLoss = if (totalCount > 0) {
            (aggressiveCount.toDouble() / totalCount) * 100.0
        } else {
            0.0 // Default to 0% if no predictions were made
        }
        
        // Save to the DrivingDatabaseHelper
        val dbHelper = DrivingDatabaseHelper(this)
        dbHelper.insertDrivingSession(
            date = currentDate,
            time = currentTime,
            drivingTime = drivingTime,
            normalCount = normalCount,
            aggressiveCount = aggressiveCount,
            performanceLoss = performanceLoss,
            driveName = driveName,
            driverName = driverName,
            ecoScore = ecoScore
        )
    }
    
    private fun formatDrivingTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = millis / (1000 * 60 * 60)
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds)
    }
}
