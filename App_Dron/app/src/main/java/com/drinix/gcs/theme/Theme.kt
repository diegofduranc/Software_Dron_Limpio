package com.drinix.gcs.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Paleta azul noche (estilo mockup Drinix GCS) ─────────────────
val Color1 = Color(0xFF1D9BF0) // acento / botón principal (azul)
val Color2 = Color(0xFF18273C) // secundario / chips
val Color3 = Color(0xFF182238) // tarjetas
val Color4 = Color(0xFF111C2E) // surface
val Color5 = Color(0xFF0A1220) // fondo

// Colores semánticos
val GreenOk = Color(0xFF4ADE80)
val AmberWarning = Color(0xFFFBBF24)
val ErrorRed = Color(0xFFF87171)
val TextPrimary = Color(0xFFE6ECF8)
val TextSecondary = Color(0xFF8CA0BC)
val IconCyan = Color(0xFF22D3EE)
val IconLilac = Color(0xFF8B7CF6)
val SkyBlue = Color(0xFF38BDF8)

// Alias usados por pantallas existentes
val NavyBackground = Color5
val NavySurface = Color4
val NavyCard = Color3
val BlueAccent = Color1

private val DarkScheme = darkColorScheme(
    primary = Color1,
    onPrimary = Color.White,
    primaryContainer = Color2,
    onPrimaryContainer = TextPrimary,
    secondary = SkyBlue,
    onSecondary = Color.White,
    tertiary = IconLilac,
    background = Color5,
    onBackground = TextPrimary,
    surface = Color4,
    onSurface = TextPrimary,
    surfaceVariant = Color3,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = Color.White,
    outline = Color(0xFF243146),
)

@Composable
fun DrinixTheme(content: @Composable () -> Unit) {
    // Tema oscuro siempre, estilo cabina de vuelo
    MaterialTheme(
        colorScheme = DarkScheme,
        content = content,
    )
}
