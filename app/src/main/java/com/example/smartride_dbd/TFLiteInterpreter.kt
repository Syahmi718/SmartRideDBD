package com.example.smartride_dbd

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream

class TFLiteInterpreter(context: Context) {

    private var interpreter: Interpreter

    init {
        val modelFile = try {
            loadModelFile(context, "driving_behavior_model.tflite")
        } catch (e: Exception) {
            throw RuntimeException("Error loading TFLite model", e)
        }
        interpreter = Interpreter(modelFile)
    }

    private fun loadModelFile(context: Context, modelFileName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelFileName)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        fileInputStream.use { stream ->
            val fileChannel = stream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }
    }

    fun predict(inputData: FloatArray): FloatArray {
        // Validate input size
        val inputShape = interpreter.getInputTensor(0).shape() // e.g., [1, inputSize]
        val inputSize = inputShape[1]
        if (inputData.size != inputSize) {
            throw IllegalArgumentException("Expected input size $inputSize but got ${inputData.size}")
        }

        // Create input buffer
        val inputBuffer = ByteBuffer.allocateDirect(inputData.size * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        for (value in inputData) {
            inputBuffer.putFloat(value)
        }
        inputBuffer.rewind()

        // Determine output size dynamically
        val outputSize = interpreter.getOutputTensor(0).numElements()
        val outputBuffer = ByteBuffer.allocateDirect(outputSize * 4)
        outputBuffer.order(ByteOrder.nativeOrder())

        // Run inference
        interpreter.run(inputBuffer, outputBuffer)

        // Extract results
        outputBuffer.rewind()
        val output = FloatArray(outputSize)
        for (i in output.indices) {
            output[i] = outputBuffer.float
        }

        return output
    }

    fun close() {
        interpreter.close()
    }
}
