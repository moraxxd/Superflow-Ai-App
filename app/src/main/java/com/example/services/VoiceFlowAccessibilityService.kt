package com.example.services

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class VoiceFlowAccessibilityService : AccessibilityService() {

    companion object {
        var instance: VoiceFlowAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null

        fun insertTextIntoFocusedNode(text: String): Boolean {
            val service = instance ?: return false
            val root = service.rootInActiveWindow ?: return false
            val focusedNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false

            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            focusedNode.recycle()
            root.recycle()
            return success
        }

        fun getFocusedText(): String? {
            val service = instance ?: return null
            val root = service.rootInActiveWindow ?: return null
            val focusedNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return null
            val text = focusedNode.text?.toString()
            focusedNode.recycle()
            root.recycle()
            return text
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Listening to focus changes in active applications
    }

    override fun onInterrupt() {
        // Service interrupted by OS
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }
}
