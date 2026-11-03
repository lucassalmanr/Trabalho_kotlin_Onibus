package br.com.mobilibus.tracker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MobilibusScheme = darkColorScheme(
    primary = Color(0xFF69E0A5),
    secondary = Color(0xFF9CCBFF),
    background = Color(0xFF101716),
    surface = Color(0xFF17201E),
    surfaceVariant = Color(0xFF22312D)
)

@Composable
fun TrackerTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = MobilibusScheme, content = content)
