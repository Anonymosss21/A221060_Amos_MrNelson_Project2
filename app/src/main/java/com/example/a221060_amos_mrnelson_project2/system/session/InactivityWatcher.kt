package com.example.a221060_amos_mrnelson_project2.system.session

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun InactivitySessionWatcher(
    onTimeoutTriggered: () -> Unit,
    content: @Composable () -> Unit
) {
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(5000) // Poll every 5 seconds
            if (SessionManager.isSessionExpired()) {
                onTimeoutTriggered()
                break
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        SessionManager.resetTimer()
                    }
                }
            }
    ) {
        content()
    }
}
