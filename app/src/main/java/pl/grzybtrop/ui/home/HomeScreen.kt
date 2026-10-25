package pl.grzybtrop.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import pl.grzybtrop.ui.theme.Amber
import pl.grzybtrop.ui.theme.Bg
import pl.grzybtrop.ui.theme.Border
import pl.grzybtrop.ui.theme.Danger
import pl.grzybtrop.ui.theme.Lime
import pl.grzybtrop.ui.theme.Muted
import pl.grzybtrop.ui.theme.OnLime
import pl.grzybtrop.ui.theme.Surface as SurfaceColor
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material.icons.outlined.Bedtime
import pl.grzybtrop.ui.theme.Blue
import androidx.compose.foundation.layout.WindowInsets
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

private val PL = Locale("pl", "PL")
private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", PL)
private val DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM", PL)

/** Stateful entry point: wires permissions, location updates and the ViewModel. */
@Composable
fun HomeRoute(
    hasCar: Boolean,
    onMarkCar: () -> Unit,
    onDeleteCar: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }

    LifecycleResumeEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        onPauseOrDispose { }
    }

    // Only listen to GPS while the screen is visible, to save battery.
    LifecycleStartEffect(hasPermission) {
        if (hasPermission) viewModel.startLocationUpdates()
        onStopOrDispose { viewModel.stopLocationUpdates() }
    }

    HomeScreen(
        state = state.copy(hasLocationPermission = hasPermission),
        onStartTrip = { /* TODO: switch to the active-trip screen */ },
        onMarkCar = onMarkCar,
        hasCar = hasCar,
        onDeleteCar = onDeleteCar,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    hasCar: Boolean,
    onStartTrip: () -> Unit,
    onMarkCar: () -> Unit,
    onDeleteCar: () -> Unit,
) {
    // Ticks every 30 s so the sunset countdown stays fresh.
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = ZonedDateTime.now()
        }
    }

    // Czy pokazać okienko „Usunąć lokalizację auta?”
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Header(offlineReady = state.mapDownloaded)

            Spacer(Modifier.height(24.dp))
            Text(
                "Gotowy do lasu?",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Nagramy Twoją trasę, żebyś zawsze trafił z powrotem.",
                color = Muted,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )

            Spacer(Modifier.height(24.dp))
            StatusGrid(state, now)

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onStartTrip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = OnLime),
            ) {
                Icon(Icons.Outlined.Route, contentDescription = null, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Text("Rozpocznij spacer", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { if (hasCar) showDeleteDialog = true else onMarkCar() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, if (hasCar) Danger else Border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) {
                Icon(
                    if (hasCar) Icons.Outlined.Delete else Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    tint = if (hasCar) Danger else Amber,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (hasCar) "Usuń lokalizację auta" else "Zaznacz miejsce samochodu",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(28.dp))
            Text(
                "OSTATNIE SPACERY",
                color = Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(12.dp))
            state.recentTrips.forEach { trip ->
                TripCard(trip)
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = SurfaceColor,
            titleContentColor = Color.White,
            textContentColor = Muted,
            title = { Text("Usunąć lokalizację auta?") },
            text = {
                Text("Zapisane miejsce zniknie z mapy. Żeby je odzyskać, trzeba będzie zaznaczyć je ponownie.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteCar()
                }) { Text("Usuń", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Anuluj", color = Color.White)
                }
            },
        )
    }
}

@Composable
private fun Header(offlineReady: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Eco, contentDescription = null, tint = Lime, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text("GrzybTrop", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = SurfaceColor,
            border = BorderStroke(1.dp, Border),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(if (offlineReady) Lime else Amber, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (offlineReady) "Offline gotowy" else "Brak mapy offline",
                    color = Color.White,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable

//##########################################################################
//############################ SUNSET TILE #################################
//###############################START######################################
// Sunset tile: clock time plus a countdown, which is what matters in the forest.
// computes in \app\src\main\java\pl\grzybtrop\data\SunCalc.kt
private fun StatusGrid(state: HomeUiState, now: ZonedDateTime) {
    val sunrise = state.sunrise
    val sunset = state.sunset

// Noc: przed dzisiejszym wschodem albo po dzisiejszym zachodzie.
    val isNight = sunrise != null && sunset != null &&
            (now.isBefore(sunrise) || now.isAfter(sunset))

// Który wschód jest "następny": dzisiejszy (po północy) albo jutrzejszy (wieczorem).
    val nextSunrise = if (sunrise != null && now.isBefore(sunrise)) sunrise else state.nextSunrise

    val sunsetValue = when {
        sunrise == null || sunset == null -> "—"
        isNight -> "Zmrok"
        else -> sunset.format(TIME_FORMAT)
    }
    val sunsetHint = when {
        sunrise == null || sunset == null -> null
        isNight -> nextSunrise?.let { "wschód ${it.format(TIME_FORMAT)}" }
        else -> countdown(now, sunset)
    }

    val skyColor = if (isNight) Blue else Amber
    val skyIcon = if (isNight) Icons.Outlined.Bedtime else Icons.Outlined.WbTwilight


//################################END###################################
//############################ SUNSET TILE #################################
//#########################################################################

    // GPS tile: green = good, amber = rough, red = poor.
    val acc = state.gpsAccuracyMeters
    val (gpsValue, gpsColor) = when {
        !state.hasLocationPermission -> "Brak zgody" to Danger
        acc == null -> "Szukam…" to Muted
        acc <= 10f -> "± ${acc.toInt()} m" to Lime
        acc <= 25f -> "± ${acc.toInt()} m" to Amber
        else -> "± ${acc.toInt()} m" to Danger
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                icon = skyIcon,
                iconTint = skyColor,
                label = "Zachód słońca",
                value = sunsetValue,
                hint = sunsetHint,
                modifier = Modifier.weight(1f),
                valueColor = if (isNight) skyColor else Color.White,
            )
            StatTile(
                icon = Icons.Outlined.BatteryFull,
                iconTint = Lime,
                label = "Bateria",
                value = state.batteryPercent?.let { "$it%" } ?: "—",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                icon = Icons.Outlined.MyLocation,
                iconTint = gpsColor,
                label = "Dokładność GPS",
                value = gpsValue,
                valueColor = gpsColor,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = Icons.Outlined.Map,
                iconTint = Lime,
                label = "Mapa okolicy",
                value = if (state.mapDownloaded) "Pobrana" else "Niepobrana",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White,
    hint: String? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = SurfaceColor,
        border = BorderStroke(1.dp, Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(12.dp))
            Text(label, color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = valueColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (hint != null) {
                Spacer(Modifier.height(2.dp))
                Text(hint, color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun TripCard(trip: TripSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceColor,
        border = BorderStroke(1.dp, Border),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Border, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Route, contentDescription = null, tint = Lime, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(trip.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${String.format(PL, "%.1f", trip.distanceKm)} km · ${findsLabel(trip.finds)}",
                    color = Muted,
                    fontSize = 14.sp,
                )
            }
            Text(trip.date.format(DATE_FORMAT), color = Muted, fontSize = 14.sp)
        }
    }
}

private fun countdown(now: ZonedDateTime, sunset: ZonedDateTime): String {
    val d = Duration.between(now, sunset)
    if (d.isNegative) return "po zachodzie"
    val h = d.toHours()
    val m = d.minusHours(h).toMinutes()
    return if (h > 0) "za $h h $m min" else "za $m min"
}

/** Polish plural forms: 1 znalezisko, 2-4 znaleziska, 5+ znalezisk. */
private fun findsLabel(n: Int): String {
    val form = when {
        n == 1 -> "znalezisko"
        n % 10 in 2..4 && n % 100 !in 12..14 -> "znaleziska"
        else -> "znalezisk"
    }
    return "$n $form"
}
