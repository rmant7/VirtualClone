package com.virtualclone.app.feature.llm.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.virtualclone.app.feature.llm.viewmodel.LlmRunnerViewModel
import com.virtualclone.app.feature.llm.viewmodel.LlmUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmRunnerScreen(
    taskId: String,
    viewModel: LlmRunnerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var questionText by remember { mutableStateOf("") }
    
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadPdf(it) }
    }
    
    val title = when (taskId) {
        "summarize" -> "Summarize Document"
        "qa" -> "Ask a Question"
        else -> "LLM Runner"
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(title) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // PDF Upload Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.pdfName.isNullOrEmpty()) {
                        Button(
                            onClick = { pdfLauncher.launch("application/pdf") }
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Upload PDF")
                        }
                    } else {
                        Text(
                            text = "📄 ${uiState.pdfName}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { pdfLauncher.launch("application/pdf") }) {
                            Text("Change PDF")
                        }
                    }
                }
            }
            
            // Question input (only for Q&A)
            if (taskId == "qa") {
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text("Your Question") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
            
            // Action Button
            Button(
                onClick = {
                    when (taskId) {
                        "summarize" -> viewModel.summarize()
                        "qa" -> viewModel.askQuestion(questionText)
                    }
                },
                enabled = !uiState.pdfContent.isNullOrEmpty() && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (taskId == "summarize") "Summarize" else "Ask")
                }
            }
            
            // Error Display
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Result Display
            uiState.result?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Result",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
