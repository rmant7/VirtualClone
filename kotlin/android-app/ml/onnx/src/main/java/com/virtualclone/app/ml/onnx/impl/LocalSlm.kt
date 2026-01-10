package com.virtualclone.app.ml.onnx.impl

import ai.onnxruntime.*
import android.util.Log
import com.virtualclone.app.core.common.Result
import com.virtualclone.app.ml.onnx.model.Slm
import com.virtualclone.app.ml.onnx.tokenizer.Gpt2Tokenizer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.LongBuffer
import java.util.*
import kotlin.math.*

class LocalSlm(
    private val context: android.content.Context,
    private val tokenizer: Gpt2Tokenizer
) : Slm {
    private val tag = LocalSlm::class.qualifiedName

    private var session: OrtSession? = null
    private var environment: OrtEnvironment? = null
    private var isInitialized = false

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val runtime = Runtime.getRuntime()
            Log.d(
                tag,
                "Memory stats - Free: ${runtime.freeMemory() / 1024}KB, " +
                        "Total: ${runtime.totalMemory() / 1024}KB, Max: ${runtime.maxMemory() / 1024}KB"
            )

            Log.d(tag, "Initializing ONNX Runtime environment")
            environment = OrtEnvironment.getEnvironment()

            // Load the model
            val modelPath = "models/distilgpt2.onnx"
            val fileExists = context.assets.list("models")?.contains("distilgpt2.onnx") ?: false
            if (!fileExists) {
                Log.e(tag, "Model file not found in assets/models/")
                Result.Error(FileNotFoundException("Model file not found"))
            }
//            Log.d(TAG, "Loading model from: $modelPath")
//            val modelBytes = context.assets.open(modelPath).readBytes()
//            Log.d(TAG, "Model loaded, size: ${modelBytes.size} bytes")

            // Load the model using memory-mapped file to avoid OOM
            Log.d(tag, "Loading model using memory-mapped file: $modelPath")

            // Copy asset to internal storage first
            val internalModelFile = File(context.filesDir, "distilgpt2.onnx")
            context.assets.open(modelPath).use { input ->
                FileOutputStream(internalModelFile).use { output ->
                    input.copyTo(output)
                }
            }

            Log.d(tag, "Model copied to internal storage: ${internalModelFile.length()} bytes")

            val sessionOptions = OrtSession.SessionOptions()
            sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)

            // Load from file instead of memory
            session = environment!!.createSession(internalModelFile.absolutePath, sessionOptions)
            Log.d(tag, "ONNX session created successfully using memory-mapped file")

//            session = environment!!.createSession(modelBytes, sessionOptions)
            Log.d(tag, "ONNX session created successfully")

            isInitialized = true
            Log.d(tag, "ONNX model loaded successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error initializing ONNX model", e)
            Result.Error(e)
        }
    }

    override suspend fun isReady(): Boolean = isInitialized

    override fun generate(
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topK: Int,
        topP: Float,
        stop: List<String>
    ): Flow<String> = flow {
        try {
            if (!isInitialized) {
                emit("Model not initialized. Please restart the app.")
                return@flow
            }

            val session = this@LocalSlm.session ?: return@flow
            val promptIds = tokenizer.encode(prompt)
            val generatedIds = mutableListOf<Int>()
            generatedIds.addAll(promptIds.toList())

            var currentText = ""
            val stopSequences = stop.map { tokenizer.encode(it) }

            for (step in 0 until maxTokens) {
                // Build input sequence
                val inputIds = generatedIds.toIntArray()
                val inputShape = longArrayOf(1, inputIds.size.toLong())
                val inputBuffer = LongBuffer.allocate(inputIds.size)
                inputIds.forEach { inputBuffer.put(it.toLong()) }
                inputBuffer.rewind()

                val inputTensor = OnnxTensor.createTensor(
                    environment!!,
                    inputBuffer,
                    inputShape
                )

                // Run inference
                val inputs = mapOf("input_ids" to inputTensor)
                val outputs = session.run(inputs)

                // Get logits for the last token
                val logitsTensor = outputs[0].value as OnnxTensor
                val logits = logitsTensor.floatBuffer
                val vocabSize = logits.capacity() / inputIds.size
                val lastTokenLogits = FloatArray(vocabSize)

                val startIndex = (inputIds.size - 1) * vocabSize
                for (i in 0 until vocabSize) {
                    lastTokenLogits[i] = logits.get(startIndex + i)
                }

                // Apply temperature
                val scaledLogits = lastTokenLogits.map { it / temperature }.toFloatArray()

                // Apply top-k filtering
                val topKLogits = applyTopK(scaledLogits, topK)

                // Apply top-p (nucleus) sampling
                val topPLogits = applyTopP(topKLogits, topP)

                // Sample next token
                val nextTokenId = sampleToken(topPLogits)
                generatedIds.add(nextTokenId)

                // Decode and emit
                val newText = tokenizer.decode(intArrayOf(nextTokenId))
                currentText += newText
                emit(newText)

                // Check for stop sequences
                val fullText = tokenizer.decode(generatedIds.toIntArray())
                for (stopSeq in stopSequences) {
                    if (fullText.endsWith(tokenizer.decode(stopSeq))) {
                        return@flow
                    }
                }

                // Check for EOS token
                if (nextTokenId == tokenizer.encode("</s>").firstOrNull()) {
                    return@flow
                }

                // Small delay to prevent blocking
                delay(10)
            }

        } catch (e: Exception) {
            Log.e(tag, "Error during text generation", e)
            emit("Sorry, I encountered an error while generating a response.")
        }
    }.flowOn(Dispatchers.Default)

    private fun applyTopK(logits: FloatArray, topK: Int): FloatArray {
        if (topK >= logits.size) return logits

        val sortedIndices = logits.mapIndexed { index, value -> index to value }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
            .toSet()

        return logits.mapIndexed { index, value ->
            if (index in sortedIndices) value else Float.NEGATIVE_INFINITY
        }.toFloatArray()
    }

    private fun applyTopP(logits: FloatArray, topP: Float): FloatArray {
        val sortedIndices = logits.mapIndexed { index, value -> index to value }
            .sortedByDescending { it.second }

        val probabilities = sortedIndices.map { exp(it.second) }
        val sum = probabilities.sum()
        val normalizedProbs = probabilities.map { it / sum }

        var cumulative = 0f
        val selectedIndices = mutableSetOf<Int>()

        for (i in normalizedProbs.indices) {
            cumulative += normalizedProbs[i]
            selectedIndices.add(sortedIndices[i].first)
            if (cumulative >= topP) break
        }

        return logits.mapIndexed { index, value ->
            if (index in selectedIndices) value else Float.NEGATIVE_INFINITY
        }.toFloatArray()
    }

    private fun sampleToken(logits: FloatArray): Int {
        val maxLogit = logits.maxOrNull() ?: 0f
        val expLogits = logits.map { exp(it - maxLogit) }
        val sum = expLogits.sum()
        val probabilities = expLogits.map { it / sum }

        val random = Random().nextFloat()
        var cumulative = 0f

        for (i in probabilities.indices) {
            cumulative += probabilities[i]
            if (random <= cumulative) {
                return i
            }
        }

        return probabilities.size - 1
    }

    fun cleanup() {
        try {
            session?.close()
            environment?.close()
        } catch (e: Exception) {
            Log.e(tag, "Error cleaning up ONNX resources", e)
        }
    }
}