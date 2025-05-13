package com.example.smartride_dbd

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

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
        holder.logDriveName.text = log[DrivingDatabaseHelper.COLUMN_DRIVE_NAME] ?: "Unnamed Drive"
        holder.logDriverName.text = log[DrivingDatabaseHelper.COLUMN_DRIVER_NAME] ?: "Unknown Driver"

        // Set date and time with minimalistic formatting
        holder.logDate.text = formatDate(log[DrivingDatabaseHelper.COLUMN_DATE] ?: "N/A")
        holder.logTime.text = formatTime(log[DrivingDatabaseHelper.COLUMN_TIME] ?: "N/A")

        // Set driving time
        holder.logDrivingTime.text = log[DrivingDatabaseHelper.COLUMN_DRIVING_TIME] ?: "N/A"

        // Parse normal and aggressive counts and format them better
        val normalCount = log[DrivingDatabaseHelper.COLUMN_NORMAL_COUNT]?.toIntOrNull() ?: 0
        val aggressiveCount = log[DrivingDatabaseHelper.COLUMN_AGGRESSIVE_COUNT]?.toIntOrNull() ?: 0
        
        // Format driving behavior with proper spacing and layout
        if (normalCount == 0 && aggressiveCount == 0) {
            holder.logPredictions.text = "No data recorded"
        } else {
            holder.logPredictions.text = "$normalCount normal · $aggressiveCount aggressive"
        }

        // Use Eco Score directly instead of calculating from performance loss
        val ecoScore = log[DrivingDatabaseHelper.COLUMN_ECO_SCORE]?.toIntOrNull() ?: 0
        holder.logPerformanceLoss.text = "${ecoScore}%"

        // Color for eco score
        val ecoScoreColor = when {
            ecoScore > 80 -> Color.parseColor("#4CAF50") // Green for good
            ecoScore > 50 -> Color.parseColor("#FF9800") // Orange for moderate
            else -> Color.parseColor("#F44336") // Red for poor
        }
        
        // We're not setting the text color because we're displaying it on a logo background
        // Instead, we'll change the background tint if possible, or leave as is if not applicable
    }

    override fun getItemCount() = logs.size
    
    private fun formatDate(dateStr: String): String {
        // Convert yyyy-MM-dd to "dd MMM yyyy" (e.g., "13 May 2025")
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            return date?.let { outputFormat.format(it) } ?: dateStr
        } catch (e: Exception) {
            return dateStr
        }
    }
    
    private fun formatTime(timeStr: String): String {
        // Convert HH:mm:ss to "h.mm a" (e.g., "1.07 PM")
        try {
            val inputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("h.mm a", Locale.getDefault())
            val time = inputFormat.parse(timeStr)
            return time?.let { outputFormat.format(it) } ?: timeStr
        } catch (e: Exception) {
            return timeStr
        }
    }
}
