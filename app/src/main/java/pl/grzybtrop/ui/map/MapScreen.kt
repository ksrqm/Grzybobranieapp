package pl.grzybtrop.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/dark"

@SuppressLint("MissingPermission") // zgodę sprawdzamy sami w hasPermission
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // NOWE: czy mamy zgodę na lokalizację oraz gotowa mapa i styl
    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    var readyMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var readyStyle by remember { mutableStateOf<Style?>(null) }

    val mapView = remember {
        MapView(context).apply {
            getMapAsync { map ->
                // NOWE: zapamiętujemy mapę dopiero, gdy styl się załaduje
                map.setStyle(STYLE_URL) { style ->
                    readyMap = map
                    readyStyle = style
                }
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(52.0, 19.0)) // mniej więcej środek Polski
                    .zoom(5.5)
                    .build()
            }
        }
    }

    // NOWE: gdy mamy mapę, styl i zgodę, włączamy kropkę z Twoją pozycją
    LaunchedEffect(readyMap, readyStyle, hasPermission) {
        val map = readyMap ?: return@LaunchedEffect
        val style = readyStyle ?: return@LaunchedEffect
        if (!hasPermission) return@LaunchedEffect

        val location = map.locationComponent
        location.activateLocationComponent(
            LocationComponentActivationOptions.builder(context, style)
                .useDefaultLocationEngine(true)
                .build()
        )
        location.isLocationComponentEnabled = true
        location.cameraMode = CameraMode.TRACKING   // mapa jedzie za Tobą
        location.renderMode = RenderMode.COMPASS    // strzałka pokazuje kierunek
        location.zoomWhileTracking(15.0)            // przybliżenie do poziomu lasu
    }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    // NOWE: po oknie z prośbą o zgodę sprawdzamy ją jeszcze raz
                    hasPermission = hasLocationPermission(context)
                }
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    AndroidView(factory = { mapView }, modifier = modifier.fillMaxSize())
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED