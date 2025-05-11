package com.example.smartride_dbd

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream

class TFLiteHelper(private val context: Context, modelName: String) {

    private var interpreter: Interpreter? = null

    init {
        // Load the TFLite model
        interpreter = Interpreter(loadModelFile(modelName))
    }

    private fun loadModelFile(modelName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd("driving_behavior_model.tflite")
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predict(inputData: FloatArray): FloatArray {
        // Create input and output buffers
        val inputBuffer = ByteBuffer.allocateDirect(4 * inputData.size).apply {
            order(ByteOrder.nativeOrder())
            for (value in inputData) putFloat(value)
        }
        val outputBuffer = ByteBuffer.allocateDirect(4 * 2).apply { order(ByteOrder.nativeOrder()) }

        // Run the model
        interpreter?.run(inputBuffer, outputBuffer)

        // Retrieve the results
        outputBuffer.rewind()
        val results = FloatArray(2)
        for (i in results.indices) {
            results[i] = outputBuffer.float
        }

        return results
    }

    fun close() {
        interpreter?.close()
    }
}
