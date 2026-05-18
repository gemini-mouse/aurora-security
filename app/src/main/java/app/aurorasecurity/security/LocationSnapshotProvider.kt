package app.aurorasecurity.security

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class AlertLocation(
    val latitude: Double,
    val longitude: Double,
) {
    val googleMapsUrl: String
        get() = "https://maps.google.com/?q=$latitude,$longitude"
}

class LocationSnapshotProvider(private val context: Context) {
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): AlertLocation? {
        if (!hasLocationPermission()) return null

        val client = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setDurationMillis(10_000)
            .build()

        val location = suspendCancellableCoroutine<Location?> { continuation ->
            client.getCurrentLocation(request, cancellationTokenSource.token)
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resume(null) }
            continuation.invokeOnCancellation { cancellationTokenSource.cancel() }
        } ?: return null

        return AlertLocation(
            latitude = location.latitude,
            longitude = location.longitude,
        )
    }
}
