package com.worldtheater.archive.platform.system

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

internal data class DesktopToastMessage(
    val id: Long,
    val text: String
)

internal object DesktopToastManager {
    private val idGenerator = AtomicLong(0L)
    private val _message = MutableStateFlow<DesktopToastMessage?>(null)
    val message: StateFlow<DesktopToastMessage?> = _message.asStateFlow()

    fun show(message: String) {
        if (message.isBlank()) return
        _message.value = DesktopToastMessage(id = idGenerator.incrementAndGet(), text = message)
    }

    fun clear(id: Long) {
        if (_message.value?.id == id) {
            _message.value = null
        }
    }
}

@Composable
fun DesktopToastHost(
    modifier: Modifier = Modifier
) {
    val message by DesktopToastManager.message.collectAsState()

    LaunchedEffect(message?.id) {
        val id = message?.id ?: return@LaunchedEffect
        delay(2200)
        DesktopToastManager.clear(id)
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    text = message?.text.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}
