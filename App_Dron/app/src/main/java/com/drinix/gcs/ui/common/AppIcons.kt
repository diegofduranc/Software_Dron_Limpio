package com.drinix.gcs.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Iconos vectoriales propios en estilo "flat" (inspirados en flaticon.es).
 * Si luego quieres los iconos exactos de flaticon.es, descarga el SVG/PNG
 * deseado y colócalo en res/drawable (o convierte el SVG con Android Studio:
 * File > New > Vector Asset).
 */
object AppIcons {

    /** Dron quadcóptero estilizado. */
    val Drone: ImageVector by lazy {
        ImageVector.Builder(
            name = "Drone", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(fill = SolidColor(Color.White)) {
            // brazos en X
            moveTo(5.2f, 5.9f); lineTo(6.6f, 4.5f); lineTo(11f, 8.9f)
            lineTo(8.9f, 11f); close()
            moveTo(18.8f, 5.9f); lineTo(17.4f, 4.5f); lineTo(13f, 8.9f)
            lineTo(15.1f, 11f); close()
            moveTo(5.2f, 18.1f); lineTo(6.6f, 19.5f); lineTo(11f, 15.1f)
            lineTo(8.9f, 13f); close()
            moveTo(18.8f, 18.1f); lineTo(17.4f, 19.5f); lineTo(13f, 15.1f)
            lineTo(15.1f, 13f); close()
            // hélices (esquinas)
            moveTo(2f, 4f); horizontalLineTo(7f); verticalLineTo(6f)
            horizontalLineTo(2f); close()
            moveTo(17f, 4f); horizontalLineTo(22f); verticalLineTo(6f)
            horizontalLineTo(17f); close()
            moveTo(2f, 18f); horizontalLineTo(7f); verticalLineTo(20f)
            horizontalLineTo(2f); close()
            moveTo(17f, 18f); horizontalLineTo(22f); verticalLineTo(20f)
            horizontalLineTo(17f); close()
            // cuerpo central
            moveTo(9f, 10.2f); curveTo(9f, 8.8f, 10.6f, 8f, 12f, 8f)
            curveTo(13.4f, 8f, 15f, 8.8f, 15f, 10.2f)
            verticalLineTo(13.8f)
            curveTo(15f, 15.2f, 13.4f, 16f, 12f, 16f)
            curveTo(10.6f, 16f, 9f, 15.2f, 9f, 13.8f)
            close()
        }.build()
    }

    /** Batería con nivel. */
    val Battery: ImageVector by lazy {
        ImageVector.Builder(
            name = "Battery", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(fill = SolidColor(Color.White)) {
            // cuerpo
            moveTo(3f, 7f)
            curveTo(3f, 5.9f, 3.9f, 5f, 5f, 5f)
            horizontalLineTo(18f)
            curveTo(19.1f, 5f, 20f, 5.9f, 20f, 7f)
            verticalLineTo(17f)
            curveTo(20f, 18.1f, 19.1f, 19f, 18f, 19f)
            horizontalLineTo(5f)
            curveTo(3.9f, 19f, 3f, 18.1f, 3f, 17f)
            close()
            // terminal
            moveTo(21f, 10f); horizontalLineTo(23f); verticalLineTo(14f)
            horizontalLineTo(21f); close()
            // celdas internas (simulan nivel)
            moveTo(6f, 8f); horizontalLineTo(9f); verticalLineTo(16f)
            horizontalLineTo(6f); close()
            moveTo(10f, 8f); horizontalLineTo(13f); verticalLineTo(16f)
            horizontalLineTo(10f); close()
            moveTo(14f, 8f); horizontalLineTo(17f); verticalLineTo(16f)
            horizontalLineTo(14f); close()
        }.build()
    }

    /** Satélite / GPS. */
    val Satellite: ImageVector by lazy {
        ImageVector.Builder(
            name = "Satellite", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(fill = SolidColor(Color.White)) {
            // paneles solares
            moveTo(2f, 8f); lineTo(8f, 2f); lineTo(10f, 4f); lineTo(4f, 10f); close()
            moveTo(14f, 20f); lineTo(20f, 14f); lineTo(22f, 16f); lineTo(16f, 22f); close()
            // cuerpo
            moveTo(9.9f, 8.5f); lineTo(15.5f, 14.1f); lineTo(13.4f, 16.2f)
            lineTo(7.8f, 10.6f); close()
            // antena
            moveTo(15.8f, 5.1f); curveTo(16.9f, 4f, 18.7f, 4f, 19.8f, 5.1f)
            curveTo(20.9f, 6.2f, 20.9f, 8f, 19.8f, 9.1f)
            curveTo(18.7f, 10.2f, 16.9f, 10.2f, 15.8f, 9.1f)
            curveTo(14.7f, 8f, 14.7f, 6.2f, 15.8f, 5.1f)
            close()
        }.build()
    }

    /** Señal (barras de intensidad). */
    val Signal: ImageVector by lazy {
        ImageVector.Builder(
            name = "Signal", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(fill = SolidColor(Color.White)) {
            moveTo(3f, 16f); horizontalLineTo(6f); verticalLineTo(20f)
            horizontalLineTo(3f); close()
            moveTo(8f, 12f); horizontalLineTo(11f); verticalLineTo(20f)
            horizontalLineTo(8f); close()
            moveTo(13f, 8f); horizontalLineTo(16f); verticalLineTo(20f)
            horizontalLineTo(13f); close()
            moveTo(18f, 4f); horizontalLineTo(21f); verticalLineTo(20f)
            horizontalLineTo(18f); close()
        }.build()
    }

    /** Altitud (montaña + flecha). */
    val Altitude: ImageVector by lazy {
        ImageVector.Builder(
            name = "Altitude", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(fill = SolidColor(Color.White)) {
            // montañas
            moveTo(3f, 18f); lineTo(9f, 7f); lineTo(13f, 13f)
            lineTo(15.5f, 9f); lineTo(21f, 18f); close()
            // sol
            moveTo(16f, 3f); curveTo(17.7f, 3f, 19f, 4.3f, 19f, 6f)
            curveTo(19f, 7.7f, 17.7f, 9f, 16f, 9f)
            curveTo(14.3f, 9f, 13f, 7.7f, 13f, 6f)
            curveTo(13f, 4.3f, 14.3f, 3f, 16f, 3f)
            close()
        }.build()
    }

    /** Velocidad (velocímetro). */
    val Speed: ImageVector by lazy {
        ImageVector.Builder(
            name = "Speed", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(fill = SolidColor(Color.White)) {
            moveTo(12f, 4f); curveTo(7.6f, 4f, 4f, 7.6f, 4f, 12f)
            curveTo(4f, 14.7f, 5.3f, 17.1f, 7.4f, 18.5f)
            lineTo(8.8f, 16.4f)
            curveTo(7.4f, 15.4f, 6.5f, 13.8f, 6.5f, 12f)
            curveTo(6.5f, 9f, 9f, 6.5f, 12f, 6.5f)
            curveTo(15f, 6.5f, 17.5f, 9f, 17.5f, 12f)
            curveTo(17.5f, 13.8f, 16.6f, 15.4f, 15.2f, 16.4f)
            lineTo(16.6f, 18.5f)
            curveTo(18.7f, 17.1f, 20f, 14.7f, 20f, 12f)
            curveTo(20f, 7.6f, 16.4f, 4f, 12f, 4f)
            close()
            // aguja
            moveTo(12f, 11f); curveTo(12.8f, 11f, 13.5f, 11.7f, 13.5f, 12.5f)
            curveTo(13.5f, 13.3f, 12.8f, 14f, 12f, 14f)
            curveTo(11.2f, 14f, 10.5f, 13.3f, 10.5f, 12.5f)
            curveTo(10.5f, 11.7f, 11.2f, 11f, 12f, 11f)
            close()
            moveTo(11.3f, 13.6f); lineTo(12.7f, 13.6f); lineTo(14.5f, 7.5f)
            close()
        }.build()
    }

    /** Distancia (ruta con marcadores). */
    val Distance: ImageVector by lazy {
        ImageVector.Builder(
            name = "Distance", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(fill = SolidColor(Color.White)) {
            // pin origen
            moveTo(6f, 3f); curveTo(4.3f, 3f, 3f, 4.3f, 3f, 6f)
            curveTo(3f, 7.7f, 4.3f, 9f, 6f, 9f)
            curveTo(7.7f, 9f, 9f, 7.7f, 9f, 6f)
            curveTo(9f, 4.3f, 7.7f, 3f, 6f, 3f)
            close()
            // pin destino
            moveTo(18f, 15f); curveTo(16.3f, 15f, 15f, 16.3f, 15f, 18f)
            curveTo(15f, 19.7f, 16.3f, 21f, 18f, 21f)
            curveTo(19.7f, 21f, 21f, 19.7f, 21f, 18f)
            curveTo(21f, 16.3f, 19.7f, 15f, 18f, 15f)
            close()
            // ruta
            moveTo(6f, 10f); verticalLineTo(13f)
            curveTo(6f, 15f, 7.5f, 15.5f, 9f, 15.5f)
            horizontalLineTo(15f)
            verticalLineTo(17.5f); horizontalLineTo(9f)
            curveTo(6.5f, 17.5f, 4f, 16.5f, 4f, 13f)
            verticalLineTo(10f)
            close()
            moveTo(18f, 14f); verticalLineTo(11f)
            curveTo(18f, 9f, 16.5f, 8.5f, 15f, 8.5f)
            horizontalLineTo(9.5f); verticalLineTo(6.5f)
            horizontalLineTo(15f)
            curveTo(17.5f, 6.5f, 20f, 7.5f, 20f, 11f)
            verticalLineTo(14f)
            close()
        }.build()
    }
}
