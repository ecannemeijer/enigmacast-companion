package com.enigmacast.companion.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Trilfeedback, centraal zodat de instelling "Haptiek" overal werkt. */
class Haptics(private val enabled: Boolean, private val feedback: HapticFeedback) {
    fun tick() {
        if (enabled) feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun confirm() {
        if (enabled) feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

@Composable
fun rememberHaptics(enabled: Boolean): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(enabled, feedback) { Haptics(enabled, feedback) }
}
