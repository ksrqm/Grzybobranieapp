package pl.grzybtrop.ui.home

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pl.grzybtrop.data.SunCalc
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pl.grzybtrop.data.db.AppDatabase
import pl.grzybtrop.data.db.FindEntity
import pl.grzybtrop.data.db.TripEntity
import pl.grzybtrop.data.db.TripListItem
import java.time.Instant

data class TripSummary(
    val id: Long,
    val name: String,
    val distanceKm: Double,
    val date: LocalDate,
    val findsReported: Boolean,
    val totalWeightGrams: Int,
)

data class HomeUiState(
    val hasLocationPermission: Boolean = false,
    /** Horizontal accuracy of the latest GPS fix in meters; null until the first fix. */
    val gpsAccuracyMeters: Float? = null,
    val sunset: ZonedDateTime? = null,
    val sunrise: ZonedDateTime? = null,
    val nextSunrise: ZonedDateTime? = null,

    val batteryPercent: Int? = null,
    /** Placeholder until offline map downloads are implemented. */
    val mapDownloaded: Boolean = true,
    /** Sample data until trips are stored in a local database. */
    val recentTrips: List<TripSummary> = listOf(),
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val fusedClient = LocationServices.getFusedLocationProviderClient(app)
    private var updating = false
    private val dao = AppDatabase.get(app).tripDao()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            val zone = ZoneId.systemDefault()

            val today = LocalDate.now(zone)
            val sunrise = SunCalc.sunrise(today, loc.latitude, loc.longitude)?.atZone(zone)
            val sunset = SunCalc.sunset(today, loc.latitude, loc.longitude)?.atZone(zone)
            val nextSunrise = SunCalc.sunrise(today.plusDays(1), loc.latitude, loc.longitude)?.atZone(zone)

            _uiState.update {
                it.copy(
                    gpsAccuracyMeters = if (loc.hasAccuracy()) loc.accuracy else null,
                    sunset = sunset,
                    sunrise = sunrise,
                    nextSunrise = nextSunrise,
                )
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = publishBattery(intent)
    }

    init {
        val sticky = ContextCompat.registerReceiver(
            app,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        sticky?.let(::publishBattery)
    }
    init {
        // Lista „Ostatnie spacery” odświeża się sama, gdy zmieni się baza
        viewModelScope.launch {
            dao.observeRecentTrips(5).collect { items ->
                _uiState.update { it.copy(recentTrips = items.map { item -> item.toSummary() }) }
            }
        }

        // TYMCZASOWE: dwa przykładowe spacery, żeby zobaczyć karty. Usuniemy w etapie 1b.
        viewModelScope.launch {
            if (dao.countTrips() > 0) return@launch
            val now = System.currentTimeMillis()
            val day = 24 * 60 * 60 * 1000L
            val hour = 60 * 60 * 1000L

            val withWeight = dao.insertTrip(
                TripEntity(
                    name = "Las Osobowicki",
                    startedAtMillis = now - 2 * day,
                    endedAtMillis = now - 2 * day + 2 * hour,
                    distanceMeters = 3400.0,
                    findsReported = true,
                )
            )
            dao.insertFinds(
                listOf(
                    FindEntity(tripId = withWeight, species = "Borowik szlachetny", weightGrams = 800),
                    FindEntity(tripId = withWeight, species = "Kurka", weightGrams = 400),
                )
            )
            dao.insertTrip(
                TripEntity(
                    name = "Lasy Milickie",
                    startedAtMillis = now - 5 * day,
                    endedAtMillis = now - 5 * day + 3 * hour,
                    distanceMeters = 5100.0,
                    findsReported = false,
                )
            )
        }
    }

    private fun publishBattery(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level >= 0 && scale > 0) {
            _uiState.update { it.copy(batteryPercent = level * 100 / scale) }
        }
    }

    /** Call only after location permission has been granted. */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (updating) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L).build()
        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        updating = true
    }

    fun stopLocationUpdates() {
        if (!updating) return
        fusedClient.removeLocationUpdates(locationCallback)
        updating = false
    }

    override fun onCleared() {
        stopLocationUpdates()
        getApplication<Application>().unregisterReceiver(batteryReceiver)
    }
    private fun TripListItem.toSummary() = TripSummary(
        id = id,
        name = name,
        distanceKm = distanceMeters / 1000.0,
        date = Instant.ofEpochMilli(startedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate(),
        findsReported = findsReported,
        totalWeightGrams = totalWeightGrams,
    )
}
