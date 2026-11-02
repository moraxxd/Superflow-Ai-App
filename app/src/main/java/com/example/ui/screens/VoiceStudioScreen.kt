package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.VoiceFlowApplication
import com.example.ai.core.*
import com.example.data.HistoryEntity
import com.example.services.TextInsertionManager
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun VoiceStudioScreen(
    speechEngine: LocalSpeechEngine,
    onNavigateToFloating: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val historyDao = VoiceFlowApplication.instance.database.historyDao()
    val historyItems by historyDao.getAllHistory().collectAsState(initial = emptyList())

    val speechState by speechEngine.speechState.collectAsState()
    val audioLevel by speechEngine.liveAudioLevel.collectAsState()
    val liveText by speechEngine.livePartialText.collectAsState()

    var inputText by remember { mutableStateOf("नमस्ते, मैं प्रोजेक्ट की प्रगति के बारे में बात करना चाहता हूँ।") }
    var selectedTone by remember { mutableStateOf(RewriteStyle.PROFESSIONAL) }
    var detectedLanguage by remember { mutableStateOf(LocalLanguageIdentifier.identify(inputText)) }
    var translationResult by remember { mutableStateOf<LocalTranslationResult>(LocalTranslationEngine.translateToEnglish(inputText, detectedLanguage)) }
    var rewrittenResult by remember { mutableStateOf(LocalTextRewriter.rewrite(
        if (translationResult is LocalTranslationResult.Success) (translationResult as LocalTranslationResult.Success).translatedText else inputText,
        selectedTone
    )) }

    // Re-evaluate when input text changes
    LaunchedEffect(inputText) {
        if (inputText.isNotBlank()) {
            val detected = LocalLanguageIdentifier.identify(inputText)
            detectedLanguage = detected
            val trans = LocalTranslationEngine.translateToEnglish(inputText, detected)
            translationResult = trans
            val textForRewriter = if (trans is LocalTranslationResult.Success && trans.translatedText.isNotBlank()) {
                trans.translatedText
            } else {
                inputText
            }
            rewrittenResult = LocalTextRewriter.rewrite(textForRewriter, selectedTone)
        }
    }

    // React to Speech Engine results
    LaunchedEffect(speechState) {
        if (speechState is SpeechState.Completed) {
            val text = (speechState as SpeechState.Completed).text
            if (text.isNotBlank()) {
                inputText = text
            }
        }
    }

    // Re-evaluate when tone style changes
    fun updateTone(newStyle: RewriteStyle) {
        selectedTone = newStyle
        val base = if (translationResult is LocalTranslationResult.Success && (translationResult as LocalTranslationResult.Success).translatedText.isNotBlank()) {
            (translationResult as LocalTranslationResult.Success).translatedText
        } else {
            inputText
        }
        rewrittenResult = LocalTextRewriter.rewrite(base, newStyle)
    }

    val isRecording = speechState is SpeechState.Recording

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero / Local Engine Status Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.VpnLock,
                                contentDescription = "Offline Secure",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                "100% Offline On-Device AI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Whisper.cpp + FastText + NLLB-200 + llama.cpp",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            "0% API Calls",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Live Mic Recording Card & Waveform
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRecording) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (isRecording) "Listening Locally (Real-time VAD)..." else "Speech-to-Text Studio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (isRecording) "Speak clearly into your microphone" else "Tap microphone to record or test phrases below",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Animated Live Waveform Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        WaveformVisualizer(isRecording = isRecording, audioLevel = audioLevel)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Big Mic Action Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (isRecording) {
                                    speechEngine.stopListening(inputText)
                                } else {
                                    speechEngine.startListening()
                                }
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRecording) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                                )
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isRecording) "Stop listening" else "Start local recording",
                                tint = if (isRecording) Color.White else MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    if (isRecording && liveText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = liveText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Quick Preset Phrases for Instant Testing
        item {
            Column {
                Text(
                    "Quick Indian & Tribal Test Phrases",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val samplePhrases = listOf(
                        "नमस्ते, आप कैसे हैं?" to "Hindi",
                        "मी ठीक आहे, काळजी करू नका." to "Marathi",
                        "কেমন আছেন, আমার সাহায্য দরকার।" to "Bengali",
                        "வணக்கம், எனக்கு உதவி வேண்டும்." to "Tamil",
                        "మీరు ఎలా ఉన్నారు, నాకు సహాయం కావాలి." to "Telugu",
                        "ᱡᱚᱦᱟᱨ, ᱪᱮᱫ ᱞᱮᱠᱟ ᱢᱮᱱᱟᱜ ᱵᱤᱱᱟ?" to "Santali (Ol Chiki)",
                        "Khublei shibun, nga koit nga khiah." to "Khasi"
                    )

                    samplePhrases.forEach { (phrase, label) ->
                        OutlinedButton(
                            onClick = { inputText = phrase },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Column {
                                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(phrase, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        // Editable Input & Language Detection Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Input / Recognized Text", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "${detectedLanguage.languageName} (${(detectedLanguage.confidence * 100).toInt()}%)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Enter or speak phrase...") },
                        maxLines = 4
                    )
                }
            }
        }

        // NLLB-200 Local English Translation Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("NLLB-200 Local English Translation", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (val res = translationResult) {
                        is LocalTranslationResult.Success -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = res.translatedText.ifEmpty { "(No text to translate)" },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Engine: ${res.modelUsed} • On-Device Neural",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        is LocalTranslationResult.Unsupported -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "Not Currently Supported",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFFEF4444)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            res.reason,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // On-Device AI Text Rewriting (llama.cpp) Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Text Rewriting (llama.cpp)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text("${rewrittenResult.processingTimeMs}ms (Local)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 7 Style Selector Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RewriteStyle.values().forEach { style ->
                            val isSelected = style == selectedTone
                            FilterChip(
                                selected = isSelected,
                                onClick = { updateTone(style) },
                                label = {
                                    Text(
                                        style.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Rewritten Content Display
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = selectedTone.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = rewrittenResult.rewrittenText.ifEmpty { "(No text generated)" },
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons: Direct Insert, Copy, Save
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val target = rewrittenResult.rewrittenText
                                TextInsertionManager.insertText(context, target)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Insert Text", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("VoiceFlow", rewrittenResult.rewrittenText))
                                Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.height(46.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val transText = if (translationResult is LocalTranslationResult.Success) {
                                        (translationResult as LocalTranslationResult.Success).translatedText
                                    } else ""
                                    historyDao.insert(
                                        HistoryEntity(
                                            originalText = inputText,
                                            detectedLanguage = detectedLanguage.languageName,
                                            confidence = detectedLanguage.confidence,
                                            translatedText = transText,
                                            rewrittenText = rewrittenResult.rewrittenText,
                                            toneStyle = selectedTone.displayName
                                        )
                                    )
                                    Toast.makeText(context, "Saved to local offline history", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.height(46.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = "Save")
                        }
                    }
                }
            }
        }

        // Offline History Section
        if (historyItems.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Saved Offline History (${historyItems.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    TextButton(onClick = {
                        scope.launch { historyDao.clearAll() }
                    }) {
                        Text("Clear All", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            items(historyItems.take(5)) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.detectedLanguage, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(item.toneStyle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.originalText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (item.rewrittenText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(item.rewrittenText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WaveformVisualizer(isRecording: Boolean, audioLevel: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val amplitude = if (isRecording) {
            (audioLevel * (height * 0.42f)).coerceAtLeast(6f)
        } else {
            3f
        }

        val numBars = 36
        val barWidth = width / (numBars * 1.5f)
        val spacing = barWidth * 0.5f

        for (i in 0 until numBars) {
            val progress = i.toFloat() / numBars
            val wave = sin(progress * 4 * Math.PI + (if (isRecording) phase else 0f)).toFloat()
            val currentH = if (isRecording) {
                (amplitude * (0.4f + 0.6f * Math.abs(wave))).coerceIn(4f, height * 0.85f)
            } else {
                8f + 4f * sin(progress * Math.PI).toFloat()
            }

            val x = i * (barWidth + spacing) + spacing
            val top = centerY - currentH / 2f
            val bottom = centerY + currentH / 2f

            drawLine(
                color = if (isRecording) Color(0xFF06B6D4) else Color(0xFF475569),
                start = Offset(x, top),
                end = Offset(x, bottom),
                strokeWidth = barWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}
