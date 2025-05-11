package com.example.smartride_dbd

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DrivingSummaryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driving_summary) // Ensure the layout file exists and is correctly named

        // Initialize UI components
        val summaryTextView: TextView = findViewById(R.id.summary_text_view)

        // Retrieve data passed from DrivingHistoryLogActivity
        val intentData = intent.extras
        val drivingSummary = intentData?.getString("summary") ?: "No driving history summary available."

        // Display the summary
        summaryTextView.text = drivingSummary
    }
}
