package pl.grzybtrop.data

import android.content.Context

data class CarLocation(
    val latitude: Double,
    val longitude: Double,
    val savedAtMillis: Long,
)

/** Zapisuje miejsce auta w pamięci aplikacji, żeby przetrwało restart. */
class CarLocationStore(context: Context) {
    private val prefs = context.getSharedPreferences("car_location", Context.MODE_PRIVATE)

    fun load(): CarLocation? {
        val lat = prefs.getString("lat", null)?.toDoubleOrNull() ?: return null
        val lon = prefs.getString("lon", null)?.toDoubleOrNull() ?: return null
        return CarLocation(lat, lon, prefs.getLong("savedAt", 0L))
    }

    fun save(car: CarLocation) {
        prefs.edit()
            .putString("lat", car.latitude.toString())
            .putString("lon", car.longitude.toString())
            .putLong("savedAt", car.savedAtMillis)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}