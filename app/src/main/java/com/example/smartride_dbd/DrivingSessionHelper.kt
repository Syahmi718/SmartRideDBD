package com.example.smartride_dbd

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DrivingSessionHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "DrivingSessionHelper"
        private const val DATABASE_NAME = "driving_sessions.db"
        private const val DATABASE_VERSION = 2  // Increased version for schema update

        // Table name
        private const val TABLE_SESSIONS = "driving_sessions"

        // Column names
        private const val KEY_ID = "id"
        private const val KEY_DATE = "date"
        private const val KEY_START_TIME = "start_time"
        private const val KEY_END_TIME = "end_time"
        private const val KEY_DURATION = "duration_minutes"
        private const val KEY_MAX_SPEED = "max_speed"
        private const val KEY_AVG_SPEED = "avg_speed"
        private const val KEY_AGGRESSIVE_COUNT = "aggressive_count"
        private const val KEY_NORMAL_COUNT = "normal_count"
        private const val KEY_ECO_SCORE = "eco_score"  // New column for Eco Score
    }
    
    // Temporary storage for acceleration data for Eco Score calculation
    private val accelerationData = mutableListOf<Triple<Float, Float, Float>>()
    private val accelerationTimestamps = mutableListOf<Long>()

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_SESSIONS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_DATE TEXT,
                $KEY_START_TIME TEXT,
                $KEY_END_TIME TEXT,
                $KEY_DURATION REAL,
                $KEY_MAX_SPEED REAL,
                $KEY_AVG_SPEED REAL,
                $KEY_AGGRESSIVE_COUNT INTEGER,
                $KEY_NORMAL_COUNT INTEGER,
                $KEY_ECO_SCORE INTEGER
            )
        """.trimIndent()

        db.execSQL(createTableQuery)
        Log.d(TAG, "Database tables created")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Add Eco Score column for version 2
            db.execSQL("ALTER TABLE $TABLE_SESSIONS ADD COLUMN $KEY_ECO_SCORE INTEGER DEFAULT 0")
            Log.d(TAG, "Added Eco Score column to existing database")
        }
    }

    /**
     * Add acceleration data for Eco Score calculation
     */
    fun addAccelerationData(x: Float, y: Float, z: Float) {
        val timestamp = System.currentTimeMillis()
        accelerationData.add(Triple(x, y, z))
        
        // Only add timestamp for differences
        if (accelerationTimestamps.isEmpty() || timestamp - accelerationTimestamps.last() > 0) {
            accelerationTimestamps.add(timestamp)
        }
        
        // Prevent unbounded growth by limiting to last 1000 data points
        if (accelerationData.size > 1000) {
            accelerationData.removeAt(0)
        }
        if (accelerationTimestamps.size > 1000) {
            accelerationTimestamps.removeAt(0)
        }
    }
    
    /**
     * Reset acceleration data (e.g., when starting a new session)
     */
    fun resetAccelerationData() {
        accelerationData.clear()
        accelerationTimestamps.clear()
    }

    /**
     * Create a new driving session entry
     */
    fun startDrivingSession(): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_DATE, getCurrentDate())
            put(KEY_START_TIME, getCurrentTime())
            put(KEY_END_TIME, "")
            put(KEY_DURATION, 0.0)
            put(KEY_MAX_SPEED, 0.0)
            put(KEY_AVG_SPEED, 0.0)
            put(KEY_AGGRESSIVE_COUNT, 0)
            put(KEY_NORMAL_COUNT, 0)
            put(KEY_ECO_SCORE, 0)  // Default eco score
        }

        // Reset acceleration data for new session
        resetAccelerationData()

        // Insert the new row, returning the primary key
        val id = db.insert(TABLE_SESSIONS, null, values)
        db.close()
        
        Log.d(TAG, "Started new driving session with ID: $id")
        return id
    }

    /**
     * Update session details when the session ends
     */
    fun endDrivingSession(
        sessionId: Long,
        maxSpeed: Float,
        avgSpeed: Float,
        aggressiveCount: Int,
        normalCount: Int
    ): Boolean {
        val db = this.writableDatabase
        val endTime = getCurrentTime()
        
        // Calculate duration in minutes
        val startTime = getSessionStartTime(sessionId)
        val durationMinutes = if (startTime.isNotEmpty()) {
            calculateDurationMinutes(startTime, endTime)
        } else {
            0.0
        }

        // Calculate Eco Score
        val ecoScore = calculateEcoScore(aggressiveCount, normalCount)

        val values = ContentValues().apply {
            put(KEY_END_TIME, endTime)
            put(KEY_DURATION, durationMinutes)
            put(KEY_MAX_SPEED, maxSpeed)
            put(KEY_AVG_SPEED, avgSpeed)
            put(KEY_AGGRESSIVE_COUNT, aggressiveCount)
            put(KEY_NORMAL_COUNT, normalCount)
            put(KEY_ECO_SCORE, ecoScore)
        }

        // Update the session
        val result = db.update(TABLE_SESSIONS, values, "$KEY_ID = ?", arrayOf(sessionId.toString())) > 0
        db.close()
        
        Log.d(TAG, "Ended driving session $sessionId, duration: $durationMinutes min, Eco Score: $ecoScore")
        return result
    }
    
    /**
     * Calculate the Eco Score based on available data
     */
    private fun calculateEcoScore(aggressiveCount: Int, normalCount: Int): Int {
        // If we have acceleration data, use the full Eco Score calculation
        return if (accelerationData.size > 10) {
            // Calculate delta times between acceleration samples
            val deltaTimeMillis = mutableListOf<Long>()
            for (i in 1 until accelerationTimestamps.size) {
                deltaTimeMillis.add(accelerationTimestamps[i] - accelerationTimestamps[i-1])
            }
            
            EcoScoreCalculator.calculateEcoScore(
                accelerationData = accelerationData,
                deltaTimeMillis = deltaTimeMillis,
                aggressivePredictions = aggressiveCount,
                totalPredictions = aggressiveCount + normalCount
            )
        } else {
            // Fallback to simplified calculation if not enough acceleration data
            EcoScoreCalculator.calculateSimplifiedEcoScore(
                aggressivePredictions = aggressiveCount,
                totalPredictions = aggressiveCount + normalCount
            )
        }
    }
    
    /**
     * Get start time for a specific session
     */
    private fun getSessionStartTime(sessionId: Long): String {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_SESSIONS,
            arrayOf(KEY_START_TIME),
            "$KEY_ID = ?",
            arrayOf(sessionId.toString()),
            null, null, null
        )
        
        var startTime = ""
        if (cursor.moveToFirst()) {
            startTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_START_TIME))
        }
        cursor.close()
        return startTime
    }
    
    /**
     * Get all driving sessions for display
     */
    fun getAllDrivingSessions(): List<DrivingSession> {
        val sessionsList = mutableListOf<DrivingSession>()
        val selectQuery = "SELECT * FROM $TABLE_SESSIONS ORDER BY $KEY_ID DESC"
        
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        
        if (cursor.moveToFirst()) {
            do {
                val ecoScoreIndex = cursor.getColumnIndex(KEY_ECO_SCORE)
                val ecoScore = if (ecoScoreIndex != -1) {
                    cursor.getInt(ecoScoreIndex)
                } else {
                    // If column doesn't exist in older database version
                    calculateLegacyEcoScore(
                        cursor.getInt(cursor.getColumnIndexOrThrow(KEY_AGGRESSIVE_COUNT)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(KEY_NORMAL_COUNT))
                    )
                }
                
                val session = DrivingSession(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                    date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE)),
                    startTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_START_TIME)),
                    endTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_END_TIME)),
                    durationMinutes = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_DURATION)),
                    maxSpeed = cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_MAX_SPEED)),
                    avgSpeed = cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_AVG_SPEED)),
                    aggressiveCount = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_AGGRESSIVE_COUNT)),
                    normalCount = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_NORMAL_COUNT)),
                    ecoScore = ecoScore
                )
                sessionsList.add(session)
            } while (cursor.moveToNext())
        }
        cursor.close()
        
        return sessionsList
    }
    
    /**
     * Calculate a legacy eco score for older database entries
     */
    private fun calculateLegacyEcoScore(aggressiveCount: Int, normalCount: Int): Int {
        return EcoScoreCalculator.calculateSimplifiedEcoScore(
            aggressivePredictions = aggressiveCount,
            totalPredictions = aggressiveCount + normalCount
        )
    }
    
    /**
     * Calculate duration between two times in minutes
     */
    private fun calculateDurationMinutes(startTime: String, endTime: String): Double {
        try {
            val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val startDate = format.parse(startTime)
            val endDate = format.parse(endTime)
            
            if (startDate != null && endDate != null) {
                // Calculate difference in milliseconds
                val diffMs = endDate.time - startDate.time
                // Convert to minutes
                return diffMs / (1000.0 * 60.0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating duration: ${e.message}")
        }
        return 0.0
    }
    
    /**
     * Get formatted date
     */
    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    /**
     * Get formatted time
     */
    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}

/**
 * Data class to represent a driving session
 */
data class DrivingSession(
    val id: Long,
    val date: String,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Double,
    val maxSpeed: Float,
    val avgSpeed: Float,
    val aggressiveCount: Int,
    val normalCount: Int,
    val ecoScore: Int = 0  // Default to 0 if not provided
) 