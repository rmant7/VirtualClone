package com.virtualclone.app.core.common

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

fun calculatePeakAmplitude(buffer: ByteArray, bytesRead: Int): Int {
    val shortBuffer = ByteBuffer.wrap(buffer, 0, bytesRead)
        .order(ByteOrder.LITTLE_ENDIAN)
        .asShortBuffer()

    var maxAmplitude = 0
    while (shortBuffer.hasRemaining()) {
        val currentSample = abs(shortBuffer.get().toInt())
        if (currentSample > maxAmplitude) {
            maxAmplitude = currentSample
        }
    }
    return maxAmplitude
}
