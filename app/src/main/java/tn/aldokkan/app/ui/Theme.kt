package tn.aldokkan.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Palette chaude d'Al Dokkan : terracotta, safran, crème, espresso, sauge. */
object Dk {
    val Terra = Color(0xFFB8441F)
    val Saffron = Color(0xFFF2A33A)
    val Cream = Color(0xFFFFF8EE)
    val Sand = Color(0xFFF6E7CF)
    val Line = Color(0xFFEADCC6)
    val Esp = Color(0xFF2B1A12)
    val Esp2 = Color(0xFF3D2619)
    val Muted = Color(0xFF6B5648)
    val Sage = Color(0xFF58A97D)
    val Pill = Color(0xFFF9D9A6)
    val GreenBg = Color(0xFFE4F2E8)
    val GreenTx = Color(0xFF1E5A3A)
    val White = Color(0xFFFFFFFF)
}

@Composable
fun DokkanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Dk.Terra,
            onPrimary = Dk.White,
            secondary = Dk.Saffron,
            background = Dk.White,
            surface = Dk.White,
            onBackground = Dk.Esp,
            onSurface = Dk.Esp,
            outline = Dk.Line,
        ),
        content = content,
    )
}
