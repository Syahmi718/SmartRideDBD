package com.example.smartride_dbd

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DrivingDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "driving_behavior.db"
        const val DATABASE_VERSION = 2
        const val TABLE_NAME = "driving_history"
        const val COLUMN_ID = "id"
        const val COLUMN_DATE = "date"
        const val COLUMN_TIME = "time"
        const val COLUMN_DRIVING_TIME = "driving_time"
        const val COLUMN_NORMAL_COUNT = "normal_count"
        const val COLUMN_AGGRESSIVE_COUNT = "aggressive_count"
        const val COLUMN_AGGRESSIVENESS_PERCENT = "aggressiveness_percent"
        const val COLUMN_PERFORMANCE_LOSS = "performance_loss"
        const val COLUMN_DRIVE_NAME = "drive_name"
        const val COLUMN_DRIVER_NAME = "driver_name"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATE TEXT,
                $COLUMN_TIME TEXT,
                $COLUMN_DRIVING_TIME TEXT,
                $COLUMN_NORMAL_COUNT INTEGER,
                $COLUMN_AGGRESSIVE_COUNT INTEGER,
                $COLUMN_AGGRESSIVENESS_PERCENT REAL,
                $COLUMN_PERFORMANCE_LOSS REAL,
                $COLUMN_DRIVE_NAME TEXT,
                $COLUMN_DRIVER_NAME TEXT
            )
        """
        db?.execSQL(createTableQuery)
        Log.d("Database", "Table created successfully.")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db?.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_DRIVE_NAME TEXT")
            db?.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_DRIVER_NAME TEXT")
            Log.d("Database", "Database upgraded to version $newVersion.")
        }
    }

    fun insertDrivingSession(
        date: String,
        time: String,
        drivingTime: String,
        normalCount: Int,
        aggressiveCount: Int,
        performanceLoss: Double,
        driveName: String,
        driverName: String
    ) {
        val db = writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_DATE, date)
            put(COLUMN_TIME, time)
            put(COLUMN_DRIVING_TIME, drivingTime)
            put(COLUMN_NORMAL_COUNT, normalCount)
            put(COLUMN_AGGRESSIVE_COUNT, aggressiveCount)
            put(COLUMN_AGGRESSIVENESS_PERCENT, (aggressiveCount * 100) / (normalCount + aggressiveCount))
            put(COLUMN_PERFORMANCE_LOSS, performanceLoss)
            put(COLUMN_DRIVE_NAME, driveName)
            put(COLUMN_DRIVER_NAME, driverName)
        }
        val result = db.insert(TABLE_NAME, null, contentValues)
        if (result == -1L) {
            Log.e("Database", "Insertion failed.")
        } else {
            Log.d("Database", "Data inserted successfully.")
        }
        db.close()
    }

    fun getAllDrivingSessions(): List<Map<String, String>> {
        val sessions = mutableListOf<Map<String, String>>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_ID DESC", null)
        while (cursor.moveToNext()) {
            val session = mapOf(
                COLUMN_DATE to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                COLUMN_TIME to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME)),
                COLUMN_DRIVING_TIME to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DRIVING_TIME)),
                COLUMN_NORMAL_COUNT to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NORMAL_COUNT)).toString(),
                COLUMN_AGGRESSIVE_COUNT to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGGRESSIVE_COUNT)).toString(),
                COLUMN_AGGRESSIVENESS_PERCENT to cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_AGGRESSIVENESS_PERCENT)).toString(),
                COLUMN_PERFORMANCE_LOSS to cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_PERFORMANCE_LOSS)).toString(),
                COLUMN_DRIVE_NAME to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DRIVE_NAME)),
                COLUMN_DRIVER_NAME to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DRIVER_NAME))
            )
            sessions.add(session)
        }
        cursor.close()
        db.close()
        return sessions
    }

    fun getMostRecentSession(): Map<String, String>? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_ID DESC LIMIT 1", null)
        var result: Map<String, String>? = null
        if (cursor.moveToFirst()) {
            result = mapOf(
                COLUMN_DATE to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                COLUMN_TIME to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME)),
                COLUMN_DRIVING_TIME to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DRIVING_TIME)),
                COLUMN_NORMAL_COUNT to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NORMAL_COUNT)).toString(),
                COLUMN_AGGRESSIVE_COUNT to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGGRESSIVE_COUNT)).toString(),
                COLUMN_AGGRESSIVENESS_PERCENT to cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_AGGRESSIVENESS_PERCENT)).toString(),
                COLUMN_PERFORMANCE_LOSS to cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_PERFORMANCE_LOSS)).toString(),
                COLUMN_DRIVE_NAME to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DRIVE_NAME)),
                COLUMN_DRIVER_NAME to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DRIVER_NAME))
            )
        }
        cursor.close()
        db.close()
        return result
    }

    fun clearAllDrivingSessions() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_NAME")
        Log.d("Database", "All driving sessions cleared.")
        db.close()
    }
}

