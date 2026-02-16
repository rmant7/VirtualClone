package com.virtualclone.app.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.virtualclone.app.core.design.theme.AiChatGradientStart
import com.virtualclone.app.core.design.theme.AskImageGradientStart
import com.virtualclone.app.core.design.theme.AudioScribeGradientStart
import com.virtualclone.app.core.design.theme.MobileActionsGradientStart
import com.virtualclone.app.core.design.theme.PromptLabGradientStart
import com.virtualclone.app.core.design.theme.TinyGardenGradientStart

data class FeatureItem(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val iconTint: Color,
    val action: () -> Unit
)

@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit = {},
    onNavigateToAskImage: () -> Unit = {},
    onNavigateToAudioScribe: () -> Unit = {},
    onNavigateToPromptLab: () -> Unit = {},
    onNavigateToTinyGarden: () -> Unit = {},
    onNavigateToMobileActions: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    // Keep legacy callbacks for backward compatibility
    onNavigateToMediaPipe: () -> Unit = {},
    onNavigateToLlm: () -> Unit = {}
) {
    val features = listOf(
        FeatureItem(
            id = "ai_chat",
            name = "AI Chat",
            description = "Multi-turn conversations with on-device AI",
            icon = Icons.Default.Chat,
            iconTint = AiChatGradientStart,
            action = onNavigateToChat
        ),
        FeatureItem(
            id = "ask_image",
            name = "Ask Image",
            description = "Upload an image and ask questions about it",
            icon = Icons.Default.Image,
            iconTint = AskImageGradientStart,
            action = onNavigateToAskImage
        ),
        FeatureItem(
            id = "audio_scribe",
            name = "Audio Scribe",
            description = "Transcribe and translate audio clips",
            icon = Icons.Default.Mic,
            iconTint = AudioScribeGradientStart,
            action = onNavigateToAudioScribe
        ),
        FeatureItem(
            id = "prompt_lab",
            name = "Prompt Lab",
            description = "Experiment with prompts — summarize, rewrite, code",
            icon = Icons.Default.Code,
            iconTint = PromptLabGradientStart,
            action = onNavigateToPromptLab
        ),
        FeatureItem(
            id = "tiny_garden",
            name = "Tiny Garden",
            description = "Grow a garden with natural language commands",
            icon = Icons.Default.Eco,
            iconTint = TinyGardenGradientStart,
            action = onNavigateToTinyGarden
        ),
        FeatureItem(
            id = "mobile_actions",
            name = "Mobile Actions",
            description = "Control your device with on-device AI",
            icon = Icons.Default.PhoneAndroid,
            iconTint = MobileActionsGradientStart,
            action = onNavigateToMobileActions
        ),
        FeatureItem(
            id = "mediapipe",
            name = "MediaPipe Tasks",
            description = "Face detection, gesture recognition & more",
            icon = Icons.Default.Visibility,
            iconTint = Color(0xFF00897B),
            action = onNavigateToMediaPipe
        ),
        FeatureItem(
            id = "llm_tasks",
            name = "LLM Tasks",
            description = "Summarize documents and ask questions",
            icon = Icons.Default.Psychology,
            iconTint = Color(0xFF7E57C2),
            action = onNavigateToLlm
        )
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            // ── App Icon + Header ──
            item {
                HomeHeader(onNavigateToSettings = onNavigateToSettings)
            }

            // ── Feature List ──
            itemsIndexed(features) { index, feature ->
                FeatureListItem(feature = feature)
                if (index < features.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }

            // ── Footer ──
            item {
                HomeFooter()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Header with App Icon
// ─────────────────────────────────────────────────────────────
@Composable
private fun HomeHeader(onNavigateToSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // ── App Icon ──
                Image(
                    painter = painterResource(id = com.virtualclone.app.R.drawable.vc_logo),
                    contentDescription = "VirtualClone",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "VirtualClone",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "On-device AI",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Section label ──
        Text(
            text = "Features",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// Feature List Item (like Google AI Edge Gallery)
// ─────────────────────────────────────────────────────────────
@Composable
private fun FeatureListItem(feature: FeatureItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { feature.action() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Icon circle ──
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(feature.iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = feature.name,
                modifier = Modifier.size(22.dp),
                tint = feature.iconTint
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // ── Text ──
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = feature.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // ── Chevron ──
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Footer
// ─────────────────────────────────────────────────────────────
@Composable
private fun HomeFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Powered by on-device AI",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
        Text(
            text = "No data leaves your phone",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            textAlign = TextAlign.Center
        )
    }
}
