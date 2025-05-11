package com.example.smartride_dbd

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ModelPredictor(context: Context) {

    private val interpreter: Interpreter
    private val mean = floatArrayOf(0.0404f, -0.0734f, 0.0082f, 0.0015f, -0.0013f, 0.0079f, 1.4378f, 0.0002f)
    private val stdDev = floatArrayOf(0.9855f, 0.9033f, 0.9850f, 0.0669f, 0.1262f, 0.1157f, 0.8349f, 0.9532f)

    init {
        interpreter = Interpreter(loadModelFile(context, "driving_behavior_model.tflite"))
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predict(inputData: FloatArray): FloatArray {
        if (inputData.size != 8) {
            Log.e("ModelPredictor", "Input data must have 8 features but got ${inputData.size}")
            return floatArrayOf(-1f)
        }
        val inputArray = arrayOf(inputData) // Wrap to 2D array [1, 8]
        val outputArray = Array(1) { FloatArray(1) } // Model output: [1, 1]

        try {
            Log.d("ModelPredictor", "Input Array: ${inputData.joinToString()}")
            interpreter.run(inputArray, outputArray)
            Log.d("ModelPredictor", "Output Array: ${outputArray[0].joinToString()}")
        } catch (e: Exception) {
            Log.e("ModelPredictor", "Error during prediction", e)
            return floatArrayOf(-1f)
        }

        return outputArray[0]
    }


    fun close() {
        interpreter.close()
    }
}
