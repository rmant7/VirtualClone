package com.virtualclone.app.feature.mediapipe.ui.components

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import kotlin.math.max
import kotlin.math.min

@Composable
fun OverlayView(
    result: ObjectDetectorResult?,
    imageHeight: Int,
    imageWidth: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        result?.detections()?.forEach { detection ->
            val boundingBox = detection.boundingBox()

            // Scale matches
            val scaleFactor = max(canvasWidth / imageWidth, canvasHeight / imageHeight)
            val boxRect = RectF(
                boundingBox.left * scaleFactor,
                boundingBox.top * scaleFactor,
                boundingBox.right * scaleFactor,
                boundingBox.bottom * scaleFactor
            )

            // Draw box
            drawRect(
                color = Color.Green,
                topLeft = Offset(boxRect.left, boxRect.top),
                size = Size(boxRect.width(), boxRect.height()),
                style = Stroke(width = 8f)
            )

            // Draw text
            val category = detection.categories().firstOrNull()
            val text = "${category?.categoryName()} ${(category?.score()?.times(100))?.toInt()}%"
            
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 50f
                    isAntiAlias = true
                }
                val bgPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    alpha = 180
                }
                
                val textWidth = paint.measureText(text)
                val textHeight = 60f
                
                drawRect(
                    boxRect.left,
                    boxRect.top - textHeight,
                    boxRect.left + textWidth + 20f,
                    boxRect.top,
                    bgPaint
                )
                
                drawText(text, boxRect.left + 10f, boxRect.top - 15f, paint)
            }
        }
    }
}
