package com.virtualclone.app.feature.mediapipe.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class MediaPipeTask(
    val id: String,
    val name: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPipeTasksScreen(
    onTaskSelected: (String) -> Unit
) {
    val tasks = listOf(
        MediaPipeTask("object_detection", "Object Detection", "Detect 80+ classes of objects."),
        MediaPipeTask("image_classification", "Image Classification", "Identify what's in an image."),
        MediaPipeTask("hand_landmarker", "Hand Landmarker", "Track 21 3D hand landmarks."),
        MediaPipeTask("face_detector", "Face Detection", "Detect faces in images.")
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MediaPipe Tasks") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tasks) { task ->
                TaskItem(task, onTaskSelected)
            }
        }
    }
}

@Composable
fun TaskItem(task: MediaPipeTask, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(task.id) }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = task.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
