package com.example.ahorragas;

import android.Manifest;

import android.os.Bundle;

import android.location.Location;

import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ahorragas.location.LocationHelper;

public class MainActivity extends AppCompatActivity {

    private Button btnLocation;
    private TextView tvLocation, tvStatus;
    private LocationHelper locationHelper;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvLocation = findViewById(R.id.tvLocation);
        tvStatus = findViewById(R.id.tvStatus);

        btnLocation = findViewById(R.id.btnLocation);

        btnLocation.setOnClickListener(v -> {
            ensureLocationPermission();
        });

        locationHelper = new LocationHelper(this);

        locationPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    boolean fine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                    boolean coarse = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));

                    if (fine || coarse) {
                        tvStatus.setText("Permiso de ubicación concedido ✅");
                        requestUserLocation();
                    } else {
                        boolean canAskAgainFine =
                                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
                        boolean canAskAgainCoarse =
                                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION);

                        if (!canAskAgainFine && !canAskAgainCoarse) {
                            tvStatus.setText("Permiso denegado permanentemente. Actívalo en Ajustes.");
                            locationHelper.openAppSettings();
                        } else {
                            tvStatus.setText("Permiso denegado. Sin ubicación no se pueden mostrar gasolineras cerca de ti, tienes que usar el localizador y poner un lugares.");
                        }

                        tvLocation.setText("Ubicación: —");
                    }
                });

        ensureLocationPermission();
    }

    private void ensureLocationPermission() {
        if (locationHelper.hasLocationPermission()) {
            tvStatus.setText("Permiso de ubicación OK ✅");
            requestUserLocation();
        } else {
            tvStatus.setText("Solicitando permiso de ubicación…");
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void requestUserLocation() {
        tvStatus.setText("Obteniendo ubicación…");

        locationHelper.getUserLocation(new LocationHelper.ResultCallback() {
            @Override
            public void onSuccess(Location location) {
                renderLocation(location);
            }

            @Override
            public void onError(LocationHelper.LocationError error) {
                // DEBUG: muestra el error exacto
                renderLocationError("ERROR: " + error.name());

                switch (error) {
                    case NO_PERMISSION:
                        renderLocationError("ERROR: NO_PERMISSION (no hay permisos)");
                        break;

                    case GPS_DISABLED:
                        renderLocationError("ERROR: GPS_DISABLED (ubicación del sistema apagada)");
                        locationHelper.openLocationSettings();
                        break;

                    case TIMEOUT:
                        renderLocationError("ERROR: TIMEOUT (no se obtuvo ubicación a tiempo)");
                        break;

                    case TECHNICAL_ERROR:
                        renderLocationError("ERROR: TECHNICAL_ERROR (fallo de Google Location Services)");
                        break;
                }
            }
        });
    }


    // esto es para pintar la ubicacion por pantalla, luego se usara la ubicacion para el mapa no para pintar las coordenadas
    private void renderLocation(Location location) {
        tvStatus.setText("Ubicación OK ✅");
        tvLocation.setText("Ubicación: " + location.getLatitude() + ", " + location.getLongitude());
    }

    private void renderLocationError(String message) {
        tvStatus.setText(message);
        tvLocation.setText("Ubicación: —");
    }


}