package com.virtualclone.app.ml.whisper

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

private const val TAG = "WhisperTranscriber"

/**
 * Helper class for initializing and running inference with Whisper TFLite models.
 */
class WhisperTranscriber(
    private val modelFile: File,
    private val vocabFile: File? = null,
    private val filterFile: File? = null
) {

    private val featureExtractor = WhisperFeatureExtractor()
    private var vocabulary: Map<Int, String>? = null
    private var interpreter: Interpreter? = null

    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing Whisper model from: ${modelFile.absolutePath}")

            if (!modelFile.exists()) {
                throw Exception("Model file not found at ${modelFile.absolutePath}")
            }

            // Load vocabulary and filters if available
            filterFile?.let {
                if (it.exists()) {
                    featureExtractor.loadFilters(it)
                }
            }
            
            vocabFile?.let {
                if (it.exists()) {
                    loadVocabulary(it)
                }
            }

            // Use standard TFLite Interpreter (standalone)
            val options = Interpreter.Options()
            options.setNumThreads(4)
            
            interpreter = Interpreter(modelFile, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Whisper model", e)
            throw e
        }
    }

    private fun loadVocabulary(file: File) {
        if (file.name.endsWith(".bin")) {
            loadVocabularyFromBin(file)
        } else if (file.name.endsWith(".json")) {
            loadVocabularyFromJson(file)
        }
    }

    private fun loadVocabularyFromBin(binFile: File) {
        try {
            val bytes = binFile.readBytes()
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            
            val magic = buffer.int
            val nMels = buffer.int
            val nFftOrBins = buffer.int

            val numBins = if (nFftOrBins < 400) nFftOrBins else (nFftOrBins / 2 + 1)
            
            val filterDataSize = nMels * numBins * 4
            buffer.position(buffer.position() + filterDataSize)
            
            val nVocab = buffer.int
            val map = mutableMapOf<Int, String>()
            for (i in 0 until nVocab) {
                val len = buffer.int
                val wordBytes = ByteArray(len)
                buffer.get(wordBytes)
                map[i] = String(wordBytes)
            }
            vocabulary = map
            Log.d(TAG, "Loaded vocabulary with ${map.size} tokens from .bin file")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse filters_vocab_multilingual.bin", e)
        }
    }

    private fun loadVocabularyFromJson(vocabFile: File) {
        try {
            val jsonString = vocabFile.readText()
            val jsonObject = JSONObject(jsonString)
            val map = mutableMapOf<Int, String>()
            jsonObject.keys().forEach { key ->
                val id = key.toIntOrNull()
                if (id != null) {
                    map[id] = jsonObject.getString(key)
                } else {
                    val token = key
                    val tokenId = jsonObject.getInt(key)
                    map[tokenId] = token
                }
            }
            vocabulary = map
            Log.d(TAG, "Loaded vocabulary from vocab.json")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse vocab.json", e)
        }
    }

    suspend fun transcribe(
        audioData: ByteArray
    ): String = withContext(Dispatchers.Default) {
        val currentInterpreter = interpreter ?: throw Exception("Model not initialized")

        try {
            Log.d(TAG, "Running transcription on ${audioData.size} bytes of audio...")

            val audioFloats = decodePcmToFloat(audioData)
            
            // Simple Voice Activity Detection (VAD) check
            val energy = calculateAudioEnergy(audioFloats)
            if (energy < 0.01f) { // Threshold for silence (range 0.0 to 1.0)
                return@withContext "[No speech detected - Input is too quiet]"
            }

            val inputFeatures = featureExtractor.extractFeatures(audioFloats)
            
            // Dynamically get output shape
            val outputTensor = currentInterpreter.getOutputTensor(0)
            val outputShape = outputTensor.shape() // e.g., [1, 451] or [1, 448]
            val outputBuffer = Array(outputShape[0]) { IntArray(outputShape[1]) }

            currentInterpreter.run(inputFeatures, outputBuffer)

            // Decode Tokens
            val tokens = outputBuffer[0].filter { it > 0 }
            val decodedText = decodeTokens(tokens)
            
            return@withContext decodedText

        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            throw e
        }
    }

    private fun decodeTokens(tokens: List<Int>): String {
        val vocab = vocabulary
        if (vocab == null) {
            return "Detected Token IDs: ${tokens.take(20).joinToString(", ")}..."
        }

        val sb = StringBuilder()
        for (token in tokens) {
            // Whisper special tokens are usually > 50257
            if (token >= 50257) continue 
            
            val word = vocab[token] ?: " "
            // Clean up Byte-Pair Encoding characters (Whisper often uses Ġ for space)
            val cleanWord = word.replace("Ġ", " ").replace("Ċ", "\n")
            sb.append(cleanWord)
        }
        
        val result = sb.toString().trim()
        return if (result.isEmpty()) "[No speech detected]" else result
    }

    private fun calculateAudioEnergy(audioData: FloatArray): Float {
        var sum = 0f
        for (sample in audioData) {
            sum += sample * sample
        }
        return if (audioData.isNotEmpty()) Math.sqrt((sum / audioData.size).toDouble()).toFloat() else 0f
    }

    private fun decodePcmToFloat(audioData: ByteArray): FloatArray {
        val shortBuffer = ByteBuffer.wrap(audioData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val floatArray = FloatArray(shortBuffer.limit())
        for (i in 0 until shortBuffer.limit()) {
            floatArray[i] = shortBuffer.get(i) / 32768.0f
        }
        return floatArray
    }

    fun release() {
        interpreter?.close()
        interpreter = null
    }
}
