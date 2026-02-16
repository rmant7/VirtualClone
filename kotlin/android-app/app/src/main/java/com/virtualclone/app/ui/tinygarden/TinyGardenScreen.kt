package com.virtualclone.app.ui.tinygarden

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtualclone.app.core.design.theme.TinyGardenGradientEnd
import com.virtualclone.app.core.design.theme.TinyGardenGradientStart

data class GardenPlot(
    val emoji: String = "🟫",   // soil
    val plantName: String = "",
    val stage: Int = 0          // 0=empty, 1=seed, 2=sprout, 3=bloom
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TinyGardenScreen(
    onNavigateBack: () -> Unit = {}
) {
    var command by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("Welcome to Tiny Garden! 🌱\nTry: \"plant a rose\", \"water plot 1\", \"harvest plot 3\"") }
    val plots = remember {
        mutableStateListOf<GardenPlot>().apply {
            repeat(9) { add(GardenPlot()) }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Tiny Garden 🌻", fontWeight = FontWeight.SemiBold)
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Garden Grid ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2D1B0E).copy(alpha = 0.08f)
                )
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(280.dp)
                ) {
                    itemsIndexed(plots) { index, plot ->
                        GardenPlotCell(
                            plot = plot,
                            plotIndex = index + 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Feedback area ──
            AnimatedVisibility(visible = feedback.isNotEmpty(), enter = fadeIn()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Text(
                        text = feedback,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Command Input ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a command…") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TinyGardenGradientStart,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        cursorColor = TinyGardenGradientStart
                    ),
                    maxLines = 2,
                    singleLine = false
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilledIconButton(
                    onClick = {
                        if (command.isNotBlank()) {
                            processGardenCommand(command, plots) { msg ->
                                feedback = msg
                            }
                            command = ""
                        }
                    },
                    enabled = command.isNotBlank(),
                    modifier = Modifier.size(52.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = TinyGardenGradientStart
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun GardenPlotCell(
    plot: GardenPlot,
    plotIndex: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (plot.stage) {
                0 -> Color(0xFF8B7355).copy(alpha = 0.15f)
                1 -> Color(0xFF8B7355).copy(alpha = 0.2f)
                2 -> TinyGardenGradientStart.copy(alpha = 0.15f)
                3 -> TinyGardenGradientEnd.copy(alpha = 0.2f)
                else -> Color(0xFF8B7355).copy(alpha = 0.15f)
            }
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = plot.emoji,
                    fontSize = 28.sp
                )
                if (plot.plantName.isNotEmpty()) {
                    Text(
                        text = plot.plantName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "#$plotIndex",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * Simple command processor — in production this would be replaced by on-device LLM.
 */
private fun processGardenCommand(
    command: String,
    plots: MutableList<GardenPlot>,
    onFeedback: (String) -> Unit
) {
    val lower = command.lowercase().trim()

    // Simple parsing — will be replaced by LLM
    when {
        lower.contains("plant") -> {
            val emptyIndex = plots.indexOfFirst { it.stage == 0 }
            if (emptyIndex == -1) {
                onFeedback("All plots are full! Try harvesting first. 🌾")
                return
            }
            val flower = when {
                lower.contains("rose") -> "Rose" to "🌹"
                lower.contains("sunflower") -> "Sunflower" to "🌻"
                lower.contains("tulip") -> "Tulip" to "🌷"
                lower.contains("daisy") -> "Daisy" to "🌼"
                lower.contains("tree") -> "Tree" to "🌳"
                else -> "Flower" to "🌱"
            }
            plots[emptyIndex] = GardenPlot(
                emoji = "🌱",
                plantName = flower.first,
                stage = 1
            )
            onFeedback("Planted ${flower.first} in plot ${emptyIndex + 1}! 🌱")
        }
        lower.contains("water") -> {
            val plotNum = Regex("\\d+").find(lower)?.value?.toIntOrNull()
            if (plotNum != null && plotNum in 1..plots.size) {
                val idx = plotNum - 1
                val plot = plots[idx]
                when (plot.stage) {
                    0 -> onFeedback("Plot $plotNum is empty. Plant something first!")
                    1 -> {
                        plots[idx] = plot.copy(emoji = "🌿", stage = 2)
                        onFeedback("Watered ${plot.plantName} in plot $plotNum — it's sprouting! 🌿")
                    }
                    2 -> {
                        val bloomEmoji = when (plot.plantName) {
                            "Rose" -> "🌹"
                            "Sunflower" -> "🌻"
                            "Tulip" -> "🌷"
                            "Daisy" -> "🌼"
                            "Tree" -> "🌳"
                            else -> "🌸"
                        }
                        plots[idx] = plot.copy(emoji = bloomEmoji, stage = 3)
                        onFeedback("${plot.plantName} in plot $plotNum is in full bloom! $bloomEmoji")
                    }
                    3 -> onFeedback("${plot.plantName} is already blooming beautifully! ${ plot.emoji}")
                }
            } else {
                onFeedback("Which plot? Try: \"water plot 1\"")
            }
        }
        lower.contains("harvest") -> {
            val plotNum = Regex("\\d+").find(lower)?.value?.toIntOrNull()
            if (plotNum != null && plotNum in 1..plots.size) {
                val idx = plotNum - 1
                val plot = plots[idx]
                if (plot.stage >= 2) {
                    onFeedback("Harvested ${plot.plantName} from plot $plotNum! 🧺")
                    plots[idx] = GardenPlot()
                } else if (plot.stage == 1) {
                    onFeedback("${plot.plantName} isn't ready yet — water it first! 💧")
                } else {
                    onFeedback("Plot $plotNum is empty.")
                }
            } else {
                onFeedback("Which plot? Try: \"harvest plot 1\"")
            }
        }
        lower.contains("status") || lower.contains("look") -> {
            val planted = plots.count { it.stage > 0 }
            val blooming = plots.count { it.stage == 3 }
            onFeedback("Garden: $planted planted, $blooming blooming, ${9 - planted} empty plots 🏡")
        }
        else -> {
            // TODO: Use on-device LLM to interpret natural language
            onFeedback("I don't understand that yet. Try: \"plant a rose\", \"water plot 1\", \"harvest plot 3\", or \"status\" 🤔")
        }
    }
}
