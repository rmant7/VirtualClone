package com.virtualclone.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ModelSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Max Tokens
            Text(
                text = "Max Tokens: ${settings.maxTokens}",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = settings.maxTokens.toFloat(),
                onValueChange = { viewModel.updateMaxTokens(it.toInt()) },
                valueRange = 50f..500f,
                steps = 8
            )
            Text(
                text = "Maximum number of tokens for generation",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Divider()
            
            // Temperature
            Text(
                text = "Temperature: ${"%.2f".format(settings.temperature)}",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = settings.temperature,
                onValueChange = { viewModel.updateTemperature(it) },
                valueRange = 0f..1f,
                steps = 9
            )
            Text(
                text = "Controls randomness: lower is more deterministic",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Divider()
            
            // Summarization Model
            var summaryModelText by remember(settings.summarizationModel) {
                mutableStateOf(settings.summarizationModel)
            }
            
            Text(
                text = "Summarization Model",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = summaryModelText,
                onValueChange = { summaryModelText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g., facebook/bart-large-cnn") },
                singleLine = true
            )
            Button(
                onClick = { viewModel.updateSummarizationModel(summaryModelText) },
                enabled = summaryModelText != settings.summarizationModel
            ) {
                Text("Update")
            }
            
            Divider()
            
            // Q&A Model
            var qaModelText by remember(settings.qaModel) {
                mutableStateOf(settings.qaModel)
            }
            
            Text(
                text = "Q&A Model",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = qaModelText,
                onValueChange = { qaModelText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g., deepset/roberta-base-squad2") },
                singleLine = true
            )
            Button(
                onClick = { viewModel.updateQaModel(qaModelText) },
                enabled = qaModelText != settings.qaModel
            ) {
                Text("Update")
            }
        }
    }
}
