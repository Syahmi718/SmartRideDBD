package com.example.smartride_dbd

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryLogAdapter(
    private val logs: List<Map<String, String>>
) : RecyclerView.Adapter<HistoryLogAdapter.LogViewHolder>() {

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val logDriveName: TextView = itemView.findViewById(R.id.log_drive_name)
        val logDriverName: TextView = itemView.findViewById(R.id.log_driver_name)
        val logDate: TextView = itemView.findViewById(R.id.log_date)
        val logTime: TextView = itemView.findViewById(R.id.log_time)
        val logDrivingTime: TextView = itemView.findViewById(R.id.log_driving_time)
        val logPredictions: TextView = itemView.findViewById(R.id.log_predictions)
        val logPerformanceLoss: TextView = itemView.findViewById(R.id.log_performance_loss)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.log_item, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]

        // Set drive and driver names
        holder.logDriveName.text = "Drive Name: ${log[DrivingDatabaseHelper.COLUMN_DRIVE_NAME] ?: "N/A"}"
        holder.logDriverName.text = "Driver Name: ${log[DrivingDatabaseHelper.COLUMN_DRIVER_NAME] ?: "N/A"}"

        // Set date and time
        holder.logDate.text = "Date: ${log[DrivingDatabaseHelper.COLUMN_DATE] ?: "N/A"}"
        holder.logTime.text = "Time: ${log[DrivingDatabaseHelper.COLUMN_TIME] ?: "N/A"}"

        // Set driving time
        holder.logDrivingTime.text = "Driving Time: ${log[DrivingDatabaseHelper.COLUMN_DRIVING_TIME] ?: "N/A"}"

        // Parse normal and aggressive counts
        val normalCount = log[DrivingDatabaseHelper.COLUMN_NORMAL_COUNT]?.toIntOrNull() ?: 0
        val aggressiveCount = log[DrivingDatabaseHelper.COLUMN_AGGRESSIVE_COUNT]?.toIntOrNull() ?: 0
        holder.logPredictions.text = "Normal: $normalCount, Aggressive: $aggressiveCount"

        // Color code for Normal and Aggressive counts
        holder.logPredictions.setTextColor(
            if (aggressiveCount > normalCount) Color.RED else Color.GREEN
        )

        // Parse performance loss
        val performanceLoss = log[DrivingDatabaseHelper.COLUMN_PERFORMANCE_LOSS]?.toDoubleOrNull() ?: 0.0
        holder.logPerformanceLoss.text = "Performance Loss: ${String.format("%.2f", performanceLoss)}%"

        // Color for performance loss
        val performanceColor = when {
            performanceLoss > 75 -> Color.RED
            performanceLoss > 50 -> Color.YELLOW
            else -> Color.GREEN
        }
        holder.logPerformanceLoss.setTextColor(performanceColor)
    }

    override fun getItemCount() = logs.size
}
