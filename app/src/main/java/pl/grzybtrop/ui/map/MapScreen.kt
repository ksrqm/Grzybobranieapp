package pl.grzybtrop.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import pl.grzybtrop.data.CarLocation
import pl.grzybtrop.ui.theme.Amber
import pl.grzybtrop.ui.theme.Border
import pl.grzybtrop.ui.theme.Lime
import pl.grzybtrop.ui.theme.OnLime
import pl.grzybtrop.ui.theme.Surface as SurfaceColor

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/dark"
private const val CAR_SOURCE = "car-source"
private const val CAR_LAYER = "car-layer"
private const val EMPTY_GEOJSON = """{"type":"FeatureCollection","features":[]}"""

@SuppressLint("MissingPermission") // zgodę sprawdzamy sami w hasPermission
@Composable
fun MapScreen(
    placingCar: Boolean,
    car: CarLocation?,
    onConfirmCar: (LatLng) -> Unit,
    onCancelCar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    var readyMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var readyStyle by remember { mutableStateOf<Style?>(null) }
    var locationReady by remember { mutableStateOf(false) }

    val mapView = remember {
        MapView(context).apply {
            getMapAsync { map ->
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

    // Niebieska kropka z Twoją pozycją (jak wcześniej)
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
        location.cameraMode = CameraMode.TRACKING
        location.renderMode = RenderMode.COMPASS
        location.zoomWhileTracking(15.0)
        locationReady = true // NOWE
    }

    // NOWE: pomarańczowa kropka w miejscu zapisanego auta
    LaunchedEffect(readyStyle, car) {
        val style = readyStyle ?: return@LaunchedEffect
        val json = if (car == null) {
            EMPTY_GEOJSON
        } else {
            """{"type":"Point","coordinates":[${car.longitude},${car.latitude}]}"""
        }
        val source = style.getSourceAs<GeoJsonSource>(CAR_SOURCE)
        if (source != null) {
            source.setGeoJson(json)
        } else {
            style.addSource(GeoJsonSource(CAR_SOURCE, json))
            style.addLayer(
                CircleLayer(CAR_LAYER, CAR_SOURCE).withProperties(
                    PropertyFactory.circleRadius(10f),
                    PropertyFactory.circleColor(0xFFE8A33D.toInt()),
                    PropertyFactory.circleStrokeWidth(3f),
                    PropertyFactory.circleStrokeColor(0xFFFFFFFF.toInt()),
                )
            )
        }
    }

    // NOWE: wejście w tryb wybierania miejsca = skok do Twojej pozycji
    LaunchedEffect(readyMap, locationReady, placingCar) {
        val map = readyMap ?: return@LaunchedEffect
        if (!placingCar || !locationReady) return@LaunchedEffect
        map.locationComponent.cameraMode = CameraMode.NONE // mapa przestaje jechać za kropką
        jumpToCurrentLocation(context, map)
    }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
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

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        if (placingCar) {
            // Pinezka stoi na środku, a mapa pod nią się przesuwa.
            // offset sprawia, że czubek pinezki (a nie jej środek) wskazuje środek mapy.
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "Miejsce samochodu",
                tint = Amber,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-20).dp)
                    .size(48.dp),
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceColor,
                border = BorderStroke(1.dp, Border),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Przesuń mapę, aż pinezka wskaże miejsce auta",
                        color = Color.White,
                        fontSize = 15.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onCancelCar,
                            modifier = Modifier.weight(1f),
                        ) { Text("Anuluj") }
                        Button(
                            onClick = { readyMap?.cameraPosition?.target?.let(onConfirmCar) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Lime,
                                contentColor = OnLime,
                            ),
                        ) { Text("Zapisz miejsce") }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun jumpToCurrentLocation(context: Context, map: MapLibreMap) {
    LocationServices.getFusedLocationProviderClient(context)
        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { loc ->
            if (loc != null) {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 18.0)
                )
            }
        }
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED