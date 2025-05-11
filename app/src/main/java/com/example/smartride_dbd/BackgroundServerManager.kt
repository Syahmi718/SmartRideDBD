// BackgroundServerManager.kt
package com.example.smartride_dbd

import android.content.Context
import android.util.Log
import com.example.smartride_dbd.HttpServer


class BackgroundServerManager(private val context: Context) {

    private lateinit var httpServer: HttpServer
    private lateinit var modelPredictor: ModelPredictor
    private lateinit var uiViewModel: UiViewModel

    fun start() {
        // Initialize ViewModel and Model Predictor
        uiViewModel = (context.applicationContext as MyApplication).uiViewModel
        modelPredictor = ModelPredictor(context)

        // Initialize HTTP server on port 8081 for Flutter communication
        httpServer = HttpServer(
            port = 8081,  // Changed to port 8081 for Flutter communication
            uiViewModel = uiViewModel
        )

        // Start the server
        httpServer.start()
        Log.d("BackgroundServerManager", "HTTP Server started on port 8081")

        // Observe sensor data and perform prediction
        uiViewModel.sensorData.observeForever { data ->
            data?.let {
                if (it.size == 8) { // Ensure 8 features are passed
                    val predictionResult = modelPredictor.predict(it)
                    val predictionText = if (predictionResult[0] >= 0.5) "Aggressive" else "Normal"
                    uiViewModel.updatePredictionResult(predictionText) // Update prediction result
                    Log.d("BackgroundServerManager", "Prediction: $predictionText")
                }
            }
        }
    }

    fun stop() {
        // Stop server and release resources
        httpServer.stop()
        modelPredictor.close()
        Log.d("BackgroundServerManager", "HTTP Server and Model Predictor stopped")
    }
}
