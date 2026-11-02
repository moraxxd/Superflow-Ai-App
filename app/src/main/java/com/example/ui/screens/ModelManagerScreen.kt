package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.VoiceFlowApplication
import com.example.ai.core.AIModelInfo
import com.example.ai.core.ModelCategory
import com.example.ai.core.ModelStatus

@Composable
fun ModelManagerScreen() {
    val context = LocalContext.current
    val modelManager = VoiceFlowApplication.instance.modelManager
    val models by modelManager.models.collectAsState()
    val hardwareMetrics by modelManager.hardwareMetrics.collectAsState()
    val isLightweightMode by modelManager.isLightweightMode.collectAsState()

    var selectedCategoryFilter by remember { mutableStateOf<ModelCategory?>(null) }
    var selectedModelForDetails by remember { mutableStateOf<AIModelInfo?>(null) }

    val filteredModels = remember(models, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) models else models.filter { it.category == selectedCategoryFilter }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hardware Telemetry & Safety Warning Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hardwareMetrics.isLowRamDevice || hardwareMetrics.isStorageLow)
                        Color(0xFFF59E0B).copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Memory,
                                contentDescription = null,
                                tint = if (hardwareMetrics.isLowRamDevice) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Device Hardware Telemetry",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        IconButton(onClick = { modelManager.refreshHardwareMetrics() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // RAM Metric Box
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("System RAM", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "${hardwareMetrics.availableRamMb} MB Free",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (hardwareMetrics.availableRamMb < 1024) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                                )
                                Text("Total: ${hardwareMetrics.totalRamMb} MB", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Storage Metric Box
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Internal Storage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "${hardwareMetrics.availableStorageMb} MB Free",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (hardwareMetrics.isStorageLow) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                                )
                                Text("Total: ${hardwareMetrics.totalStorageMb} MB", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (hardwareMetrics.isLowRamDevice) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Low RAM detected (< 4 GB). Lightweight mode automatically suggested to prevent memory termination.",
                                fontSize = 11.sp,
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }
                }
            }
        }

        // Lightweight Mode Toggle Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lightweight Mode (Budget Devices)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "Prioritizes low-footprint Q4/Q5 models (< 100MB) to optimize thermal and battery performance.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isLightweightMode,
                        onCheckedChange = { modelManager.toggleLightweightMode(it) }
                    )
                }
            }
        }

        // Category Filter Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategoryFilter == null,
                    onClick = { selectedCategoryFilter = null },
                    label = { Text("All Models (${models.size})") }
                )
                ModelCategory.values().forEach { category ->
                    val count = models.count { it.category == category }
                    FilterChip(
                        selected = selectedCategoryFilter == category,
                        onClick = { selectedCategoryFilter = category },
                        label = { Text("${category.displayName.substringBefore(" ")} ($count)") }
                    )
                }
            }
        }

        // Models List
        items(filteredModels) { model ->
            ModelCard(
                model = model,
                isLightweightMode = isLightweightMode,
                onDownload = {
                    modelManager.startDownload(model.id) {
                        Toast.makeText(context, "${model.name} installed successfully!", Toast.LENGTH_SHORT).show()
                    }
                },
                onCancel = { modelManager.cancelDownload(model.id) },
                onDelete = { modelManager.deleteModel(model.id) },
                onLoadRam = {
                    val loaded = modelManager.loadModelIntoRam(model.id)
                    if (!loaded) {
                        Toast.makeText(context, "Insufficient RAM to keep model pinned in memory", Toast.LENGTH_SHORT).show()
                    }
                },
                onUnloadRam = { modelManager.unloadModelFromRam(model.id) },
                onDetailsClick = { selectedModelForDetails = model }
            )
        }
    }

    // Model Details Modal Dialog
    selectedModelForDetails?.let { model ->
        AlertDialog(
            onDismissRequest = { selectedModelForDetails = null },
            title = {
                Text(model.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Framework: ${model.framework}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("Quantization: ${model.quantization}", fontSize = 12.sp)
                    Text("File Size: ${model.sizeMb} MB", fontSize = 12.sp)
                    Text("RAM Footprint: ~${model.ramRequiredMb} MB", fontSize = 12.sp)
                    Text("License: ${model.license}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Description: ${model.description}", fontSize = 12.sp)
                    Text("Supported Languages: ${model.supportedLanguages.joinToString(", ")}", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Integrity Checksum (SHA-256):\n${model.sha256Expected}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedModelForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ModelCard(
    model: AIModelInfo,
    isLightweightMode: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onLoadRam: () -> Unit,
    onUnloadRam: () -> Unit,
    onDetailsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Category, Name, Lightweight Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        model.category.displayName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        model.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (model.isLightweightRecommended) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            "Lightweight",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Specs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Size: ${model.sizeMb} MB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Text("RAM: ~${model.ramRequiredMb} MB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Text(model.quantization, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                model.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                lineHeight = 16.sp
            )

            // Download Progress Bar (if downloading)
            if (model.status == ModelStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { model.downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Downloading offline package: ${(model.downloadProgress * 100).toInt()}%",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${(model.downloadedBytes / (1024 * 1024))} / ${model.sizeMb.toInt()} MB",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDetailsClick) {
                    Text("License & Specs", fontSize = 11.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (model.status) {
                        ModelStatus.NOT_DOWNLOADED -> {
                            Button(
                                onClick = onDownload,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download", fontSize = 12.sp)
                            }
                        }
                        ModelStatus.DOWNLOADING -> {
                            OutlinedButton(
                                onClick = onCancel,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel", fontSize = 12.sp)
                            }
                        }
                        ModelStatus.INSTALLED -> {
                            OutlinedButton(
                                onClick = onLoadRam,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Pin in RAM", fontSize = 11.sp)
                            }
                            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete model", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            }
                        }
                        ModelStatus.LOADED_IN_RAM -> {
                            Button(
                                onClick = onUnloadRam,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Active in RAM", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
