package com.virtualclone.app.ml.whisper

import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/**
 * Handles Mel Spectrogram extraction for Whisper models.
 * Converts raw PCM audio (16kHz) into a [1, 80, 3000] feature tensor.
 */
class WhisperFeatureExtractor {

    private val TAG = "WhisperFeatureExtractor"

    private val sampleRate = 16000
    private val nFft = 512 // Power of 2 for FFT
    private val frameLen = 400 // Whisper's window size
    private val hopLength = 160
    private val nMels = 80
    private val nSamples = 30 * sampleRate // 30 seconds of audio
    private val melLen = 3000
    
    private var melFilters: Array<FloatArray>? = null

    fun loadFilters(binFile: File) {
        try {
            val bytes = binFile.readBytes()
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            
            // Magic (4b), nMels (4b), nFft (4b)
            val magic = buffer.int 
            val fileNMels = buffer.int
            val fileNFft = buffer.int
            
            // Only use filters if they match our FFT configuration
            if (fileNMels == nMels && fileNFft == nFft) {
                val numBins = fileNFft / 2 + 1
                val filters = Array(fileNMels) { FloatArray(numBins) }
                for (m in 0 until fileNMels) {
                    for (k in 0 until numBins) {
                        filters[m][k] = buffer.float
                    }
                }
                melFilters = filters
                Log.d(TAG, "Loaded Mel filters from .bin file: nMels=$fileNMels, nFft=$fileNFft")
            } else {
                Log.w(TAG, "Filter mismatch: file($fileNMels, $fileNFft) vs extractor($nMels, $nFft). Falling back to generated filters.")
                melFilters = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load filters from .bin", e)
            melFilters = null
        }
    }

    fun extractFeatures(audio: FloatArray): Array<Array<FloatArray>> {
        // 1. Pad or trim audio to exactly 30 seconds (480,000 samples)
        val paddedAudio = FloatArray(nSamples)
        audio.copyInto(paddedAudio, 0, 0, min(audio.size, nSamples))

        // 2. STFT and Mel Spectrogram calculation
        val features = Array(1) { Array(nMels) { FloatArray(melLen) } }
        val activeFilters = melFilters ?: getMelFilters() // Fallback to calculated filters
        val window = getHanningWindow(frameLen)
        
        val fftBufferReal = DoubleArray(nFft)
        val fftBufferImag = DoubleArray(nFft)

        for (i in 0 until melLen) {
            val start = i * hopLength
            if (start + frameLen > nSamples) break

            // Apply window and pad to nFft (512)
            for (j in 0 until nFft) {
                if (j < frameLen) {
                    fftBufferReal[j] = (paddedAudio[start + j] * window[j]).toDouble()
                } else {
                    fftBufferReal[j] = 0.0 // Zero padding
                }
                fftBufferImag[j] = 0.0
            }

            // Calculate FFT (Fast Fourier Transform) - now safe with size 512
            calculateFFT(fftBufferReal, fftBufferImag)

            // Calculate Power Spectrum
            val powerSpectrum = FloatArray(nFft / 2 + 1)
            for (j in 0 until powerSpectrum.size) {
                powerSpectrum[j] = (fftBufferReal[j] * fftBufferReal[j] + fftBufferImag[j] * fftBufferImag[j]).toFloat()
            }

            // Apply Mel Filters
            val filterBank = activeFilters
            for (m in 0 until min(nMels, filterBank.size)) {
                var melEnergy = 0f
                val filter = filterBank[m]
                val numBins = min(powerSpectrum.size, filter.size)
                for (j in 0 until numBins) {
                    melEnergy += powerSpectrum[j] * filter[j]
                }
                // Log scaling (Whisper uses Log10 and a specific floor)
                features[0][m][i] = log10(max(melEnergy, 1e-10f))
            }
        }

        // Global normalization (Whisper expects features centered around 0)
        normalizeFeatures(features[0])

        return features
    }

    private fun normalizeFeatures(melSpectrogram: Array<FloatArray>) {
        var maxVal = -100f
        for (m in 0 until nMels) {
            for (i in 0 until melLen) {
                if (melSpectrogram[m][i] > maxVal) maxVal = melSpectrogram[m][i]
            }
        }
        val floor = maxVal - 8.0f
        for (m in 0 until nMels) {
            for (i in 0 until melLen) {
                melSpectrogram[m][i] = (max(melSpectrogram[m][i], floor) + 4.0f) / 4.0f
            }
        }
    }

    private fun calculateFFT(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        if (n <= 1) return

        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n) {
            if (i < j) {
                val tempReal = real[i]
                real[i] = real[j]
                real[j] = tempReal
                val tempImag = imag[i]
                imag[i] = imag[j]
                imag[j] = tempImag
            }
            var m = n shr 1
            while (m >= 1 && j >= m) {
                j -= m
                m = m shr 1
            }
            j += m
        }

        // Iterative FFT calculation
        var len = 2
        while (len <= n) {
            val ang = 2.0 * PI / len
            val wlenReal = cos(ang)
            val wlenImag = -sin(ang)
            for (i in 0 until n step len) {
                var wReal = 1.0
                var wImag = 0.0
                for (k in 0 until len / 2) {
                    val uReal = real[i + k]
                    val uImag = imag[i + k]
                    val vReal = real[i + k + len / 2] * wReal - imag[i + k + len / 2] * wImag
                    val vImag = real[i + k + len / 2] * wImag + imag[i + k + len / 2] * wReal
                    real[i + k] = uReal + vReal
                    imag[i + k] = uImag + vImag
                    real[i + k + len / 2] = uReal - vReal
                    imag[i + k + len / 2] = uImag - vImag
                    val nextWReal = wReal * wlenReal - wImag * wlenImag
                    wImag = wReal * wlenImag + wImag * wlenReal
                    wReal = nextWReal
                }
            }
            len = len shl 1
        }
    }

    private fun getHanningWindow(size: Int): FloatArray {
        val window = FloatArray(size)
        for (i in 0 until size) {
            window[i] = (0.5 * (1.0 - cos(2.0 * PI * i / (size - 1)))).toFloat()
        }
        return window
    }

    private fun getMelFilters(): Array<FloatArray> {
        val numBins = nFft / 2 + 1
        val filters = Array(nMels) { FloatArray(numBins) }
        
        // Convert frequencies to Mel scale
        fun hzToMel(hz: Float): Float = 2595f * log10(1f + hz / 700f)
        fun melToHz(mel: Float): Float = 700f * (10f.pow(mel / 2595f) - 1f)

        val minMel = hzToMel(0f)
        val maxMel = hzToMel(sampleRate / 2f)
        
        // Create nMels + 2 points linearly spaced in Mel scale
        val melPoints = FloatArray(nMels + 2)
        for (i in 0 until nMels + 2) {
            melPoints[i] = minMel + i * (maxMel - minMel) / (nMels + 1)
        }
        
        // Convert Mel points back to Hz
        val hzPoints = FloatArray(nMels + 2) { melToHz(melPoints[it]) }
        
        // Convert Hz points to FFT bin indices
        val binPoints = IntArray(nMels + 2) { 
            floor((nFft + 1) * hzPoints[it] / sampleRate).toInt().coerceIn(0, numBins - 1)
        }

        for (m in 0 until nMels) {
            val startBin = binPoints[m]
            val centerBin = binPoints[m + 1]
            val endBin = binPoints[m + 2]

            for (k in startBin until centerBin) {
                filters[m][k] = (k - startBin).toFloat() / (centerBin - startBin)
            }
            for (k in centerBin until endBin) {
                filters[m][k] = (endBin - k).toFloat() / (endBin - centerBin)
            }
        }
        return filters
    }
}
