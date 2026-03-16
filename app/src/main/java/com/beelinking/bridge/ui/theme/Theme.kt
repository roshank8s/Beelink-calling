package com.beelinking.bridge.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2E3FC),
    secondary = Color(0xFF5F6368),
    surface = Color(0xFFF8F9FA),
    background = Color.White,
    error = Color(0xFFEA4335),
)

val CallGreen = Color(0xFF34A853)
val CallRed = Color(0xFFEA4335)
val BluetoothBlue = Color(0xFF4285F4)

@Composable
fun BeelinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
