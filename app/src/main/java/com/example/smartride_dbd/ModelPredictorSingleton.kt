package com.example.smartride_dbd

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object ModelPredictorSingleton {
    private var interpreter: Interpreter? = null

    fun initialize(context: Context) {
        if (interpreter == null) {
            interpreter = Interpreter(loadModelFile(context, "driving_behavior_model.tflite"))
        }
    }

    fun predict(inputData: FloatArray): FloatArray {
        val outputArray = Array(1) { FloatArray(1) }
        interpreter?.run(arrayOf(inputData), outputArray)
        return outputArray[0]
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )
    }
}
