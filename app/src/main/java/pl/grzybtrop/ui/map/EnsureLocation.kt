package pl.grzybtrop.ui.map

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

/**
 * Zwraca funkcję do wywołania po kliknięciu przycisku. Ona po kolei:
 * 1) prosi o zgodę na lokalizację (jeśli jej brak),
 * 2) prosi o włączenie lokalizacji w telefonie (jeśli wyłączona),
 * 3) dopiero potem woła onReady. Przy odmowie woła onDenied.
 */
@Composable
fun rememberEnsureLocation(onReady: () -> Unit, onDenied: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnReady by rememberUpdatedState(onReady)
    val currentOnDenied by rememberUpdatedState(onDenied)

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) currentOnReady() else currentOnDenied()
    }

    fun checkPhoneLocationSetting() {
        val request = LocationSettingsRequest.Builder()
            .addLocationRequest(
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L).build()
            )
            .build()
        LocationServices.getSettingsClient(context)
            .checkLocationSettings(request)
            .addOnSuccessListener { currentOnReady() }
            .addOnFailureListener { error ->
                if (error is ResolvableApiException) {
                    settingsLauncher.launch(IntentSenderRequest.Builder(error.resolution).build())
                } else {
                    currentOnDenied()
                }
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            checkPhoneLocationSetting()
        } else {
            currentOnDenied()
        }
    }

    return {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            checkPhoneLocationSetting()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }
}