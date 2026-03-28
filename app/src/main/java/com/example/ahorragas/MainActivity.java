package com.example.ahorragas;

import com.example.ahorragas.map.GasolineraInfoWindow;

import android.Manifest;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ahorragas.adapter.GasolineraAdapter;
import com.example.ahorragas.data.CachedRemoteApiDataSource;
import com.example.ahorragas.data.DataSourceOrigin;
import com.example.ahorragas.data.GasolineraRepository;
import com.example.ahorragas.data.RepoError;
import com.example.ahorragas.location.LocationHelper;
import com.example.ahorragas.map.MarkerBitmapFactory;
import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Gasolinera;
import com.example.ahorragas.model.PriceRange;
import com.example.ahorragas.model.Vehicle;
import com.example.ahorragas.util.GasolineraSorter;
import com.example.ahorragas.util.RadiusUtils;
import com.example.ahorragas.util.VehiclePrefs;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String PREF_SELECTED_FUEL = "pref_selected_fuel";
    private static final GeoPoint SPAIN_CENTER = new GeoPoint(40.4168, -3.7038);
    private static final double ZOOM_SPAIN = 6.0;
    private static final double ZOOM_USER = 14.0;
    private static final double ZOOM_STATION = 16.0;
    private static final int MAX_MAP_MARKERS = 100;
    private static final int INFO_DELAY_MS = 450;

    private MapView mapView;
    private RecyclerView recyclerView;
    private Button btnMostrarCerca;
    private FloatingActionButton fabMiUbicacion;
    private TextView tvStationsCount;
    private TextView tvDataStatus;
    private TextView tvLocation;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNav;
    private int lastRadiusKm = RadiusUtils.DEFAULT_KM;
    private int lastMarkersCount = RadiusUtils.DEFAULT_MARKERS;

    private final List<Gasolinera> allGasolineras = new ArrayList<>();
    private List<Gasolinera> visibleGasolineras = new ArrayList<>();
    private GasolineraAdapter adapter;
    private FuelType selectedFuel = FuelType.GASOLEO_A;
    private Location userLocation;
    private final Map<Integer, Marker> markerMap = new HashMap<>();
    private MyLocationNewOverlay locationOverlay;

    private CachedRemoteApiDataSource dataSource;
    private GasolineraRepository repository;
    private LocationHelper locationHelper;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        boolean fineGranted = Boolean.TRUE.equals(
                                result.get(Manifest.permission.ACCESS_FINE_LOCATION)
                        );
                        boolean coarseGranted = Boolean.TRUE.equals(
                                result.get(Manifest.permission.ACCESS_COARSE_LOCATION)
                        );

                        if (fineGranted || coarseGranted) {
                            requestUserLocation();
                        } else {
                            showSpainFallback();
                            updateLocationStatus(getString(R.string.status_location_denied));
                            btnMostrarCerca.setEnabled(false);
                            Toast.makeText(
                                    this,
                                    R.string.location_permission_message,
                                    Toast.LENGTH_LONG
                            ).show();
                            renderMetaStatus();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(
                getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
        );
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_main);

        dataSource = new CachedRemoteApiDataSource(this);
        repository = new GasolineraRepository(dataSource);
        locationHelper = new LocationHelper(this);

        selectedFuel = FuelType.fromString(
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getString(PREF_SELECTED_FUEL, FuelType.GASOLEO_A.name())
        );

        initViews();
        setupMap();
        setupRecyclerView();
        setupButtons();
        setupBottomNav();

        updateLocationStatus(getString(R.string.status_location_pending));
        tvDataStatus.setText(R.string.status_loading_data);

        loadGasolineras();
        requestLocationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();

        if (locationOverlay != null && locationHelper.hasLocationPermission()) {
            locationOverlay.enableMyLocation();
        }

        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_map);
        }

        // Si no hay ningún vehículo, mostrar diálogo bloqueante
        if (!VehiclePrefs.hasVehicles(this)) {
            showFirstVehicleDialog();
            return;
        }

        // Sincronizar combustible con el vehículo activo
        FuelType savedFuel = FuelType.fromString(
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getString(PREF_SELECTED_FUEL, FuelType.GASOLEO_A.name())
        );
        if (savedFuel != selectedFuel) {
            selectedFuel = savedFuel;
            MarkerBitmapFactory.clearCache();
            updateDisplayForFuel(selectedFuel);
        }

        // si el radio de preferencias es diferente se actualiza
        int currentRadius = RadiusUtils.loadRadiusKm(this);
        if (currentRadius != lastRadiusKm) {
            lastRadiusKm = currentRadius;
            updateDisplayForFuel(selectedFuel);
        }

        // si el marcador de las gasolineras cambia se actualiza
        int currentMarkers = RadiusUtils.loadMarkersCount(this);
        if (currentMarkers != lastMarkersCount) {
            lastMarkersCount = currentMarkers;
            updateDisplayForFuel(selectedFuel);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();

        if (locationOverlay != null) {
            locationOverlay.disableMyLocation();
            locationOverlay.disableFollowLocation();
        }
    }

    // ─── DIÁLOGO PRIMER VEHÍCULO ─────────────────────────────────────────────

    /**
     * Diálogo bloqueante que se muestra cuando no hay ningún vehículo guardado.
     * No tiene botón cancelar y no se puede cerrar tocando fuera.
     * Al guardar correctamente, sincroniza el combustible y refresca la UI.
     */
    private void showFirstVehicleDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(12), dp(20), dp(4));

        TextView labelName = new TextView(this);
        labelName.setText("Nombre del vehículo");
        labelName.setTextColor(0xFF333333);
        labelName.setTextSize(13);
        layout.addView(labelName);

        EditText etName = new EditText(this);
        etName.setHint("Ej: Mi Coche, Furgoneta…");
        etName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        layout.addView(etName);

        TextView labelCons = new TextView(this);
        labelCons.setText("Consumo (L/100 km)");
        labelCons.setTextColor(0xFF333333);
        labelCons.setTextSize(13);
        labelCons.setPadding(0, dp(12), 0, 0);
        layout.addView(labelCons);

        EditText etCons = new EditText(this);
        etCons.setHint("Ej: 6.5");
        etCons.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etCons);

        TextView labelFuel = new TextView(this);
        labelFuel.setText("Tipo de combustible");
        labelFuel.setTextColor(0xFF333333);
        labelFuel.setTextSize(13);
        labelFuel.setPadding(0, dp(12), 0, 0);
        layout.addView(labelFuel);

        FuelType[] fuels = FuelType.values();
        String[] fuelNames = new String[fuels.length];
        for (int i = 0; i < fuels.length; i++) fuelNames[i] = fuels[i].displayName();

        final FuelType[] selectedFuelLocal = {FuelType.GASOLEO_A};

        TextView tvFuelSelector = new TextView(this);
        tvFuelSelector.setText(selectedFuelLocal[0].displayName());
        tvFuelSelector.setTextColor(0xFF000000);
        tvFuelSelector.setBackgroundColor(0xFFEEEEEE);
        tvFuelSelector.setPadding(dp(12), dp(10), dp(12), dp(10));
        tvFuelSelector.setTextSize(14);
        tvFuelSelector.setClickable(true);
        tvFuelSelector.setFocusable(true);
        tvFuelSelector.setOnClickListener(v -> {
            int checked = 0;
            for (int i = 0; i < fuels.length; i++) {
                if (fuels[i] == selectedFuelLocal[0]) { checked = i; break; }
            }
            new AlertDialog.Builder(this)
                    .setTitle("Combustible")
                    .setSingleChoiceItems(fuelNames, checked, (d, which) -> {
                        selectedFuelLocal[0] = fuels[which];
                        tvFuelSelector.setText(selectedFuelLocal[0].displayName());
                        d.dismiss();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
        layout.addView(tvFuelSelector);

        // Construir diálogo — sin cancelar, sin cerrar tocando fuera
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Añade tu vehículo para empezar")
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton("Guardar", null) // null para manejar manualmente y evitar cierre automático
                .create();

        dialog.show();

        // Sobreescribir el botón positivo para validar antes de cerrar
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String consStr = etCons.getText().toString().trim().replace(",", ".");

            if (name.isEmpty()) {
                Toast.makeText(this, "Escribe un nombre.", Toast.LENGTH_SHORT).show();
                return;
            }

            double cons;
            try {
                cons = Double.parseDouble(consStr);
                if (cons <= 0 || cons > 100) throw new NumberFormatException();
            } catch (Exception e) {
                Toast.makeText(this, "Consumo no válido (ej: 6.5).", Toast.LENGTH_SHORT).show();
                return;
            }

            Vehicle vehicle = new Vehicle(name, selectedFuelLocal[0], cons);
            VehiclePrefs.addVehicle(this, vehicle); // también sincroniza pref_selected_fuel

            selectedFuel = selectedFuelLocal[0];
            MarkerBitmapFactory.clearCache();
            updateDisplayForFuel(selectedFuel);

            dialog.dismiss();
        });
    }

    // ─── VIEWS ───────────────────────────────────────────────────────────────

    private void initViews() {
        mapView = findViewById(R.id.mapView);
        recyclerView = findViewById(R.id.recyclerView);
        btnMostrarCerca = findViewById(R.id.btnMostrarCerca);
        fabMiUbicacion = findViewById(R.id.fabMiUbicacion);
        tvStationsCount = findViewById(R.id.tvStationsCount);
        tvDataStatus = findViewById(R.id.tvDataStatus);
        tvLocation = findViewById(R.id.tvLocation);
        progressBar = findViewById(R.id.progressBar);
        bottomNav = findViewById(R.id.bottomNav);
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);
        showSpainFallback();

        mapView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                InfoWindow.closeAllInfoWindowsOn(mapView);
                if (locationOverlay != null) {
                    locationOverlay.disableFollowLocation();
                }
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        adapter = new GasolineraAdapter(
                new ArrayList<>(),
                selectedFuel,
                this::animateMapToGasolinera
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupButtons() {
        btnMostrarCerca.setOnClickListener(v -> {
            if (userLocation == null) {
                Toast.makeText(this, R.string.location_gps_message, Toast.LENGTH_LONG).show();
                requestLocationPermission();
                return;
            }

            int radiusKm = RadiusUtils.loadRadiusKm(this);
            double radiusMeters = RadiusUtils.kmToMetersClamped(radiusKm);
            int markersCount = RadiusUtils.loadMarkersCount(this);

            List<Gasolinera> inRadius = GasolineraSorter.getForMap(
                    GasolineraSorter.filterByFuel(allGasolineras, selectedFuel),
                    userLocation.getLatitude(),
                    userLocation.getLongitude(),
                    radiusMeters,
                    markersCount
            );

            clearMapMarkers();
            for (Gasolinera g : inRadius) addMarker(g);
            mapView.invalidate();

            if (!inRadius.isEmpty()) focusOnGasolinera(inRadius.get(0));

            Toast.makeText(
                    this,
                    getString(R.string.showing_nearest, inRadius.size()),
                    Toast.LENGTH_SHORT
            ).show();
        });

        fabMiUbicacion.setOnClickListener(v -> {
            if (userLocation != null) {
                if (locationOverlay != null) {
                    locationOverlay.enableFollowLocation();
                }
                GeoPoint point = new GeoPoint(
                        userLocation.getLatitude(),
                        userLocation.getLongitude()
                );
                mapView.getController().animateTo(point);
                mapView.getController().setZoom(ZOOM_USER);
            } else {
                requestLocationPermission();
            }
        });
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_map);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_map) {
                return true;
            } else if (id == R.id.nav_preferences) {
                startActivity(new Intent(this, PreferencesActivity.class));
                return true;
            } else if (id == R.id.nav_distance) {
            startActivity(new Intent(this, DistanceListActivity.class));
            return true;
            } else {
                return false;
            }
        });
    }

    private void requestLocationPermission() {
        if (locationHelper.hasLocationPermission()) {
            requestUserLocation();
        } else {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void requestUserLocation() {
        if (!locationHelper.isLocationEnabled()) {
            showSpainFallback();
            updateLocationStatus(getString(R.string.status_location_disabled));
            btnMostrarCerca.setEnabled(false);
            Toast.makeText(this, R.string.location_gps_message, Toast.LENGTH_LONG).show();
            renderMetaStatus();
            return;
        }

        updateLocationStatus(getString(R.string.status_location_loading));

        locationHelper.getUserLocation(new LocationHelper.ResultCallback() {
            @Override
            public void onSuccess(Location location) {
                applyUserLocation(location);
            }

            @Override
            public void onError(LocationHelper.LocationError error) {
                showSpainFallback();
                btnMostrarCerca.setEnabled(false);
                updateLocationStatus(buildLocationErrorMessage(error));
                Toast.makeText(
                        MainActivity.this,
                        buildLocationToast(error),
                        Toast.LENGTH_LONG
                ).show();
                updateDisplayForFuel(selectedFuel);
            }
        });
    }

    private void applyUserLocation(Location location) {
        userLocation = location;
        addMyLocationOverlay();

        GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
        mapView.getController().animateTo(point);
        mapView.getController().setZoom(ZOOM_USER);

        updateLocationStatus(getString(
                R.string.status_location_format,
                location.getLatitude(),
                location.getLongitude()
        ));

        updateDisplayForFuel(selectedFuel);
    }

    private void loadGasolineras() {
        progressBar.setVisibility(View.VISIBLE);
        btnMostrarCerca.setEnabled(false);

        tvDataStatus.setText(
                repository.getLastOrigin() == null
                        ? getString(R.string.status_loading_data)
                        : getString(R.string.status_refreshing_data)
        );

        new Thread(() -> {
            try {
                List<Gasolinera> loaded = repository.getGasolineras();

                runOnUiThread(() -> {
                    allGasolineras.clear();
                    allGasolineras.addAll(loaded);
                    progressBar.setVisibility(View.GONE);
                    updateDisplayForFuel(selectedFuel);
                });

            } catch (RepoError error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvDataStatus.setText(
                            getString(R.string.error_loading_data) + ": " + buildRepoErrorMessage(error)
                    );
                    if (allGasolineras.isEmpty()) {
                        clearMapMarkers();
                        adapter.updateData(Collections.emptyList(), selectedFuel);
                        tvStationsCount.setText(getString(R.string.stations_count_format, 0));
                    }
                    Toast.makeText(MainActivity.this, buildRepoErrorMessage(error), Toast.LENGTH_LONG).show();
                });

            } catch (Exception error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvDataStatus.setText(getString(R.string.error_loading_data) + ": " + error.getMessage());
                    Toast.makeText(MainActivity.this, getString(R.string.error_loading_data), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void updateDisplayForFuel(FuelType fuel) {
        selectedFuel = fuel != null ? fuel : FuelType.GASOLEO_A;
        visibleGasolineras = buildVisibleGasolineras(selectedFuel);

        PriceRange range = GasolineraSorter.calculatePriceRange(visibleGasolineras, selectedFuel);
        for (Gasolinera gasolinera : visibleGasolineras) {
            gasolinera.setPriceLevel(
                    GasolineraSorter.getPriceLevel(
                            gasolinera.getPrecio(selectedFuel),
                            range
                    )
            );
        }

        adapter.updateData(visibleGasolineras, selectedFuel);
        tvStationsCount.setText(getString(R.string.stations_count_format, visibleGasolineras.size()));
        btnMostrarCerca.setEnabled(userLocation != null && !visibleGasolineras.isEmpty());

        if (allGasolineras.isEmpty()) {
            clearMapMarkers();
            renderMetaStatus();
            return;
        }

        if (visibleGasolineras.isEmpty()) {
            clearMapMarkers();
            Toast.makeText(this,
                    getString(R.string.no_stations_for_fuel, selectedFuel.displayName()),
                    Toast.LENGTH_SHORT).show();
        } else {
            showStationsOnMap(RadiusUtils.loadMarkersCount(this));
        }

        renderMetaStatus();
    }

    private List<Gasolinera> buildVisibleGasolineras(FuelType fuel) {
        List<Gasolinera> filtered = GasolineraSorter.filterByFuel(allGasolineras, fuel);

        if (userLocation != null) {
            int radiusKm = RadiusUtils.loadRadiusKm(this);
            double radiusMeters = RadiusUtils.kmToMetersClamped(radiusKm);
            return GasolineraSorter.getForMap(
                    filtered,
                    userLocation.getLatitude(),
                    userLocation.getLongitude(),
                    radiusMeters,
                    RadiusUtils.loadMarkersCount(this)
            );
        }

        for (Gasolinera gasolinera : filtered) {
            gasolinera.setDistanceMeters(null);
        }

        filtered.sort(
                Comparator
                        .comparing(
                                (Gasolinera g) -> g.getPrecio(fuel),
                                Comparator.nullsLast(Double::compareTo)
                        )
                        .thenComparing(
                                g -> safeText(g.getMarca()),
                                String.CASE_INSENSITIVE_ORDER
                        )
        );

        return filtered;
    }

    private void renderMetaStatus() {
        String originText = getOriginLabel(repository.getLastOrigin());
        String orderText = userLocation != null
                ? getString(R.string.order_by_distance)
                : getString(R.string.order_by_price);

        tvDataStatus.setText(getString(
                R.string.data_status_format,
                originText,
                allGasolineras.size(),
                visibleGasolineras.size(),
                selectedFuel.displayName(),
                orderText
        ));
    }

    private void showStationsOnMap(int count) {
        clearMapMarkers();
        int toShow = Math.min(count, visibleGasolineras.size());
        for (int i = 0; i < toShow; i++) {
            addMarker(visibleGasolineras.get(i));
        }
        mapView.invalidate();
    }

    private void addMarker(Gasolinera gasolinera) {
        if (gasolinera.getLat() == null || gasolinera.getLon() == null) return;

        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(gasolinera.getLat(), gasolinera.getLon()));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        String marca = safeText(gasolinera.getMarca());
        marker.setTitle(marca.isEmpty() ? "Sin marca" : marca);
        marker.setSnippet(buildMarkerSnippet(gasolinera));

        marker.setInfoWindow(new GasolineraInfoWindow(mapView, gasolinera, selectedFuel));

        marker.setIcon(new android.graphics.drawable.BitmapDrawable(
                getResources(),
                MarkerBitmapFactory.createMarker(this, gasolinera, selectedFuel)
        ));

        marker.setOnMarkerClickListener((clickedMarker, ignoredMapView) -> {
            InfoWindow.closeAllInfoWindowsOn(mapView);
            clickedMarker.showInfoWindow();
            scrollListToGasolinera(gasolinera);
            return true;
        });

        mapView.getOverlays().add(marker);
        markerMap.put(gasolinera.getId(), marker);
    }

    private void clearMapMarkers() {
        List<Overlay> toRemove = new ArrayList<>();
        for (Overlay overlay : mapView.getOverlays()) {
            if (overlay instanceof Marker) toRemove.add(overlay);
        }
        mapView.getOverlays().removeAll(toRemove);
        markerMap.clear();
    }

    private void animateMapToGasolinera(Gasolinera gasolinera) {
        focusOnGasolinera(gasolinera);
        Marker marker = markerMap.get(gasolinera.getId());
        if (marker != null) {
            new Handler(Looper.getMainLooper())
                    .postDelayed(marker::showInfoWindow, INFO_DELAY_MS);
        }
    }

    private void focusOnGasolinera(Gasolinera gasolinera) {
        if (gasolinera.getLat() == null || gasolinera.getLon() == null) return;
        mapView.getController().animateTo(
                new GeoPoint(gasolinera.getLat(), gasolinera.getLon())
        );
        mapView.getController().setZoom(ZOOM_STATION);
    }

    private void scrollListToGasolinera(Gasolinera gasolinera) {
        int position = adapter.getPositionOf(gasolinera);
        if (position >= 0) recyclerView.smoothScrollToPosition(position);
    }

    private void addMyLocationOverlay() {
        if (locationOverlay != null || !locationHelper.hasLocationPermission()) return;
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        locationOverlay.enableMyLocation();
        mapView.getOverlays().add(locationOverlay);
        mapView.invalidate();
    }

    private void showSpainFallback() {
        mapView.getController().setZoom(ZOOM_SPAIN);
        mapView.getController().setCenter(SPAIN_CENTER);
    }

    private String buildRepoErrorMessage(RepoError error) {
        switch (error.getType()) {
            case NETWORK:        return "Sin conexión con la API";
            case TIMEOUT:        return "La API tardó demasiado en responder";
            case HTTP:           return "Error HTTP " + error.getHttpCode();
            case EMPTY_RESPONSE: return "La API devolvió una respuesta vacía";
            case PARSE:
            default:             return "No se pudieron procesar las gasolineras";
        }
    }

    private String buildLocationErrorMessage(LocationHelper.LocationError error) {
        switch (error) {
            case GPS_DISABLED:   return getString(R.string.status_location_disabled);
            case NO_PERMISSION:  return getString(R.string.status_location_denied);
            case TIMEOUT:
            case TECHNICAL_ERROR:
            default:             return getString(R.string.status_location_error);
        }
    }

    private String buildLocationToast(LocationHelper.LocationError error) {
        switch (error) {
            case GPS_DISABLED:   return getString(R.string.location_gps_message);
            case NO_PERMISSION:  return getString(R.string.location_permission_message);
            case TIMEOUT:        return "No se pudo obtener la ubicación a tiempo.";
            case TECHNICAL_ERROR:
            default:             return "No se pudo obtener la ubicación actual.";
        }
    }

    private void updateLocationStatus(String value) { tvLocation.setText(value); }

    private String getOriginLabel(DataSourceOrigin origin) {
        if (origin == null) return getString(R.string.origin_unknown);
        switch (origin) {
            case REMOTE:         return getString(R.string.origin_remote);
            case CACHE:          return getString(R.string.origin_cache);
            case LOCAL_FALLBACK:
            default:             return getString(R.string.origin_fallback);
        }
    }

    private String buildMarkerSnippet(Gasolinera gasolinera) {
        StringBuilder sb = new StringBuilder();
        sb.append(gasolinera.getFormattedPrice(selectedFuel))
                .append(" · ")
                .append(gasolinera.getDisplayAddress());
        String horario = gasolinera.getFormattedHorario();
        if (!horario.equals("Horario no disponible")) {
            sb.append("\n").append(horario);
        }
        return sb.toString();
    }

    private int indexOfFuel(FuelType[] fuels, FuelType target) {
        for (int i = 0; i < fuels.length; i++) {
            if (fuels[i] == target) return i;
        }
        return 0;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private int dp(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }
}