package com.virtualclone.app.feature.mediapipe.ui

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.virtualclone.app.feature.mediapipe.ui.components.CameraView
import com.virtualclone.app.feature.mediapipe.ui.components.OverlayView
import com.virtualclone.app.ml.mediapipe.MediaPipeHelper

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MediaPipeRunnerScreen(
    taskId: String
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    // State for detection results - using Any? to avoid import issues
    var detectionResult by remember { mutableStateOf<com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult?>(null) }
    var inferenceTime by remember { mutableLongStateOf(0L) }
    var imageWidth by remember { mutableIntStateOf(0) }
    var imageHeight by remember { mutableIntStateOf(0) }

    // Helper setup
    val helper = remember {
        MediaPipeHelper(
            context = context,
            listener = object : MediaPipeHelper.DetectorListener {
                override fun onError(error: String) {
                    // Handle error
                }

                override fun onResults(
                    result: com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult,
                    inferenceTimeMs: Long,
                    imgHeight: Int,
                    imgWidth: Int
                ) {
                    detectionResult = result
                    inferenceTime = inferenceTimeMs
                    imageHeight = imgHeight
                    imageWidth = imgWidth
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
        // Initialize helper
        if (taskId == "object_detection") {
             helper.setupObjectDetector(mode = com.google.mediapipe.tasks.vision.core.RunningMode.LIVE_STREAM)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            helper.close()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            if (cameraPermissionState.status.isGranted) {
                CameraView(
                    onImageAnalysis = { imageProxy ->
                        helper.detectLivestream(imageProxy, isFrontCamera = false)
                    }
                )
                
                OverlayView(
                    result = detectionResult,
                    imageHeight = imageHeight,
                    imageWidth = imageWidth
                )
            } else {
                 Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                     Text("Camera Permission Required", color = Color.White)
                 }
            }

            // Google AI Edge style bottom stats
            InferenceStatsBar(
                inferenceTime = inferenceTime,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun InferenceStatsBar(
    inferenceTime: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xAA000000),
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Inference Time", fontSize = 12.sp, color = Color.LightGray)
                Text("${inferenceTime}ms", fontSize = 18.sp, color = Color.Green)
            }
            Text("Delegate: CPU", fontSize = 14.sp, color = Color.LightGray)
        }
    }
}
