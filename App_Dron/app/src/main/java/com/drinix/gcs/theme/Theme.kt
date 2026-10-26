package com.drinix.gcs.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Paleta azul noche (estilo mockup Drinix GCS) ─────────────────
val Color1 = Color(0xFF1D9BF0) // acento / botón principal (azul)
val Color2 = Color(0xFF18273C) // secundario / chips
val Color3 = Color(0xFF182238) // tarjetas oscuro
val Color4 = Color(0xFF111C2E) // surface oscuro
val Color5 = Color(0xFF0A1220) // fondo oscuro

// Colores semánticos oscuro
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

// ── Paleta clara (versión diurna) ────────────────────────────────
val LightColor1 = Color(0xFF1D9BF0)
val LightColor2 = Color(0xFFE3F2FD)
val LightColor3 = Color(0xFFBBDEFB)
val LightColor4 = Color(0xFF90CAF9)
val LightColor5 = Color(0xFFFFFFFF)

val LightGreenOk = Color(0xFF2E7D32)
val LightAmberWarning = Color(0xFFF57C00)
val LightErrorRed = Color(0xFFC62828)
val LightTextPrimary = Color(0xFF212121)
val LightTextSecondary = Color(0xFF757575)
val LightIconCyan = Color(0xFF0288D1)
val LightIconLilac = Color(0xFF7B1FA2)
val LightSkyBlue = Color(0xFF0288D1)

private val LightScheme = lightColorScheme(
    primary = LightColor1,
    onPrimary = Color.White,
    primaryContainer = LightColor2,
    onPrimaryContainer = LightTextPrimary,
    secondary = LightSkyBlue,
    onSecondary = Color.White,
    tertiary = LightIconLilac,
    background = LightColor5,
    onBackground = LightTextPrimary,
    surface = LightColor4,
    onSurface = LightTextPrimary,
    surfaceVariant = LightColor3,
    onSurfaceVariant = LightTextSecondary,
    error = LightErrorRed,
    onError = Color.White,
    outline = Color(0xFFBDBDBD),
)

@Composable
fun DrinixTheme(isDark: Boolean = true, content: @Composable () -> Unit) {
    val colorScheme = if (isDark) DarkScheme else LightScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
