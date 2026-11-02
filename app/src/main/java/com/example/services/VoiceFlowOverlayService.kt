package com.example.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.ai.core.*
import kotlinx.coroutines.launch

class VoiceFlowOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var speechEngine: LocalSpeechEngine? = null

    companion object {
        const val CHANNEL_ID = "voiceflow_overlay_channel"
        const val NOTIFICATION_ID = 1001

        var isOverlayRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, VoiceFlowOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VoiceFlowOverlayService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        speechEngine = LocalSpeechEngine(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        setupOverlayView()
        isOverlayRunning = true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VoiceFlow Floating Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs the 100% offline on-device speech assistant overlay"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VoiceFlow AI Active")
            .setContentText("Local offline assistant ready for speech & text insertion")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun setupOverlayView() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 200
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setViewTreeLifecycleOwner(this@VoiceFlowOverlayService)
            setViewTreeSavedStateRegistryOwner(this@VoiceFlowOverlayService)

            setContent {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        surface = Color(0xFF0F172A),
                        primary = Color(0xFF06B6D4),
                        secondary = Color(0xFF8B5CF6),
                        background = Color(0xFF0B0F19)
                    )
                ) {
                    OverlayBubbleCard(
                        speechEngine = speechEngine!!,
                        onDragDelta = { dx, dy ->
                            layoutParams.x = (layoutParams.x + dx.toInt()).coerceAtLeast(0)
                            layoutParams.y = (layoutParams.y + dy.toInt()).coerceAtLeast(0)
                            windowManager?.updateViewLayout(this, layoutParams)
                        },
                        onCloseOverlay = {
                            stopSelf()
                        },
                        onInsert = { textToInsert ->
                            TextInsertionManager.insertText(this@VoiceFlowOverlayService, textToInsert)
                        }
                    )
                }
            }
        }

        overlayView = composeView
        windowManager?.addView(composeView, layoutParams)
    }

    override fun onDestroy() {
        super.onDestroy()
        isOverlayRunning = false
        speechEngine?.cancelListening()
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}

@Composable
fun OverlayBubbleCard(
    speechEngine: LocalSpeechEngine,
    onDragDelta: (Float, Float) -> Unit,
    onCloseOverlay: () -> Unit,
    onInsert: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val speechState by speechEngine.speechState.collectAsState()
    val audioLevel by speechEngine.liveAudioLevel.collectAsState()
    val liveText by speechEngine.livePartialText.collectAsState()

    var recognizedText by remember { mutableStateOf("नमस्ते, मैं ठीक हूँ।") }
    var selectedTone by remember { mutableStateOf(RewriteStyle.PROFESSIONAL) }
    var rewrittenText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var detectedLang by remember { mutableStateOf("Hindi (Devanagari)") }

    // Update state when speech completes
    LaunchedEffect(speechState) {
        if (speechState is SpeechState.Completed) {
            val text = (speechState as SpeechState.Completed).text
            if (text.isNotBlank()) {
                recognizedText = text
                val detected = LocalLanguageIdentifier.identify(text)
                detectedLang = "${detected.languageName} (${detected.nativeScript})"
                val transRes = LocalTranslationEngine.translateToEnglish(text, detected)
                translatedText = when (transRes) {
                    is LocalTranslationResult.Success -> transRes.translatedText
                    is LocalTranslationResult.Unsupported -> transRes.reason
                }
                val rewriterRes = LocalTextRewriter.rewrite(
                    if (translatedText.isNotBlank() && transRes is LocalTranslationResult.Success) translatedText else text,
                    selectedTone
                )
                rewrittenText = rewriterRes.rewrittenText
            }
        }
    }

    // React to tone change
    LaunchedEffect(selectedTone, translatedText, recognizedText) {
        val base = if (translatedText.isNotBlank()) translatedText else recognizedText
        if (base.isNotBlank()) {
            val res = LocalTextRewriter.rewrite(base, selectedTone)
            rewrittenText = res.rewrittenText
        }
    }

    Column(
        modifier = Modifier
            .padding(8.dp)
            .shadow(16.dp, RoundedCornerShape(20.dp))
    ) {
        if (!isExpanded) {
            // Floating Mini Bubble
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6))
                        )
                    )
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDragDelta(dragAmount.x, dragAmount.y)
                        }
                    }
                    .clickable { isExpanded = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Open VoiceFlow Floating Assistant",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            // Expanded Floating Assistant Card
            Card(
                modifier = Modifier
                    .width(320.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F172A)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Bar with Drag Handle & Close
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onDragDelta(dragAmount.x, dragAmount.y)
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DragHandle, contentDescription = "Drag overlay", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("VoiceFlow AI", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF00C853).copy(alpha = 0.2f)
                            ) {
                                Text("Offline", color = Color(0xFF00C853), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        Row {
                            IconButton(onClick = { isExpanded = false }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Remove, contentDescription = "Minimize overlay", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = onCloseOverlay, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close overlay", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Local Mic Recording Control
                    val isRecording = speechState is SpeechState.Recording
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isRecording) Color(0xFF1E293B) else Color(0xFF131D31))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (isRecording) "Recording locally..." else "Tap Mic to speak",
                                color = if (isRecording) Color(0xFF06B6D4) else Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (isRecording) {
                                LinearProgressIndicator(
                                    progress = { audioLevel },
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .height(4.dp)
                                        .padding(top = 4.dp),
                                    color = Color(0xFF06B6D4),
                                    trackColor = Color(0xFF334155),
                                )
                            } else {
                                Text("Local Whisper.cpp STT", color = Color(0xFF64748B), fontSize = 10.sp)
                            }
                        }

                        IconButton(
                            onClick = {
                                if (isRecording) {
                                    speechEngine.stopListening()
                                } else {
                                    speechEngine.startListening()
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isRecording) Color(0xFFEF4444) else Color(0xFF06B6D4))
                        ) {
                            Icon(
                                if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isRecording) "Stop listening" else "Start listening",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live / Recognized Text Box
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Input / Recognized", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text(detectedLang, color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isRecording && liveText.isNotBlank()) liveText else recognizedText,
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Translated Preview (if non-English)
                    if (translatedText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF132A3E),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("NLLB Local English Translation", color = Color(0xFF38BDF8), fontSize = 10.sp)
                                Text(translatedText, color = Color(0xFFE0F2FE), fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tone Selector Row
                    Text("AI Style (llama.cpp on-device)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        RewriteStyle.values().forEach { style ->
                            val isSelected = style == selectedTone
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF8B5CF6) else Color(0xFF1E293B),
                                modifier = Modifier.clickable { selectedTone = style }
                            ) {
                                Text(
                                    text = style.displayName,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Rewritten Result Preview
                    if (rewrittenText.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1F1D36),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Ready to Insert (${selectedTone.displayName})", color = Color(0xFFA78BFA), fontSize = 10.sp)
                                Text(rewrittenText, color = Color.White, fontSize = 12.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Direct 1-Tap Insert Button
                    Button(
                        onClick = {
                            val textToInsert = if (rewrittenText.isNotBlank()) rewrittenText else if (translatedText.isNotBlank()) translatedText else recognizedText
                            onInsert(textToInsert)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Insert Into Active App", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
