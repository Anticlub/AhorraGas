package com.example.ahorragas;

import com.example.ahorragas.detail.StationDetailActivity;
import com.example.ahorragas.model.Discount;
import android.Manifest;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

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
import com.example.ahorragas.util.DiscountPrefs;
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
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends BaseActivity {

    private static final String PREF_SELECTED_FUEL = "pref_selected_fuel";
    private static final GeoPoint SPAIN_CENTER = new GeoPoint(40.4168, -3.7038);
    private static final double ZOOM_SPAIN = 6.0;
    private static final double ZOOM_USER = 14.0;
    private static final double ZOOM_STATION = 16.0;

    private MapView mapView;
    private FloatingActionButton fabMiUbicacion;
    private TextView tvDataStatus;
    private TextView tvLocation;
    private ProgressBar progressBarSearch;
    private EditText etSearch;
    private BottomNavigationView bottomNav;
    private int lastRadiusKm = RadiusUtils.DEFAULT_KM;
    private int lastMarkersCount = RadiusUtils.DEFAULT_MARKERS;

    private final List<Gasolinera> allGasolineras = new ArrayList<>();
    private List<Gasolinera> visibleGasolineras = new ArrayList<>();
    private FuelType selectedFuel = FuelType.GASOLEO_A;
    private Location userLocation;
    private final Map<Integer, Marker> markerMap = new HashMap<>();
    private MyLocationNewOverlay locationOverlay;

    private CachedRemoteApiDataSource dataSource;
    private GasolineraRepository repository;
    private LocationHelper locationHelper;
    private final java.util.concurrent.ExecutorService executor =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private final android.os.Handler mainHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());

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
        repository = GasolineraRepository.getInstance(dataSource);
        locationHelper = new LocationHelper(this);

        selectedFuel = FuelType.fromString(
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getString(PREF_SELECTED_FUEL, FuelType.GASOLEO_A.name())
        );

        initViews();
        setupMap();
        setupFab();
        setupSearch();
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

        if (!VehiclePrefs.hasVehicles(this)) {
            showFirstVehicleDialog();
            return;
        }

        FuelType savedFuel = FuelType.fromString(
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getString(PREF_SELECTED_FUEL, FuelType.GASOLEO_A.name())
        );
        if (savedFuel != selectedFuel) {
            selectedFuel = savedFuel;
            MarkerBitmapFactory.clearCache();
            updateDisplayForFuel(selectedFuel);
        }

        int currentRadius = RadiusUtils.loadRadiusKm(this);
        if (currentRadius != lastRadiusKm) {
            lastRadiusKm = currentRadius;
            updateDisplayForFuel(selectedFuel);
        }

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    // ─── DIÁLOGO PRIMER VEHÍCULO ─────────────────────────────────────────────

    private void showFirstVehicleDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(12), dp(20), dp(4));

        TextView labelName = new TextView(this);
        labelName.setText(getString(R.string.dialogo_vehiculo_nombre));
        labelName.setTextColor(0xFF333333);
        labelName.setTextSize(13);
        layout.addView(labelName);

        EditText etName = new EditText(this);
        etName.setHint(getString(R.string.dialogo_vehiculo_nombre_hint));
        etName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        layout.addView(etName);

        TextView labelCons = new TextView(this);
        labelCons.setText(getString(R.string.dialogo_vehiculo_consumo));
        labelCons.setTextColor(0xFF333333);
        labelCons.setTextSize(13);
        labelCons.setPadding(0, dp(12), 0, 0);
        layout.addView(labelCons);

        EditText etCons = new EditText(this);
        etCons.setHint(getString(R.string.dialogo_vehiculo_consumo_hint));
        etCons.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etCons);

        TextView labelTank = new TextView(this);
        labelTank.setText("Capacidad depósito (L)  · opcional");
        labelTank.setTextColor(0xFF333333);
        labelTank.setTextSize(13);
        labelTank.setPadding(0, dp(12), 0, 0);
        layout.addView(labelTank);

        EditText etTank = new EditText(this);
        etTank.setHint("Ej: 50  (entre 1 y 200)");
        etTank.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etTank);

        TextView labelFuel = new TextView(this);
        labelFuel.setText(getString(R.string.dialogo_vehiculo_combustible));
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
                    .setTitle(getString(R.string.dialogo_vehiculo_titulo))
                    .setSingleChoiceItems(fuelNames, checked, (d, which) -> {
                        selectedFuelLocal[0] = fuels[which];
                        tvFuelSelector.setText(selectedFuelLocal[0].displayName());
                        d.dismiss();
                    })
                    .setNegativeButton(getString(R.string.dialogo_vehiculo_cancelar), null)
                    .show();
        });
        layout.addView(tvFuelSelector);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialogo_vehiculo_titulo))
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.dialogo_vehiculo_guardar), null)
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String consStr = etCons.getText().toString().trim().replace(",", ".");
            String tankStr = etTank.getText().toString().trim().replace(",", ".");

            if (name.isEmpty()) {
                Toast.makeText(this, getString(R.string.dialogo_vehiculo_nombre_vacio), Toast.LENGTH_SHORT).show();
                return;
            }

            double cons;
            try {
                cons = Double.parseDouble(consStr);
                if (cons <= 0 || cons > 100) throw new NumberFormatException();
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.dialogo_vehiculo_consumo_invalido), Toast.LENGTH_SHORT).show();
                return;
            }

            double tank = 0.0;
            if (!tankStr.isEmpty()) {
                try {
                    tank = Double.parseDouble(tankStr);
                    if (tank <= 0 || tank > 200) throw new NumberFormatException();
                } catch (Exception e) {
                    Toast.makeText(this, "Capacidad no válida. Introduce un número entre 1 y 200.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            Vehicle vehicle = new Vehicle(name, selectedFuelLocal[0], cons, tank);
            VehiclePrefs.addVehicle(this, vehicle);

            selectedFuel = selectedFuelLocal[0];
            MarkerBitmapFactory.clearCache();
            updateDisplayForFuel(selectedFuel);

            dialog.dismiss();
        });
    }

    // ─── VIEWS ───────────────────────────────────────────────────────────────

    private void initViews() {
        mapView = findViewById(R.id.mapView);
        fabMiUbicacion = findViewById(R.id.fabMiUbicacion);
        tvDataStatus = findViewById(R.id.tvDataStatus);
        tvLocation = findViewById(R.id.tvLocation);
        progressBarSearch = findViewById(R.id.progressBarSearch);
        bottomNav = findViewById(R.id.bottomNav);
        etSearch = findViewById(R.id.etSearch);
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);
        showSpainFallback();

        mapView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (locationOverlay != null) {
                    locationOverlay.disableFollowLocation();
                }
            }
            return false;
        });
    }

    private void setupBottomNav() {
        setupBottomNav(bottomNav, R.id.nav_map);
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
        progressBarSearch.setVisibility(View.VISIBLE);

        tvDataStatus.setText(
                repository.getLastOrigin() == null
                        ? getString(R.string.status_loading_data)
                        : getString(R.string.status_refreshing_data)
        );

        executor.execute(() -> {
            try {
                List<Gasolinera> loaded = repository.getGasolineras();

                mainHandler.post(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    allGasolineras.clear();
                    allGasolineras.addAll(loaded);
                    progressBarSearch.setVisibility(View.GONE);
                    updateDisplayForFuel(selectedFuel);
                });

            } catch (RepoError error) {
                mainHandler.post(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    progressBarSearch.setVisibility(View.GONE);
                    tvDataStatus.setText(
                            getString(R.string.error_loading_data) + ": " + buildRepoErrorMessage(error)
                    );
                    if (allGasolineras.isEmpty()) {
                        clearMapMarkers();
                    }
                    Toast.makeText(MainActivity.this, buildRepoErrorMessage(error), Toast.LENGTH_LONG).show();
                });

            } catch (Exception error) {
                mainHandler.post(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    progressBarSearch.setVisibility(View.GONE);
                    tvDataStatus.setText(getString(R.string.error_loading_data) + ": " + error.getMessage());
                    Toast.makeText(MainActivity.this, getString(R.string.error_loading_data), Toast.LENGTH_LONG).show();
                });
            }
        });
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
        MarkerBitmapFactory.clearCache();
        clearMapMarkers();
        int toShow = Math.min(count, visibleGasolineras.size());
        for (int i = 0; i < toShow; i++) {
            addMarker(visibleGasolineras.get(i));
        }
        mapView.invalidate();
    }

    private void addMarker(Gasolinera gasolinera) {
        if (gasolinera.getLat() == null || gasolinera.getLon() == null) return;

        // Aplicar descuento al precio del marcador si existe
        Discount discount = DiscountPrefs.findForBrand(this, gasolinera.getMarca());
        Double originalPrice = gasolinera.getPrecio(selectedFuel);
        if (discount != null && originalPrice != null && originalPrice > 0) {
            gasolinera.setPrecio(selectedFuel, discount.applyTo(originalPrice));
        }

        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(gasolinera.getLat(), gasolinera.getLon()));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setInfoWindow(null);

        marker.setIcon(new android.graphics.drawable.BitmapDrawable(
                getResources(),
                MarkerBitmapFactory.createMarker(this, gasolinera, selectedFuel)
        ));

        marker.setOnMarkerClickListener((clickedMarker, ignoredMapView) -> {
            Intent intent = new Intent(this, StationDetailActivity.class);
            intent.putExtra(StationDetailActivity.EXTRA_GASOLINERA, gasolinera);
            startActivity(intent);
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

    private void focusOnGasolinera(Gasolinera gasolinera) {
        if (gasolinera.getLat() == null || gasolinera.getLon() == null) return;
        mapView.getController().animateTo(
                new GeoPoint(gasolinera.getLat(), gasolinera.getLon())
        );
        mapView.getController().setZoom(ZOOM_STATION);
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
            case NETWORK:        return getString(R.string.error_sin_conexion);
            case TIMEOUT:        return getString(R.string.error_api_timeout);
            case HTTP:           return "Error HTTP " + error.getHttpCode();
            case EMPTY_RESPONSE: return getString(R.string.error_respuesta_vacia);
            case PARSE:
            default:             return getString(R.string.error_procesar_gasolineras);
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
            case TIMEOUT:        return getString(R.string.error_ubicacion_timeout);
            case TECHNICAL_ERROR:
            default:             return getString(R.string.error_ubicacion_actual);
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

    private void setupSearch() {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchLocalidad(query);
                }
                return true;
            }
            return false;
        });
        etSearch.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etSearch.getRight()
                        - etSearch.getCompoundDrawables()[2].getBounds().width()
                        - etSearch.getPaddingEnd())) {
                    String query = etSearch.getText().toString().trim();
                    if (!query.isEmpty()) {
                        searchLocalidad(query);
                    }
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Geocodifica la localidad introducida usando Nominatim (OpenStreetMap),
     * centra el mapa en las coordenadas obtenidas y filtra los markers por municipio.
     *
     * @param query Nombre de la localidad a buscar.
     */
    private void searchLocalidad(String query) {
        progressBarSearch.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            try {
                String encoded = java.net.URLEncoder.encode(query, "UTF-8");
                String url = "https://nominatim.openstreetmap.org/search?city="
                        + encoded
                        + "&country=Spain&limit=1&format=json";

                java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                        new java.net.URL(url).openConnection();
                conn.setRequestProperty("User-Agent", getPackageName());
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                java.io.InputStream is = conn.getInputStream();
                String response = new java.util.Scanner(is).useDelimiter("\\A").next();
                conn.disconnect();

                int latStart = response.indexOf("\"lat\":\"") + 7;
                int latEnd   = response.indexOf("\"", latStart);
                int lonStart = response.indexOf("\"lon\":\"") + 7;
                int lonEnd   = response.indexOf("\"", lonStart);

                if (latStart < 7 || lonStart < 7) {
                    mainHandler.post(() -> {
                        progressBarSearch.setVisibility(View.GONE);
                        Toast.makeText(this,
                                "No se encontró la localidad", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                double lat = Double.parseDouble(response.substring(latStart, latEnd));
                double lon = Double.parseDouble(response.substring(lonStart, lonEnd));

                mainHandler.post(() -> {
                    progressBarSearch.setVisibility(View.GONE);

                    GeoPoint point = new GeoPoint(lat, lon);
                    mapView.getController().animateTo(point);
                    mapView.getController().setZoom(13.0);

                    filterMarkersByMunicipio(query);

                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager)
                                    getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    progressBarSearch.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Error al buscar la localidad", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Normaliza un texto eliminando tildes y pasándolo a minúsculas.
     *
     * @param text Texto a normalizar.
     * @return Texto normalizado.
     */
    private String normalize(String text) {
        if (text == null) return "";
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(java.util.Locale.getDefault());
    }

    /**
     * Comprueba si el municipio coincide con la búsqueda.
     *
     * @param normalizedMunicipio Municipio ya normalizado.
     * @param normalizedQuery     Búsqueda ya normalizada.
     * @return true si alguna parte del municipio coincide con la búsqueda.
     */
    private boolean matchesMunicipio(String normalizedMunicipio, String normalizedQuery) {
        if (normalizedMunicipio.equals(normalizedQuery)) return true;
        for (String part : normalizedMunicipio.split("/")) {
            if (part.trim().equals(normalizedQuery)) return true;
        }
        return false;
    }

    /**
     * Filtra los markers del mapa mostrando solo las gasolineras
     * cuyo municipio contiene el texto indicado.
     *
     * @param query Texto a buscar en el municipio.
     */
    private void filterMarkersByMunicipio(String query) {
        clearMapMarkers();
        String normalizedQuery = normalize(query);

        List<Gasolinera> filtered = new ArrayList<>();
        for (Gasolinera g : allGasolineras) {
            if (matchesMunicipio(normalize(g.getMunicipio()), normalizedQuery)
                    && g.hasPrice(selectedFuel)) {
                filtered.add(g);
            }
        }

        PriceRange range = GasolineraSorter.calculatePriceRange(filtered, selectedFuel);
        for (Gasolinera g : filtered) {
            g.setPriceLevel(GasolineraSorter.getPriceLevel(g.getPrecio(selectedFuel), range));
        }

        for (Gasolinera g : filtered) {
            addMarker(g);
        }
        mapView.invalidate();

        if (filtered.isEmpty()) {
            Toast.makeText(this,
                    "No hay gasolineras en esa localidad", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupFab() {
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

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private int dp(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }
}