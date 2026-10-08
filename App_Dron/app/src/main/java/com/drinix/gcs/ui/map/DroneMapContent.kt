package com.drinix.gcs.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.drinix.gcs.R
import com.drinix.gcs.data.model.Telemetry
import com.drinix.gcs.ui.DroneViewModel
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberMarkerState

/** Convierte un vector drawable en BitmapDescriptor para marcadores de mapa. */
private fun vectorToBitmapDescriptor(context: android.content.Context, resId: Int, sizePx: Int): BitmapDescriptor? {
    val drawable = ContextCompat.getDrawable(context, resId) ?: return null
    drawable.setBounds(0, 0, sizePx, sizePx)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    drawable.draw(AndroidCanvas(bitmap))
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/**
 * Contenido compartido de los mapas: marcador del dron (rotado por heading),
 * polyline del recorrido y marcador Home.
 */
@Composable
fun DroneMapContent(viewModel: DroneViewModel, telemetry: Telemetry?) {
    val path by viewModel.pathPoints.collectAsState()
    val home by viewModel.homePoint.collectAsState()

    // Icono de dron (cuadricóptero) cacheado
    val context = LocalContext.current
    val droneIcon = remember {
        vectorToBitmapDescriptor(context, R.drawable.ic_drone_marker, 96)
    }

    // Marcador del dron rotado según heading
    telemetry?.let { t ->
        if (t.lat != null && t.lon != null) {
            Marker(
                state = rememberMarkerState(position = LatLng(t.lat, t.lon)),
                title = "Dron",
                snippet = "Alt: ${t.alt?.toInt() ?: "--"} m · ${t.mode ?: ""}",
                rotation = (t.heading ?: 0.0).toFloat(),
                flat = true,
                anchor = Offset(0.5f, 0.5f),
                icon = droneIcon ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
            )
        }
    }

    // Polyline del recorrido
    if (path.size >= 2) {
        Polyline(
            points = path.map { LatLng(it.first, it.second) },
            color = Color(0xFF2F7CF6),
            width = 6f,
        )
    }

    // Marcador Home (punto de armado)
    home?.let {
        Marker(
            state = rememberMarkerState(position = LatLng(it.first, it.second)),
            title = "Home",
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
        )
    }
}
