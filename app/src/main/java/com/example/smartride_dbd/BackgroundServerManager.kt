// BackgroundServerManager.kt
package com.example.smartride_dbd

import android.content.Context
import android.util.Log

/**
 * This class manages the sensor data collection and model prediction in the background
 */
class BackgroundServerManager(private val context: Context) {

    private lateinit var sensorDataCollector: SensorDataCollector
    private lateinit var modelPredictor: ModelPredictor
    private lateinit var uiViewModel: UiViewModel

    fun start() {
        // Initialize ViewModel and Model Predictor
        uiViewModel = (context.applicationContext as MyApplication).uiViewModel
        modelPredictor = ModelPredictor(context)

        // Initialize sensor data collector
        sensorDataCollector = SensorDataCollector(context, uiViewModel)
        
        // Start collecting sensor data
        sensorDataCollector.startCollection()
        Log.d(TAG, "Sensor data collection started")

        // Observe sensor data and perform prediction
        uiViewModel.sensorData.observeForever { data ->
            data?.let {
                if (it.size == 8) { // Ensure 8 features are passed
                    val predictionResult = modelPredictor.predict(it)
                    val predictionText = if (predictionResult[0] >= 0.5) "Aggressive" else "Normal"
                    uiViewModel.updatePredictionResult(predictionText) // Update prediction result
                    Log.d(TAG, "Prediction: $predictionText")
                }
            }
        }
    }

    fun stop() {
        // Stop sensor data collection and release resources
        sensorDataCollector.stopCollection()
        modelPredictor.close()
        Log.d(TAG, "Sensor data collection and Model Predictor stopped")
    }
    
    companion object {
        private const val TAG = "BackgroundServerManager"
    }
}
