package com.virtualclone.app.feature.onboarding

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.virtualclone.app.core.common.UiEvent
import com.virtualclone.app.core.common.model.ModelUi
import com.virtualclone.app.feature.onboarding.ui.ModelDownloadUiItem

@Composable
fun ModelDownloadRoute(
    onOnboardingComplete: () -> Unit,
    viewModel: ModelDownloadViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showHfTokenDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomUrlDialog by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.error
                    )
                }
                is UiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        message = event.message
                    )
                }
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
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (uiState) {
                is DownloadUiState.Idle -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is DownloadUiState.Loaded -> {
                    val state = uiState as DownloadUiState.Loaded

                    ModelDownloadScreen(
                        models = state.allModels,
                        statusMap = state.statusMap,
                        userSelected = state.userSelected,
                        onModelSelectionChanged = { id, selected ->
                            viewModel.toggleSelection(id, selected)
                        },
                        onDownloadStart = { viewModel.startDownloads() },
                        onOnboardingComplete = onOnboardingComplete,
                        onDeleteConfirmed = { viewModel.cancel(it) },
                        onPause = { viewModel.pause(it) },
                        onResume = { viewModel.resume(it) },
                        onSettingsClick = {
                            showHfTokenDialog = true
                        },
                        onCustomUrlClick = {
                            showCustomUrlDialog = true
                        }
                    )
                }
            }

            if (showHfTokenDialog) {
                HfTokenDialog(
                    onSave = { token ->
                        viewModel.saveHfToken(token)
                        showHfTokenDialog = false
                    },
                    onDismiss = {
                        showHfTokenDialog = false
                    }
                )
            }

            if (showCustomUrlDialog) {
                CustomUrlDialog(
                    onDownload = { name, url ->
                        viewModel.downloadFromUrl(name, url)
                        showCustomUrlDialog = false
                    },
                    onDismiss = {
                        showCustomUrlDialog = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDownloadScreen(
    models: List<ModelUi>,
    statusMap: Map<String, ModelDownloadUiItem>,
    userSelected: Set<String>,
    onModelSelectionChanged: (String, Boolean) -> Unit,
    onDownloadStart: () -> Unit,
    onOnboardingComplete: () -> Unit,
    onDeleteConfirmed: (String) -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onCustomUrlClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteModelId by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Select Models to Download") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(
                        onClick = onCustomUrlClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Custom URL"
                        )
                    }
                    IconButton(
                        onClick = onSettingsClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(models.size) { idx ->

                    val model = models[idx]
                    val uiItem = statusMap[model.id]
                    val isSelected = userSelected.contains(model.id)

                    ModelItem(
                        model = model,
                        isSelected = isSelected,
                        uiItem = uiItem,
                        onSelect = { onModelSelectionChanged(model.id, it) },
                        onPause = { onPause(model.id) },
                        onResume = { onResume(model.id) },
                        onDeleteRequest = {
                            deleteModelId = model.id
                            showDeleteDialog = true
                        }
                    )
                }
            }

            ControlButtons(
                userSelected = userSelected,
                statusMap = statusMap,
                onDownloadStart = onDownloadStart,
                onOnboardingComplete = onOnboardingComplete
            )
        }
    }

    // delete dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    "Delete Model",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete this model?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteConfirmed(deleteModelId)
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    }
}

@Composable
fun ModelItem(
    model: ModelUi,
    isSelected: Boolean,
    uiItem: ModelDownloadUiItem?,
    onSelect: (Boolean) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDeleteRequest: () -> Unit
) {

    val isDownloading = uiItem?.status == ModelDownloadUiStatus.DOWNLOADING
    val isPaused = uiItem?.status == ModelDownloadUiStatus.PAUSED

    val canDelete = uiItem?.status in setOf(
        ModelDownloadUiStatus.COMPLETED,
        ModelDownloadUiStatus.FAILED,
        ModelDownloadUiStatus.PAUSED
    )


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (canDelete)
                    MaterialTheme.colorScheme.surfaceContainerHighest
                else
                    MaterialTheme.colorScheme.surfaceContainerLow
            )
            .clickable(enabled = !isDownloading && !canDelete) {
                onSelect(!isSelected)
            }
            .padding(16.dp)
            .animateContentSize()
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Checkbox(
                checked = isSelected || canDelete,
                onCheckedChange = { if ( !isDownloading && !canDelete) onSelect(it) },
                enabled = uiItem?.status != ModelDownloadUiStatus.DOWNLOADING
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    model.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                uiItem?.let {
                    val mb = "%.2f".format(it.downloadedMB)
                    val totalMb = "%.2f".format(it.totalMB)
                    if (model.needsAuth && totalMb == "0.00") {
                        Text(
                            "🔒 Requires Hugging Face token",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "$mb / $totalMb MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!isPaused && canDelete) {
                    Text(
                        "Downloaded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

            }

            if (canDelete) {
                IconButton(onClick = onDeleteRequest) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        uiItem?.let { status ->

            when (status.status) {

                ModelDownloadUiStatus.DOWNLOADING -> {
                    LinearProgressIndicator(
                        progress = status.progressPercentage / 100f,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${status.progressPercentage}%",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onPause) {
                            Text("Pause", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                ModelDownloadUiStatus.PAUSED -> {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        TextButton(onClick = onResume) {
                            Text("Resume", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                ModelDownloadUiStatus.FAILED -> {
                    Text(
                        "Failed",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                else -> Unit
            }
        }
    }
}


@Composable
fun ControlButtons(
    userSelected: Set<String>,
    statusMap: Map<String, ModelDownloadUiItem>,
    onDownloadStart: () -> Unit,
    onOnboardingComplete: () -> Unit
) {
    val isDownloading = statusMap.values.any {
        it.status == ModelDownloadUiStatus.DOWNLOADING
    }

    val canDownload = userSelected.any { id ->
        val st = statusMap[id]?.status
        st == ModelDownloadUiStatus.NOT_STARTED ||
                st == ModelDownloadUiStatus.PAUSED ||
                st == ModelDownloadUiStatus.FAILED ||
                st == ModelDownloadUiStatus.DELETED ||
                st == null
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val message = when {
            isDownloading -> "Downloading models..."
            userSelected.isEmpty() -> "Select models to begin."
            canDownload -> "Ready to download selected models."
            else -> ""
        }

        if (message.isNotBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (canDownload) {
            Button(
                onClick = onDownloadStart,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Download Selected Models")
            }
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = onOnboardingComplete,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = MaterialTheme.shapes.medium,
            enabled = true,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onPrimary
            ),
        ) {
            Text("Continue")
        }
    }
}
@Composable
fun HfTokenDialog(
    initialValue: String = "",
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var token by remember { mutableStateOf(initialValue) }
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hugging Face Access Token") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Text(
                    text = "Some models are gated and require a Hugging Face token to download.",
                    style = MaterialTheme.typography.bodyMedium
                )

                val annotatedText = buildAnnotatedString {
                    append("How to get a token:\n")
                    append("1. Go to ")

                    pushStringAnnotation(
                        tag = "URL",
                        annotation = "https://huggingface.co/settings/tokens"
                    )
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append("huggingface.co")
                    }
                    pop()

                    append("\n2. Sign in → Settings → Access Tokens")
                    append("\n3. Create a new token with Read access")
                    append("\n4. Copy and paste it below")
                }

                ClickableText(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    onClick = { offset ->
                        annotatedText
                            .getStringAnnotations("URL", offset, offset)
                            .firstOrNull()
                            ?.let { annotation ->
                                uriHandler.openUri(annotation.item)
                            }
                    }
                )

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("HF Read Token") },
                    singleLine = true
                )

                Text(
                    text = "Your token is stored securely on this device and is only used to download models.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(token.trim()) },
                enabled = token.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CustomUrlDialog(
    onDownload: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download Model from URL") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Model Name (e.g. whisper-tiny)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Direct URL (.tflite, .bin, etc)") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onDownload(name.trim(), url.trim()) },
                enabled = name.isNotBlank() && url.isNotBlank()
            ) {
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

