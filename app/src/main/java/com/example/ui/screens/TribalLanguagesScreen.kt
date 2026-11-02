package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.core.LocalLanguageIdentifier
import com.example.ai.core.LocalTranslationEngine
import com.example.ai.core.LocalTranslationResult
import com.example.ai.core.TribalLanguage
import com.example.ai.core.TribalLanguageRegistry
import com.example.ai.core.TribalSupportStatus

@Composable
fun TribalLanguagesScreen() {
    val languages = TribalLanguageRegistry.languages

    var selectedLanguageForTest by remember { mutableStateOf<TribalLanguage?>(languages.first()) }
    var testPhrase by remember { mutableStateOf("ᱡᱚᱦᱟᱨ, ᱪᱮᱫ ᱞᱮᱠᱟ ᱢᱮᱱᱟᱜ ᱵᱤᱱᱟ?") }
    var testResult by remember { mutableStateOf<LocalTranslationResult?>(null) }
    var expandedLanguageId by remember { mutableStateOf<String?>(null) }

    fun runTest(lang: TribalLanguage, phrase: String) {
        val detected = LocalLanguageIdentifier.identify(phrase)
        // If testing explicitly selected language, override detected language code
        val enrichedDetected = detected.copy(languageCode = lang.isoCode, languageName = lang.name)
        val result = LocalTranslationEngine.translateToEnglish(phrase, enrichedDetected)
        testResult = result
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Architecture & Honesty Commitment Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                "Tribal Languages Architecture",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Strict Zero-Hallucination & Honest Status Policy",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        "VoiceFlow integrates local NLLB-200 and IndicTrans models. To safeguard cultural integrity, languages lacking verified local checkpoints are honestly labeled 'Not currently supported' rather than generating misleading or incorrect translations.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Interactive Testing Playground
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Tribal Translation Verification Playground",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Language Selector Chips
                    ScrollableTabRow(
                        selectedTabIndex = languages.indexOfFirst { it.id == selectedLanguageForTest?.id }.coerceAtLeast(0),
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        languages.forEach { lang ->
                            val isSelected = selectedLanguageForTest?.id == lang.id
                            Tab(
                                selected = isSelected,
                                onClick = {
                                    selectedLanguageForTest = lang
                                    testResult = null
                                    testPhrase = when (lang.id) {
                                        "santali" -> "ᱡᱚᱦᱟᱨ, ᱪᱮᱫ ᱞᱮᱠᱟ ᱢᱮᱱᱟᱜ ᱵᱤᱱᱟ?"
                                        "khasi" -> "Khublei shibun, nga donkam jingiarap."
                                        "garo" -> "Mittam nama dong·a."
                                        "bhili" -> "भीली बोली मा बातचीत।"
                                        "gondi" -> "गोंडी भाषेचा संवाद।"
                                        "mundari" -> "ᱢᱩᱱᱰᱟᱨᱤ ᱡᱟᱜᱟᱨ।"
                                        "ho" -> "𑢹𑣉𑣉 𑣎𑣋𑣜 ᱠᱟᱛᱷᱟ।"
                                        "kokborok" -> "Kokborok rwchabmung."
                                        "kui" -> "କୁଇ ଭାଷା କଥାବାର୍ତ୍ତା।"
                                        "kurukh" -> "कुड़ुख़ पाही।"
                                        else -> "Sample phrase"
                                    }
                                },
                                text = {
                                    Text(
                                        lang.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    selectedLanguageForTest?.let { lang ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${lang.nativeName} • ${lang.primaryScript}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            StatusBadge(lang.supportStatus)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = testPhrase,
                            onValueChange = { testPhrase = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            placeholder = { Text("Enter phrase to test local pipeline...") }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { runTest(lang, testPhrase) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Local Translation Pipeline")
                        }

                        testResult?.let { res ->
                            Spacer(modifier = Modifier.height(10.dp))
                            when (res) {
                                is LocalTranslationResult.Success -> {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("English Output (Local Verified):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                            Text(res.translatedText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("Model: ${res.modelUsed}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                is LocalTranslationResult.Unsupported -> {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Honest Safety Check Triggered", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(res.reason, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Detailed Language Registry
        item {
            Text(
                "Tribal Languages Registry & Model Feasibility (${languages.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // List of all 10 Tribal Languages
        items(languages) { lang ->
            TribalLanguageCard(
                language = lang,
                isExpanded = expandedLanguageId == lang.id,
                onToggleExpand = {
                    expandedLanguageId = if (expandedLanguageId == lang.id) null else lang.id
                }
            )
        }
    }
}

@Composable
fun StatusBadge(status: TribalSupportStatus) {
    val bgColor = when (status) {
        TribalSupportStatus.SUPPORTED -> Color(0xFF10B981).copy(alpha = 0.15f)
        TribalSupportStatus.EXPERIMENTAL -> Color(0xFFF59E0B).copy(alpha = 0.15f)
        TribalSupportStatus.UNSUPPORTED -> Color(0xFFEF4444).copy(alpha = 0.15f)
    }
    val textColor = when (status) {
        TribalSupportStatus.SUPPORTED -> Color(0xFF10B981)
        TribalSupportStatus.EXPERIMENTAL -> Color(0xFFF59E0B)
        TribalSupportStatus.UNSUPPORTED -> Color(0xFFEF4444)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = status.label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TribalLanguageCard(
    language: TribalLanguage,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            language.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "(${language.nativeName})",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Family: ${language.languageFamily} • ISO: ${language.isoCode}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(language.supportStatus)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Script: ${language.primaryScript}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Text("Speakers: ${language.estimatedSpeakers}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Regions: ${language.primaryRegions.joinToString(", ")}", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Model Requirement: ${language.modelRequirement}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Dataset Notes: ${language.datasetNotes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            language.statusExplanation,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
