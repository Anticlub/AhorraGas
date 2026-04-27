package com.example.ahorragas.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Clase encargada de gestionar toda la lógica relacionada con la ubicación.
 *
 * Responsabilidades:
 * - Comprobar permisos de ubicación.
 * - Verificar si la ubicación del sistema está activada.
 * - Obtener la ubicación del usuario (lastLocation + fallback one-shot).
 * - Manejar errores y estados especiales.
 *
 * Esta clase desacopla la lógica de ubicación de la UI (MainActivity),
 * facilitando mantenimiento y reutilización en futuras pantallas (mapa, lista, etc.).
 */
public class LocationHelper {


    /**
     * Tipos de error posibles al obtener la ubicación.
     */
    public enum LocationError {
        NO_PERMISSION,
        GPS_DISABLED,
        TIMEOUT,
        TECHNICAL_ERROR
    }

    /**
     * Callback para comunicar resultados a la UI.
     */
    public interface ResultCallback {
        void onSuccess(Location location);
        void onError(LocationError error);
    }

    // Si la última ubicación tiene más de 2 minutos, la consideramos desactualizada // pensar q tiempo es el ideal
    private static final long MAX_LAST_LOCATION_AGE_MS = 2 * 60 * 1000;
    private final Activity activity;
    private final FusedLocationProviderClient fusedLocation;


    /**
     * Constructor.
     *
     * @param activity Activity desde la que se solicita la ubicación.
     */
    public LocationHelper(Activity activity) {
        this.activity = activity;
        this.fusedLocation = LocationServices.getFusedLocationProviderClient(activity);
    }

    /**
     * Comprueba si el usuario ha concedido permisos de ubicación.
     *
     * Se considera válido si al menos uno de los siguientes está concedido:
     * - ACCESS_FINE_LOCATION
     * - ACCESS_COARSE_LOCATION
     *
     * @return true si hay permiso concedido.
     */
    public boolean hasLocationPermission() {
        boolean fine = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        return fine || coarse;
    }

    /**
     * Verifica si la ubicación del sistema (GPS o red) está activada.
     *
     * @return true si al menos un proveedor está habilitado.
     */
    public boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) activity.getSystemService(Activity.LOCATION_SERVICE);
        return lm != null && (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    /**
     * Abre la pantalla de ajustes del sistema para activar la ubicación.
     */
    public void openLocationSettings() {
        activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    /**
     * Abre la pantalla de ajustes de la aplicación (para permisos permanentes).
     */
    public void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
        activity.startActivity(intent);
    }

    /**
     * Obtiene la ubicación actual del usuario.
     *
     * Flujo:
     * 1. Comprueba si la ubicación del sistema está activada.
     * 2. Comprueba si existen permisos.
     * 3. Intenta obtener la última ubicación conocida (rápido).
     * 4. Si es null, solicita una actualización puntual (one-shot).
     *
     * @param callback Devuelve ubicación o mensaje de error.
     */
    @SuppressLint("MissingPermission")
    public void getUserLocation(ResultCallback callback) {
        if (!isLocationEnabled()) {
            callback.onError(LocationError.GPS_DISABLED);
            return;
        }

        if (!hasLocationPermission()) {
            callback.onError(LocationError.NO_PERMISSION);
            return;
        }

        fusedLocation.getLastLocation()
                .addOnSuccessListener(activity, location -> {
                    if (location != null) {
                        long ageMs = System.currentTimeMillis() - location.getTime();

                        if (ageMs <= MAX_LAST_LOCATION_AGE_MS) {
                            callback.onSuccess(location);
                        } else {
                            requestOneShotUpdate(callback);
                        }
                    } else {
                        requestOneShotUpdate(callback);
                    }
                })
                .addOnFailureListener(activity,
                        e -> callback.onError(LocationError.TECHNICAL_ERROR));
    }


    /**
     * Solicita una única actualización de ubicación como fallback.
     *
     * Se ejecuta cuando getLastLocation() devuelve null.
     * Incluye un timeout de 12 segundos para evitar bloqueos indefinidos.
     *
     * @param callback Devuelve ubicación o mensaje de error.
     */
    @SuppressLint("MissingPermission")
    private void requestOneShotUpdate(ResultCallback callback) {
        boolean fineGranted = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        int priority = fineGranted ? Priority.PRIORITY_HIGH_ACCURACY
                : Priority.PRIORITY_BALANCED_POWER_ACCURACY;

        LocationRequest request = new LocationRequest.Builder(priority, 1000)
                .setMinUpdateIntervalMillis(500)
                .setMaxUpdates(1)
                .build();

        final boolean[] finished = {false};

        LocationCallback oneShotCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (finished[0]) return;
                finished[0] = true;

                fusedLocation.removeLocationUpdates(this);

                Location loc = locationResult.getLastLocation();
                if (loc != null) callback.onSuccess(loc);
                else callback.onError(LocationError.TECHNICAL_ERROR);
            }
        };

        fusedLocation.requestLocationUpdates(request, oneShotCallback, Looper.getMainLooper())
                .addOnFailureListener(e -> {
                    if (finished[0]) return;
                    finished[0] = true;
                    callback.onError(LocationError.TECHNICAL_ERROR);
                });

        // si en 12s no llega nada, cortamos
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (finished[0]) return;
            finished[0] = true;

            fusedLocation.removeLocationUpdates(oneShotCallback);
            callback.onError(LocationError.TIMEOUT);
            }, 12_000);
    }

    /**
     * Cancela cualquier petición de ubicación en curso.
     * Debe llamarse desde onDestroy() de la Activity para evitar
     * callbacks tras la destrucción de la Activity.
     */
    public void cancel() {
        fusedLocation.removeLocationUpdates(new LocationCallback() {});
    }
}