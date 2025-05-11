package com.example.smartride_dbd

import android.Manifest
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Pair
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView

class MainActivity : AppCompatActivity() {

    private lateinit var backgroundServerManager: BackgroundServerManager
    private lateinit var toggleNotificationButton: Button
    private lateinit var notificationStatusTextView: TextView
    private lateinit var speedometerAnimation: LottieAnimationView
    private lateinit var httpServer: HttpServer

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Don't start the service immediately to avoid crash
            // We'll add a button to start it instead
            // startForegroundServiceWithPermissions()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        toggleNotificationButton = findViewById(R.id.toggle_notification_button)
        notificationStatusTextView = findViewById(R.id.notification_status)
        speedometerAnimation = findViewById(R.id.speedometer)
        
        // Configure Lottie animation - using the non-deprecated approach
        speedometerAnimation.setAnimation(R.raw.speedometer_animation)
        speedometerAnimation.repeatCount = com.airbnb.lottie.LottieDrawable.INFINITE
        speedometerAnimation.playAnimation()

        // Create notification channels
        NotificationUtils.createNotificationChannels(this)

        // Check and request permissions for foreground service
        if (hasRequiredPermissions()) {
            // Don't start the service immediately to avoid crash
            // We'll start it on button click instead
            // startForegroundServiceWithPermissions()
        } else {
            requestRequiredPermissions()
        }

        // Initialize and start background server
        backgroundServerManager = BackgroundServerManager(this)
        backgroundServerManager.start()

        // Set up HttpServer
        httpServer = HttpServer(8080, (application as MyApplication).uiViewModel)
        httpServer.start()

        updateNotificationState()

        toggleNotificationButton.setOnClickListener {
            showConfirmationDialog()
        }

        val monitorPredictionButton: Button = findViewById(R.id.monitor_prediction_button)
        val drivingHistoryLogButton: Button = findViewById(R.id.driving_history_log_button)
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

        drivingHistoryLogButton.setOnClickListener {
            val intent = Intent(this, DrivingHistoryLogActivity::class.java)
            
            // Create animation pairs for shared elements
            val options = ActivityOptions.makeSceneTransitionAnimation(
                this,
                Pair(drivingHistoryLogButton as View, "shared_button_transition"),
                Pair(welcomeText as View, "title_transition")
            )
            
            // Start activity with transition
            startActivity(intent, options.toBundle())
        }
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

    // This is still here but we're not calling it automatically
    private fun startForegroundServiceWithPermissions() {
        val serviceIntent = Intent(this, PredictionForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
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
        notificationStatusTextView.text = if (isEnabled) "Notification Enabled!" else "Notification Disabled!"
        notificationStatusTextView.setTextColor(
            if (isEnabled) getColor(R.color.green) else getColor(R.color.red)
        )
        toggleNotificationButton.text = if (isEnabled) "Disable Notifications" else "Enable Notifications"
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundServerManager.stop()
        httpServer.stop()

        val serviceIntent = Intent(this, PredictionForegroundService::class.java)
        stopService(serviceIntent)
    }
}
