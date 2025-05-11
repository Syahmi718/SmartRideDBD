package com.example.smartride_dbd

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class PredictionActivity : AppCompatActivity() {

    private lateinit var accelerometerChart: LineChart
    private lateinit var gyroscopeChart: LineChart
    private lateinit var predictionResultTextView: TextView
    private lateinit var connectionStatusTextView: TextView
    private lateinit var uiViewModel: UiViewModel

    private val accelerometerEntriesX = mutableListOf<Entry>()
    private val accelerometerEntriesY = mutableListOf<Entry>()
    private val accelerometerEntriesZ = mutableListOf<Entry>()

    private val gyroscopeEntriesX = mutableListOf<Entry>()
    private val gyroscopeEntriesY = mutableListOf<Entry>()
    private val gyroscopeEntriesZ = mutableListOf<Entry>()

    private var timeCounter = 0f
    private var aggressiveCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prediction)

        // Initialize UI components
        accelerometerChart = findViewById(R.id.accelerometer_chart)
        gyroscopeChart = findViewById(R.id.gyroscope_chart)
        predictionResultTextView = findViewById(R.id.prediction_result)
        connectionStatusTextView = findViewById(R.id.connection_status)

        setupCharts()

        // Access UiViewModel from MyApplication
        uiViewModel = (application as MyApplication).uiViewModel

        // Observe LiveData for updates
        observeLiveData()
    }

    override fun onDestroy() {
        super.onDestroy()
        ModelPredictorSingleton.close() // Close model interpreter
    }

    private fun setupCharts() {
        setupChart(accelerometerChart, "Accelerometer Data")
        setupChart(gyroscopeChart, "Gyroscope Data")
    }

    private fun setupChart(chart: LineChart, label: String) {
        val data = LineData()

        data.addDataSet(LineDataSet(mutableListOf(), "$label - X").apply {
            color = getColor(R.color.blue)
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
        })

        data.addDataSet(LineDataSet(mutableListOf(), "$label - Y").apply {
            color = getColor(R.color.green)
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
        })

        data.addDataSet(LineDataSet(mutableListOf(), "$label - Z").apply {
            color = getColor(R.color.red)
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
        })

        chart.data = data
        chart.description = Description().apply { text = "" }
        chart.invalidate()
    }

    private fun observeLiveData() {
        // Observe sensor data
        uiViewModel.sensorData.observe(this, Observer { data ->
            data?.let {
                if (it.size == 8) { // Ensure all 8 features are present
                    val accelerometerData = floatArrayOf(it[0], it[1], it[2])
                    val gyroscopeData = floatArrayOf(it[3], it[4], it[5])

                    updateCharts(accelerometerData, gyroscopeData)
                } else {
                    Log.e("PredictionActivity", "Incomplete sensor data")
                }
            }
        })

        // Observe prediction result
        uiViewModel.predictionResult.observe(this, Observer { result ->
            Log.d("PredictionActivity", "Observed Prediction Result: $result")
            predictionResultTextView.text = "Prediction: $result"

            // Track consecutive aggressive predictions
            if (result == "Aggressive") {
                aggressiveCount++
                if (aggressiveCount >= 5) {
                    // Reset counter after generating advice
                    aggressiveCount = 0
                }
            } else {
                aggressiveCount = 0 // Reset counter if prediction is not aggressive
            }
        })

        // Observe advice for alert
        uiViewModel.adviceForAlert.observe(this, Observer { advice ->
            if (!advice.isNullOrEmpty() && uiViewModel.shouldShowAlert()) {
                showAggressiveDrivingAlert(advice)
                uiViewModel.markAlertAsShown()
            }
        })

        // Observe HTTP connection status
        uiViewModel.httpConnectionStatus.observe(this, Observer { status ->
            connectionStatusTextView.text =
                if (status) "HTTP Connection: Established" else "HTTP Connection: Not Established"
            connectionStatusTextView.setTextColor(
                if (status) getColor(R.color.green) else getColor(R.color.red)
            )
        })
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

    private fun updateCharts(accelerometerData: FloatArray, gyroscopeData: FloatArray) {
        runOnUiThread {
            val accelerometerChartData = accelerometerChart.data ?: return@runOnUiThread
            val gyroscopeChartData = gyroscopeChart.data ?: return@runOnUiThread

            accelerometerEntriesX.add(Entry(timeCounter, accelerometerData[0]))
            accelerometerEntriesY.add(Entry(timeCounter, accelerometerData[1]))
            accelerometerEntriesZ.add(Entry(timeCounter, accelerometerData[2]))

            gyroscopeEntriesX.add(Entry(timeCounter, gyroscopeData[0]))
            gyroscopeEntriesY.add(Entry(timeCounter, gyroscopeData[1]))
            gyroscopeEntriesZ.add(Entry(timeCounter, gyroscopeData[2]))

            (accelerometerChartData.getDataSetByIndex(0) as LineDataSet).values = accelerometerEntriesX
            (accelerometerChartData.getDataSetByIndex(1) as LineDataSet).values = accelerometerEntriesY
            (accelerometerChartData.getDataSetByIndex(2) as LineDataSet).values = accelerometerEntriesZ

            (gyroscopeChartData.getDataSetByIndex(0) as LineDataSet).values = gyroscopeEntriesX
            (gyroscopeChartData.getDataSetByIndex(1) as LineDataSet).values = gyroscopeEntriesY
            (gyroscopeChartData.getDataSetByIndex(2) as LineDataSet).values = gyroscopeEntriesZ

            accelerometerChartData.notifyDataChanged()
            gyroscopeChartData.notifyDataChanged()

            accelerometerChart.notifyDataSetChanged()
            gyroscopeChart.notifyDataSetChanged()

            accelerometerChart.invalidate()
            gyroscopeChart.invalidate()

            timeCounter += 1
        }
    }

    private fun showAggressiveDrivingAlert(advice: String) {
        // Create custom dialog using the same layout as MainActivity
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
}
