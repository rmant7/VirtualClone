package com.virtualclone.app.ml.mediapipe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult

/**
 * Helper class to encapsulate MediaPipe Logic.
 */
class MediaPipeHelper(
    private val context: Context,
    private val listener: DetectorListener? = null,
    private val delegate: Int = DELEGATE_CPU
) {
    private val tag = "MediaPipeHelper"
    private var objectDetector: ObjectDetector? = null
    private var runningMode: RunningMode = RunningMode.IMAGE

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val MODEL_EFFICIENTDET = "efficientdet_lite0.tflite" // Ensure this file exists in assets!
    }

    interface DetectorListener {
        fun onError(error: String)
        fun onResults(
            result: ObjectDetectorResult,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }

    fun setupObjectDetector(
        modelPath: String = MODEL_EFFICIENTDET,
        mode: RunningMode = RunningMode.IMAGE
    ) {
        runningMode = mode
        val baseOptionsBuilder = BaseOptions.builder()

        if (delegate == DELEGATE_CPU) {
            baseOptionsBuilder.setDelegate(Delegate.CPU)
        } else if (delegate == DELEGATE_GPU) {
            baseOptionsBuilder.setDelegate(Delegate.GPU)
        }

        baseOptionsBuilder.setModelAssetPath(modelPath)

        try {
            val optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setRunningMode(mode)
                .setScoreThreshold(0.5f)
                .setMaxResults(5)

            if (mode == RunningMode.LIVE_STREAM) {
                optionsBuilder.setResultListener { result, image ->
                    val finishTime = SystemClock.uptimeMillis()
                    // Calculate inference time if needed, or just pass result
                    // Note: image is the MPImage input
                    listener?.onResults(
                        result,
                        0, // inference calcs can be complex, skipping for brevity
                        image.height,
                        image.width
                    )
                }
                optionsBuilder.setErrorListener { error ->
                    listener?.onError(error.message ?: "Unknown error")
                }
            }

            objectDetector = ObjectDetector.createFromOptions(context, optionsBuilder.build())
            Log.i(tag, "ObjectDetector created successfully in mode $mode")
        } catch (e: Exception) {
            listener?.onError("ObjectDetector creation failed: ${e.message}")
            Log.e(tag, "ObjectDetector creation failed: ${e.message}")
        }
    }

    fun detect(bitmap: Bitmap): MediaPipeResult? {
        if (objectDetector == null) setupObjectDetector(mode = RunningMode.IMAGE)

        return try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = objectDetector?.detect(mpImage)
            MediaPipeResult(result)
        } catch (e: Exception) {
            Log.e(tag, "Detection failed: ${e.message}")
            null
        }
    }

    fun detectLivestream(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        if (runningMode != RunningMode.LIVE_STREAM) {
            return
        }
        
        val frameTime = SystemClock.uptimeMillis()

        // Convert ImageProxy to Bitmap (simplified)
        val bitmap = imageProxy.toBitmap()
        
        // Rotate bitmap if needed
        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            if (isFrontCamera) {
                postScale(-1f, 1f, bitmap.width.toFloat(), bitmap.height.toFloat())
            }
        }
        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 
            0, 0, 
            bitmap.width, 
            bitmap.height, 
            matrix, 
            true
        )
        
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        objectDetector?.detectAsync(mpImage, frameTime)
        
        imageProxy.close()
    }

    fun close() {
        objectDetector?.close()
        objectDetector = null
    }

    // Wrapper
    data class MediaPipeResult(
        val rawResult: ObjectDetectorResult?
    )
}
