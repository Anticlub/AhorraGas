package com.example.ahorragas.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Looper;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationHelper {

    public interface ResultCallback {
        void onSuccess(Location location);
        void onError(String message);
    }

    private final Activity activity;
    private final FusedLocationProviderClient fusedLocation;

    public LocationHelper(Activity activity) {
        this.activity = activity;
        this.fusedLocation = LocationServices.getFusedLocationProviderClient(activity);
    }

    public boolean hasLocationPermission() {
        boolean fine = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        return fine || coarse;
    }

    public boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) activity.getSystemService(Activity.LOCATION_SERVICE);
        return lm != null && (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    public void openLocationSettings() {
        activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    public void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
        activity.startActivity(intent);
    }

    @SuppressLint("MissingPermission")
    public void getUserLocation(ResultCallback callback) {
        if (!isLocationEnabled()) {
            callback.onError("Ubicación del sistema desactivada. Actívala en Ajustes.");
            return;
        }

        if (!hasLocationPermission()) {
            callback.onError("Sin permiso de ubicación.");
            return;
        }

        fusedLocation.getLastLocation()
                .addOnSuccessListener(activity, location -> {
                    if (location != null) callback.onSuccess(location);
                    else requestOneShotUpdate(callback);
                })
                .addOnFailureListener(activity,
                        e -> callback.onError("Error leyendo ubicación: " + e.getMessage()));
    }

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

        LocationCallback oneShotCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                fusedLocation.removeLocationUpdates(this);
                Location loc = locationResult.getLastLocation();
                if (loc != null) callback.onSuccess(loc);
                else callback.onError("No se pudo obtener ubicación. Prueba en exterior.");
            }
        };

        fusedLocation.requestLocationUpdates(request, oneShotCallback, Looper.getMainLooper())
                .addOnFailureListener(e ->
                        callback.onError("Error solicitando actualización: " + e.getMessage()));
    }
}