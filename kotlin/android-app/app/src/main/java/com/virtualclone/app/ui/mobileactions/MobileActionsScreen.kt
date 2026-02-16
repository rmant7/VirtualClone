package com.virtualclone.app.ui.mobileactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.virtualclone.app.core.design.theme.MobileActionsGradientEnd
import com.virtualclone.app.core.design.theme.MobileActionsGradientStart

data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val command: String
)

data class ActionResult(
    val action: String,
    val parameters: String,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileActionsScreen(
    onNavigateBack: () -> Unit = {}
) {
    var command by remember { mutableStateOf("") }
    var actionResult by remember { mutableStateOf<ActionResult?>(null) }

    val quickActions = listOf(
        QuickAction("Flashlight", Icons.Default.FlashlightOn, "Turn on the flashlight"),
        QuickAction("Set alarm", Icons.Default.Alarm, "Set an alarm for 7 AM"),
        QuickAction("Settings", Icons.Default.Settings, "Open device settings"),
        QuickAction("Screenshot", Icons.Default.Screenshot, "Take a screenshot")
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Mobile Actions", fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Privacy Banner ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MobileActionsGradientStart.copy(alpha = 0.08f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MobileActionsGradientStart
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Using on-device AI — no data leaves your phone",
                        style = MaterialTheme.typography.labelMedium,
                        color = MobileActionsGradientStart,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Quick Actions ──
            Text(
                text = "Quick actions",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickActions.forEach { qa ->
                    AssistChip(
                        onClick = {
                            command = qa.command
                            actionResult = ActionResult(
                                action = qa.label,
                                parameters = qa.command,
                                status = "Ready to execute"
                            )
                        },
                        label = { Text(qa.label) },
                        leadingIcon = {
                            Icon(
                                qa.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            leadingIconContentColor = MobileActionsGradientStart
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Command Input ──
            Text(
                text = "What would you like to do?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("e.g. \"Turn on Wi-Fi\"") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MobileActionsGradientStart,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        cursorColor = MobileActionsGradientStart
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilledIconButton(
                    onClick = {
                        if (command.isNotBlank()) {
                            // TODO: replace with on-device LLM function calling
                            actionResult = parseAction(command)
                        }
                    },
                    enabled = command.isNotBlank(),
                    modifier = Modifier.size(52.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MobileActionsGradientStart
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Execute",
                        tint = Color.White
                    )
                }
            }

            // ── Result Card ──
            AnimatedVisibility(
                visible = actionResult != null,
                enter = fadeIn() + slideInVertically { it / 2 }
            ) {
                actionResult?.let { result ->
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        MobileActionsGradientStart,
                                                        MobileActionsGradientEnd
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PhoneAndroid,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Parsed Action",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                ActionRow(label = "Action", value = result.action)
                                Spacer(modifier = Modifier.height(8.dp))
                                ActionRow(label = "Parameters", value = result.parameters)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFF34D399)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = result.status,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF34D399),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ActionRow(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Simple command parser — will be replaced by on-device LLM function calling.
 */
private fun parseAction(command: String): ActionResult {
    val lower = command.lowercase()
    return when {
        lower.contains("flashlight") || lower.contains("torch") ->
            ActionResult("TOGGLE_FLASHLIGHT", "state=ON", "Ready to execute")
        lower.contains("alarm") -> {
            val time = Regex("\\d+\\s*(am|pm|AM|PM)?").find(lower)?.value ?: "unknown"
            ActionResult("SET_ALARM", "time=$time", "Ready to execute")
        }
        lower.contains("setting") ->
            ActionResult("OPEN_SETTINGS", "page=main", "Ready to execute")
        lower.contains("screenshot") ->
            ActionResult("TAKE_SCREENSHOT", "delay=0", "Ready to execute")
        lower.contains("wifi") || lower.contains("wi-fi") ->
            ActionResult("TOGGLE_WIFI", "state=ON", "Ready to execute")
        lower.contains("bluetooth") ->
            ActionResult("TOGGLE_BLUETOOTH", "state=ON", "Ready to execute")
        lower.contains("volume") -> {
            val direction = if (lower.contains("up") || lower.contains("increase")) "UP" else "DOWN"
            ActionResult("ADJUST_VOLUME", "direction=$direction", "Ready to execute")
        }
        lower.contains("brightness") ->
            ActionResult("ADJUST_BRIGHTNESS", "level=80%", "Ready to execute")
        else ->
            ActionResult("UNKNOWN", "raw=\"$command\"", "Needs LLM interpretation")
    }
}
