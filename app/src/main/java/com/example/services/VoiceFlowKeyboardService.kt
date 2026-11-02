package com.example.services

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.ai.core.LocalLanguageIdentifier
import com.example.ai.core.LocalTextRewriter
import com.example.ai.core.LocalTranslationEngine
import com.example.ai.core.RewriteStyle

class VoiceFlowKeyboardService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    companion object {
        var instance: VoiceFlowKeyboardService? = null
            private set

        fun commitDirectText(text: String): Boolean {
            val ic = instance?.currentInputConnection ?: return false
            return ic.commitText(text, 1)
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        instance = this
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        return ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setViewTreeLifecycleOwner(this@VoiceFlowKeyboardService)
            setViewTreeSavedStateRegistryOwner(this@VoiceFlowKeyboardService)

            setContent {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        surface = Color(0xFF0F172A),
                        primary = Color(0xFF06B6D4),
                        secondary = Color(0xFF8B5CF6),
                        background = Color(0xFF0B0F19)
                    )
                ) {
                    KeyboardLayout(
                        onKeyClick = { key ->
                            currentInputConnection?.commitText(key, 1)
                        },
                        onBackspace = {
                            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
                        },
                        onEnter = {
                            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                        },
                        onSpace = {
                            currentInputConnection?.commitText(" ", 1)
                        },
                        onQuickRewrite = { style ->
                            // Read current input or selected text, rewrite locally, commit
                            val selectedText = currentInputConnection?.getSelectedText(0)?.toString()
                            val textToRewrite = if (!selectedText.isNullOrBlank()) {
                                selectedText
                            } else {
                                currentInputConnection?.getTextBeforeCursor(200, 0)?.toString() ?: ""
                            }
                            if (textToRewrite.isNotBlank()) {
                                val result = LocalTextRewriter.rewrite(textToRewrite, style)
                                currentInputConnection?.commitText(result.rewrittenText, 1)
                            }
                        },
                        onQuickTranslate = {
                            val text = currentInputConnection?.getTextBeforeCursor(200, 0)?.toString() ?: ""
                            if (text.isNotBlank()) {
                                val detected = LocalLanguageIdentifier.identify(text)
                                val result = LocalTranslationEngine.translateToEnglish(text, detected)
                                if (result is com.example.ai.core.LocalTranslationResult.Success) {
                                    currentInputConnection?.commitText(result.translatedText, 1)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        if (instance == this) instance = null
    }
}

@Composable
fun KeyboardLayout(
    onKeyClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onQuickRewrite: (RewriteStyle) -> Unit,
    onQuickTranslate: () -> Unit
) {
    val rows = listOf(
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("Z", "X", "C", "V", "B", "N", "M")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0F19))
            .padding(bottom = 8.dp)
    ) {
        // VoiceFlow AI Quick Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131D31))
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF06B6D4).copy(alpha = 0.2f),
                modifier = Modifier.clickable { onQuickTranslate() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Translate, contentDescription = "Translate to English", tint = Color(0xFF06B6D4), modifier = Modifier.size(16.dp))
                    Text("Translate", fontSize = 12.sp, color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold)
                }
            }

            // Quick Rewrite Style chips
            val quickStyles = listOf(
                RewriteStyle.FORMAL,
                RewriteStyle.CASUAL,
                RewriteStyle.PROFESSIONAL,
                RewriteStyle.GEN_Z,
                RewriteStyle.SHORT
            )

            quickStyles.forEach { style ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.clickable { onQuickRewrite(style) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(14.dp))
                        Text(style.displayName, fontSize = 12.sp, color = Color(0xFFE2E8F0))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // QWERTY rows
        rows.forEachIndexed { index, rowList ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                rowList.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                            .clickable { onKeyClick(key.lowercase()) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = key, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Bottom control row (Space, Backspace, Enter)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF334155))
                    .clickable { onKeyClick("? ") },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "?", color = Color.White, fontSize = 16.sp)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E293B))
                    .clickable { onSpace() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "VoiceFlow Offline", color = Color(0xFF64748B), fontSize = 12.sp)
            }

            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF334155))
                    .clickable { onBackspace() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = Color.White, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF06B6D4))
                    .clickable { onEnter() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardReturn, contentDescription = "Enter", tint = Color.Black, modifier = Modifier.size(20.dp))
            }
        }
    }
}
