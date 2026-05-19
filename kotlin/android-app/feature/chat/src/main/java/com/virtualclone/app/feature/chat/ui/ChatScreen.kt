package com.virtualclone.app.feature.chat.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import com.virtualclone.app.core.domain.model.WhisperModelsCatalog
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.virtualclone.app.core.common.UiEvent
import com.virtualclone.app.core.domain.model.ChatMessage
import com.virtualclone.app.core.domain.model.ModelPhase
import com.virtualclone.app.feature.chat.R
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.CircularProgressIndicator
import com.virtualclone.app.feature.chat.ui.components.audio.AudioAnimation
import com.virtualclone.app.feature.chat.ui.components.audio.AudioRecorderPanel
import com.virtualclone.app.feature.chat.ui.components.AttachmentDialog


import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun ChatRoute(
    conversationId: String,
    viewModel: ChatViewModel,
    onClose: () -> Unit,
    onNavigateToDocuments: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val inputEnabled by viewModel.isTextInputEnabled.collectAsStateWithLifecycle()
    val tokens by viewModel.tokensRemaining.collectAsStateWithLifecycle()
    val modelName by viewModel.modelName.collectAsStateWithLifecycle()
    val draft by viewModel.draftMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onMicClick()
        } else {
            viewModel.onPermissionDenied("Audio recording permission is required for voice input.")
        }
    }

    // Initialize chat
    LaunchedEffect(conversationId) {
        viewModel.initialize(conversationId)
    }

    // Collect one-off UI events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage ->
                    snackbarHostState.showSnackbar(event.message)
                is UiEvent.ShowError ->
                    snackbarHostState.showSnackbar(event.error)
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { paddingValues ->
        ChatScreen(
            modifier = Modifier.padding(paddingValues),
            modelName = modelName,
            uiState = uiState,
            inputEnabled = inputEnabled,
            tokens = tokens,
            draft = draft,
            onMessageChanged = { message ->
                viewModel.onDraftChanged(message)
            },
            onSendMessage = { message ->
                viewModel.sendMessage(message)
                viewModel.clearDraft()
            },
            onResetSession = {
                viewModel.resetSession()
            },
            onClose = onClose,
            onCloseDocumentOnly = viewModel::clearSelectedDocument,
            onNavigateToDocuments = onNavigateToDocuments,
            isRecording = uiState.isRecordingAudio,
            amplitude = uiState.currentAmplitude,
            isTranscribing = uiState.isTranscribingAudio,
            onAmplitudeChanged = viewModel::onAmplitudeChanged,
            onToggleRecording = viewModel::onToggleRecording,
            onSendAudioClip = viewModel::onSendAudioClip,
            onMicClick = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    viewModel.onMicClick()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onWhisperModelSelected = viewModel::onWhisperModelSelected
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    modelName: String,
    uiState: ChatUiState,
    draft: String,
    inputEnabled: Boolean,
    tokens: Int,
    isRecording: Boolean,
    amplitude: Int,
    isTranscribing: Boolean,
    onMessageChanged: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onResetSession: () -> Unit,
    onClose: () -> Unit,
    onCloseDocumentOnly: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onAmplitudeChanged: (Int) -> Unit,
    onToggleRecording: (Boolean) -> Unit,
    onSendAudioClip: (ByteArray) -> Unit,
    onMicClick: () -> Unit,
    onWhisperModelSelected: (String) -> Unit
) {
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var showAudioRecorder by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (isRecording) {
            AudioAnimation(
                bgColor = MaterialTheme.colorScheme.surface,
                amplitude = amplitude,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            CenterAlignedTopAppBar(
            title = {
                Text(
                    text = modelName,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            actions = {
                IconButton(onClick = { showSettingsDialog = true }, enabled = inputEnabled) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
                IconButton(onClick = onResetSession, enabled = inputEnabled) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                }
                IconButton(onClick = onClose, enabled = inputEnabled) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        )

        when {
            uiState.messages.isEmpty() -> {
                EmptyChatPlaceholder(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }

            else -> {
                MessagesList(
                    messages = uiState.messages,
                    modifier = Modifier.weight(1f)
                )
            }
        }

            if (uiState.selectedDocumentName != null) {
                SelectedDocumentIndicator(
                    uiState.selectedDocumentName,
                    onCloseDocumentOnly,
                    inputEnabled
                )
            }

            if (showAudioRecorder) {
                AudioRecorderPanel(
                    onAmplitudeChanged = onAmplitudeChanged,
                    onSendAudioClip = {
                        onSendAudioClip(it)
                        showAudioRecorder = false
                        onToggleRecording(false)
                    },
                    onClose = {
                        showAudioRecorder = false
                        onToggleRecording(false)
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                ChatInputBar(
                    message = draft,
                    enabled = inputEnabled,
                    tokens = tokens,
                    isTranscribing = isTranscribing,
                    whisperDownloadProgress = uiState.whisperDownloadProgress,
                    onMessageChange = {
                        onMessageChanged(it)
                    },
                    onSend = {
                        onSendMessage(draft)
                    },
                    onAttach = { showAttachmentDialog = true },
                    onMicClick = {
                        onMicClick()
                        showAudioRecorder = true
                        onToggleRecording(true)
                    }
                )
            }
        }
    }

    if (showAttachmentDialog) {
        AttachmentDialog(
            onDismiss = { showAttachmentDialog = false },
            onDocumentsClick = {
                showAttachmentDialog = false
                onNavigateToDocuments()
            },
            onImagesClick = { showAttachmentDialog = false }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Speech Settings") },
            text = {
                Column {
                    Text("Select Whisper Model:", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    WhisperModelsCatalog.all.forEach { model ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = uiState.selectedWhisperModelId == model.id,
                                onClick = {
                                    onWhisperModelSelected(model.id)
                                    showSettingsDialog = false
                                }
                            )
                            Text(model.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun EmptyChatPlaceholder(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.SmartToy,
            contentDescription = null,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text("No messages yet")
    }
}

@Composable
private fun MessagesList(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        reverseLayout = true,
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {

        items(messages, key = { it.id }) {
            ChatItem(it)
        }
    }
}

@Composable
private fun SelectedDocumentIndicator(
    documentId: String?,
    onClear: () -> Unit,
    isEnabled: Boolean
) {
    documentId ?: return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)

    ) {
        Text(
            text = "📄 $documentId",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        IconButton(
            onClick = onClear,
            enabled = isEnabled,
            modifier = Modifier
                .size(24.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Clear")
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInputBar(
    message: String,
    enabled: Boolean,
    tokens: Int,
    isTranscribing: Boolean,
    whisperDownloadProgress: Float?,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onMicClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .wrapContentWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.End)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Tokens Remaining: $tokens",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.End
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                label = { Text(stringResource(R.string.chat_label)) },
                leadingIcon = {
                    IconButton(onClick = onAttach, enabled = enabled) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach")
                    }
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isTranscribing) {
                            if (whisperDownloadProgress != null) {
                                CircularProgressIndicator(
                                    progress = { whisperDownloadProgress },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .padding(12.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .padding(12.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        } else {
                            IconButton(onClick = onMicClick, enabled = enabled) {
                                Icon(Icons.Rounded.Mic, contentDescription = "Voice Input")
                            }
                        }
                        IconButton(
                            onClick = onSend,
                            enabled = enabled && message.isNotBlank() && tokens != 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatItem(
    chatMessage: ChatMessage
) {

    val backgroundColor = when {
        chatMessage.isFromUser ->
            MaterialTheme.colorScheme.primaryContainer

        chatMessage.isLoading ->
            MaterialTheme.colorScheme.tertiaryContainer

        chatMessage.phase == ModelPhase.THINKING ->
            MaterialTheme.colorScheme.surfaceContainerLow

        else ->
            MaterialTheme.colorScheme.secondaryContainer
    }

    val textColor = when {
        chatMessage.isFromUser ->
            MaterialTheme.colorScheme.onPrimaryContainer

        chatMessage.phase == ModelPhase.THINKING ->
            MaterialTheme.colorScheme.onSurface

        chatMessage.isLoading ->
            MaterialTheme.colorScheme.onTertiaryContainer

        else ->
            MaterialTheme.colorScheme.onSecondaryContainer
    }

    val bubbleShape = if (chatMessage.isFromUser) {
        RoundedCornerShape(21.dp, 21.dp, 8.dp, 21.dp)
    } else {
        RoundedCornerShape(21.dp, 21.dp, 21.dp, 8.dp)
    }

    val alignment =
        if (chatMessage.isFromUser) Alignment.End else Alignment.Start

    if (chatMessage.phase == ModelPhase.THINKING && chatMessage.text.isBlank()) {
        // Don't show empty thinking bubbles
        return
    }

    Column(
        horizontalAlignment = alignment,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {

        Card(
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = bubbleShape,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {

                if (chatMessage.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        gapSize = ProgressIndicatorDefaults.CircularIndicatorTrackGapSize
                    )
                }

                if (chatMessage.text.isNotBlank()) {
                    Text(
                        text = chatMessage.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }
            }
        }

        Text(
            text = when {
                chatMessage.isFromUser ->
                    stringResource(R.string.user_label)

                chatMessage.isLoading ->
                    stringResource(R.string.loading_label)

                chatMessage.phase == ModelPhase.THINKING ->
                    stringResource(R.string.thinking_label)

                else ->
                    stringResource(R.string.model_label)
            },
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
        )
    }
}