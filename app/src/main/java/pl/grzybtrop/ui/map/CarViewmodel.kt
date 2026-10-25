package pl.grzybtrop.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.grzybtrop.data.CarLocation
import pl.grzybtrop.data.CarLocationStore

class CarViewModel(app: Application) : AndroidViewModel(app) {
    private val store = CarLocationStore(app)

    private val _car = MutableStateFlow(store.load())
    val car: StateFlow<CarLocation?> = _car.asStateFlow()

    private val _placing = MutableStateFlow(false)
    val placing: StateFlow<Boolean> = _placing.asStateFlow()

    fun startPlacing() { _placing.value = true }
    fun cancelPlacing() { _placing.value = false }

    fun saveCar(latitude: Double, longitude: Double) {
        val car = CarLocation(latitude, longitude, System.currentTimeMillis())
        store.save(car)
        _car.value = car
        _placing.value = false
    }

    fun deleteCar() {
        store.clear()
        _car.value = null
    }
}