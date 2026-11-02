package com.example.services

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

enum class InsertionMethod {
    ACCESSIBILITY_SERVICE,
    INPUT_METHOD_EDITOR,
    CLIPBOARD_FALLBACK
}

data class InsertionResult(
    val success: Boolean,
    val methodUsed: InsertionMethod,
    val message: String
)

object TextInsertionManager {

    fun insertText(context: Context, text: String): InsertionResult {
        if (text.isBlank()) {
            return InsertionResult(false, InsertionMethod.CLIPBOARD_FALLBACK, "No text to insert.")
        }

        // Method 1: Try Accessibility Service (Direct UI node manipulation in external app)
        if (VoiceFlowAccessibilityService.isRunning()) {
            val inserted = VoiceFlowAccessibilityService.insertTextIntoFocusedNode(text)
            if (inserted) {
                Toast.makeText(context, "Inserted via Accessibility Service", Toast.LENGTH_SHORT).show()
                return InsertionResult(true, InsertionMethod.ACCESSIBILITY_SERVICE, "Text inserted into active input field.")
            }
        }

        // Method 2: Try Input Method Editor (IME commitText)
        if (VoiceFlowKeyboardService.instance != null) {
            val committed = VoiceFlowKeyboardService.commitDirectText(text)
            if (committed) {
                Toast.makeText(context, "Inserted via VoiceFlow Keyboard", Toast.LENGTH_SHORT).show()
                return InsertionResult(true, InsertionMethod.INPUT_METHOD_EDITOR, "Committed via active InputConnection.")
            }
        }

        // Method 3: Clipboard Fallback with user guidance
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("VoiceFlow AI", text)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, "Copied to clipboard! (Ready to paste in any app)", Toast.LENGTH_LONG).show()
        return InsertionResult(
            true,
            InsertionMethod.CLIPBOARD_FALLBACK,
            "Copied to clipboard. Enable Accessibility or VoiceFlow Keyboard for 1-tap direct insertion."
        )
    }
}
