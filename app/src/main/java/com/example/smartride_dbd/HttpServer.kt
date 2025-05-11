package com.example.smartride_dbd

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.sqrt

class HttpServer(
    private val port: Int = 8081,  // Port for Flutter communication
    private val uiViewModel: UiViewModel
) : NanoHTTPD(port) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO) // Create CoroutineScope for background tasks
    private var previousMagnitude: Float? = null // To calculate Jerk

    override fun start() {
        super.start(30 * 1000) // Set 30 seconds timeout for server socket
        Log.d("HttpServer", "Server started on port $port with timeout of 30 seconds")
        uiViewModel.updateHttpConnectionStatus(false)
    }

    override fun stop() {
        super.stop()
        Log.d("HttpServer", "Server stopped")
        uiViewModel.updateHttpConnectionStatus(false)
    }

    override fun serve(session: IHTTPSession): Response {
        Log.d("HttpServer", "New connection from IP: ${session.remoteIpAddress}, URI: ${session.uri}")
        return try {
            if (session.uri == "/data") {
                resetTimeout()

                // Read incoming data
                val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
                val reader = BufferedReader(InputStreamReader(session.inputStream))
                val payloadData = StringBuilder()
                var bytesRead = 0
                val charBuffer = CharArray(1024)

                while (bytesRead < contentLength) {
                    val count = reader.read(charBuffer)
                    if (count == -1) break
                    payloadData.append(charBuffer, 0, count)
                    bytesRead += count
                }

                Log.d("HttpServer", "Payload read (${bytesRead} bytes): ${payloadData.toString().take(1000)}...")
                val jsonObject = JSONObject(payloadData.toString())
                val payloadArray = jsonObject.getJSONArray("payload")

                // Process Sensor Data
                var accData: FloatArray? = null
                var gyroData: FloatArray? = null
                for (i in 0 until payloadArray.length()) {
                    val sensorData = payloadArray.getJSONObject(i)
                    when (sensorData.getString("name")) {
                        "accelerometer" -> {
                            accData = floatArrayOf(
                                sensorData.getJSONObject("values").getDouble("x").toFloat(),
                                sensorData.getJSONObject("values").getDouble("y").toFloat(),
                                sensorData.getJSONObject("values").getDouble("z").toFloat()
                            )
                            Log.d("HttpServer", "Parsed Accelerometer Data: ${accData.joinToString()}")
                        }
                        "gyroscope" -> {
                            gyroData = floatArrayOf(
                                sensorData.getJSONObject("values").getDouble("x").toFloat(),
                                sensorData.getJSONObject("values").getDouble("y").toFloat(),
                                sensorData.getJSONObject("values").getDouble("z").toFloat()
                            )
                            Log.d("HttpServer", "Parsed Gyroscope Data: ${gyroData.joinToString()}")
                        }
                        else -> {
                            Log.w("HttpServer", "Unrecognized sensor data: ${sensorData.getString("name")}")
                        }
                    }
                }

                if (accData != null && gyroData != null) {
                    val magnitude = calculateMagnitude(accData)
                    val jerk = calculateJerk(magnitude)
                    val combinedData = accData + gyroData + floatArrayOf(magnitude, jerk)
                    Log.d("HttpServer", "Processed Sensor Data: ${combinedData.joinToString()}")

                    // Check if aggressive driving is detected and notify Flutter


                    uiViewModel.updateSensorData(combinedData)
                } else {
                    Log.e("HttpServer", "Incomplete sensor data: Accelerometer or Gyroscope data is missing")
                }

                // Update the connection status
                uiViewModel.updateHttpConnectionStatus(true)

                newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "Data processed successfully")
            } else if (session.uri == "/sendNotification") {
                // Handle the Flutter notification request
                val response = handleNotificationRequest(session)
                newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, response)
            } else {
                Log.e("HttpServer", "Invalid endpoint: ${session.uri}")
                newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Endpoint not found")
            }
        } catch (e: Exception) {
            Log.e("HttpServer", "Error processing data", e)
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error processing data: ${e.message}")
        }
    }

    private fun handleNotificationRequest(session: IHTTPSession): String {
        // Read the body of the request (notification message)
        val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
        val reader = BufferedReader(InputStreamReader(session.inputStream))
        val payloadData = StringBuilder()
        var bytesRead = 0
        val charBuffer = CharArray(1024)

        while (bytesRead < contentLength) {
            val count = reader.read(charBuffer)
            if (count == -1) break
            payloadData.append(charBuffer, 0, count)
            bytesRead += count
        }

        val jsonObject = JSONObject(payloadData.toString())
        val message = jsonObject.getString("message")

        Log.d("HttpServer", "Received notification: $message")

        // Return a simple confirmation message to Flutter
        return "Notification received: $message"
    }

    private fun resetTimeout() {
        // You can choose to remove timeout logic if it's not necessary with Coroutines
    }

    private fun calculateMagnitude(accData: FloatArray): Float {
        // Magnitude = sqrt(x^2 + y^2 + z^2)
        return sqrt(accData[0] * accData[0] + accData[1] * accData[1] + accData[2] * accData[2])
    }

    private fun calculateJerk(currentMagnitude: Float): Float {
        // Jerk = Difference in Magnitude
        val jerk = if (previousMagnitude != null) {
            currentMagnitude - previousMagnitude!!
        } else {
            0f
        }
        previousMagnitude = currentMagnitude
        return jerk
    }



    private fun isAggressiveDriving(data: FloatArray): Boolean {
        // Implement your aggressive driving detection logic
        // Return true if the driving behavior is aggressive
        return data[0] > 1.5 // Example: This condition needs to be customized for your case
    }
}
