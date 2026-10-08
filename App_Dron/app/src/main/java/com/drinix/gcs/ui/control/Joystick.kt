package com.drinix.gcs.ui.control

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Joystick virtual de 2 ejes.
 * onMove(x, y) → valores normalizados en [-1f, 1f] (x: derecha+, y: arriba+).
 * onRelease() se llama al soltar (la UI debe recentrar a 1500).
 */
@Composable
fun VirtualJoystick(
    label: String,
    onMove: (x: Float, y: Float) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    /** Si true, el eje vertical no vuelve al centro al soltar (estilo throttle). */
    throttleMode: Boolean = false,
) {
    var sizePx by remember { mutableStateOf(0) }
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val boxSize = 170.dp
    val knobSize = 64.dp

    fun clamp(offset: Offset, radius: Float): Offset {
        val dist = offset.getDistance()
        return if (dist <= radius) offset
        else {
            val angle = Math.atan2(offset.y.toDouble(), offset.x.toDouble())
            Offset(
                (radius * cos(angle)).toFloat(),
                (radius * sin(angle)).toFloat(),
            )
        }
    }

    Box(
        modifier = modifier
            .size(boxSize)
            .onSizeChanged { sizePx = min(it.width, it.height) }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, if (dragging) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape)
            .pointerInput(throttleMode) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = {
                        dragging = false
                        val radius = sizePx / 2f - with(density) { (knobSize / 2).toPx() }
                        knobOffset = if (throttleMode) Offset(0f, knobOffset.y) else Offset.Zero
                        onMove(
                            knobOffset.x / radius,
                            (-knobOffset.y / radius).coerceIn(-1f, 1f),
                        )
                        onRelease()
                    },
                    onDragCancel = {
                        dragging = false
                        knobOffset = if (throttleMode) Offset(0f, knobOffset.y) else Offset.Zero
                        onRelease()
                    },
                ) { change, dragAmount ->
                    change.consume()
                    val radius = sizePx / 2f - with(density) { (knobSize / 2).toPx() }
                    if (radius > 0) {
                        knobOffset = clamp(knobOffset + dragAmount, radius)
                        onMove(
                            (knobOffset.x / radius).coerceIn(-1f, 1f),
                            (-knobOffset.y / radius).coerceIn(-1f, 1f),
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            style = MaterialTheme.typography.labelMedium,
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt()) }
                .size(knobSize)
                .clip(CircleShape)
                .background(
                    if (dragging) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primaryContainer
                ),
        )
    }
}

/** Convierte un valor normalizado [-1,1] a PWM (1000..2000, centro 1500). */
fun normalizedToPwm(value: Float): Int =
    (1500 + value.coerceIn(-1f, 1f) * 500).roundToInt().coerceIn(1000, 2000)

/** Convierte throttle [0,1] (abajo..arriba de pantalla) a PWM 1000..2000. */
fun throttleToPwm(value: Float): Int =
    (1000 + (value.coerceIn(0f, 1f)) * 1000).roundToInt().coerceIn(1000, 2000)
