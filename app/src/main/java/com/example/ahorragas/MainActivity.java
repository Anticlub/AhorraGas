package com.example.ahorragas;

import android.Manifest;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ahorragas.data.CachedRemoteApiDataSource;
import com.example.ahorragas.data.GasolineraRepository;
import com.example.ahorragas.data.LocalJsonDataSource;
import com.example.ahorragas.location.LocationHelper;
import com.example.ahorragas.model.Gasolinera;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button btnLocation;
    private TextView tvLocation, tvStatus, tvDataStatus;

    private LocationHelper locationHelper;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    private GasolineraRepository repo;

    // Semana 7: estado en memoria
    private List<Gasolinera> gasolineras;
    private Location userLocation;

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
        tvDataStatus = findViewById(R.id.tvDataStatus);

        btnLocation = findViewById(R.id.btnLocation);

        // ✅ Un único listener (sin test code)
        btnLocation.setOnClickListener(v -> ensureLocationPermission());

        Button btnOpenTest = findViewById(R.id.btnOpenTest);
        btnOpenTest.setOnClickListener(v ->
                startActivity(new Intent(this, TestDistanceActivity.class))
        );

        locationHelper = new LocationHelper(this);

        repo = new GasolineraRepository(
                new CachedRemoteApiDataSource(this),
                new LocalJsonDataSource(this)
        );

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
                            tvStatus.setText("Permiso denegado. Sin ubicación no se pueden mostrar gasolineras cercanas.");
                        }

                        tvLocation.setText("Ubicación: —");
                    }
                });

        // Carga datos en background al arrancar
        loadGasolinerasAsync();

        // Si quieres pedir ubicación al arrancar, déjalo. Si no, quítalo.
        ensureLocationPermission();
    }

    // ==============================
    // PERMISOS / UBICACIÓN
    // ==============================

    private void ensureLocationPermission() {
        if (locationHelper.hasLocationPermission()) {
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
                userLocation = location;
                renderLocation(location);
                tvStatus.setText("Localización OK ✅");
                checkIfReady();
            }

            @Override
            public void onError(LocationHelper.LocationError error) {
                tvStatus.setText("Error ubicación: " + error.name());
                tvLocation.setText("Ubicación: —");
            }
        });
    }

    // esto es para pintar la ubicacion por pantalla, luego se usara la ubicacion para el mapa no para pintar las coordenadas
    private void renderLocation(Location location) {
        tvLocation.setText("Ubicación: " + location.getLatitude() + ", " + location.getLongitude());
    }

    // ==============================
    // CARGA DE GASOLINERAS
    // ==============================

    private void loadGasolinerasAsync() {
        tvDataStatus.setText("Cargando gasolineras…");

        new Thread(() -> {
            try {
                List<Gasolinera> list = repo.getGasolineras();

                runOnUiThread(() -> {
                    gasolineras = list;

                    // Origen de datos
                    String originText;
                    switch (repo.getLastOrigin()) {
                        case CACHE:
                            originText = "cache (archivo)";
                            break;
                        case REMOTE:
                            originText = "remoto (API)";
                            break;
                        case LOCAL_FALLBACK:
                        default:
                            originText = "local (fallback)";
                            break;
                    }

                    tvDataStatus.setText("Gasolineras: " + list.size() + " · origen: " + originText);
                    checkIfReady();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        tvDataStatus.setText("Error cargando gasolineras")
                );
            }
        }).start();
    }

    // ==============================
    // CUANDO TODO ESTÁ LISTO
    // ==============================

    private void checkIfReady() {
        if (gasolineras != null && userLocation != null) {
            tvStatus.setText("Listo ✅ Datos + ubicación disponibles");
        }
    }
}