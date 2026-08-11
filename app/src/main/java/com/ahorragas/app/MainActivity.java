package com.ahorragas.app;

import com.ahorragas.app.data.ElectrolineraRepository;
import com.ahorragas.app.data.EstacionRepository;
import com.ahorragas.app.data.remote.RemoteDgtDataSource;
import com.ahorragas.app.data.RoomElectrolineraDataSource;
import com.ahorragas.app.data.RoomGasolineraDataSource;
import com.ahorragas.app.data.local.AppDatabase;
import com.ahorragas.app.detail.StationDetailActivity;
import android.Manifest;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
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
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.ahorragas.app.data.DataSourceOrigin;
import com.ahorragas.app.data.GasolineraRepository;
import com.ahorragas.app.data.RepoError;
import com.ahorragas.app.location.LocationHelper;
import com.ahorragas.app.map.MarkerBitmapFactory;
import com.ahorragas.app.model.FuelType;
import com.ahorragas.app.model.Gasolinera;
import com.ahorragas.app.model.PriceLevel;
import com.ahorragas.app.model.PriceRange;
import com.ahorragas.app.model.Vehicle;
import com.ahorragas.app.util.DiscountPrefs;
import com.ahorragas.app.util.FavoritesPrefs;
import com.ahorragas.app.util.GasolineraSorter;
import com.ahorragas.app.util.GeoUtils;
import com.ahorragas.app.util.PriceAlertScheduler;
import com.ahorragas.app.util.RadiusUtils;
import com.ahorragas.app.util.VehiclePrefs;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.ahorragas.app.map.OsmTiles;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends BaseActivity {

    private static final String PREF_SELECTED_FUEL = "pref_selected_fuel";
    private static final String BRAND_ALL_FILTER_KEY = "__all__";
    private static final GeoPoint SPAIN_CENTER = new GeoPoint(40.4168, -3.7038);
    private static final double ZOOM_SPAIN = 6.0;
    private static final double ZOOM_USER = 14.0;
    private static final double ZOOM_STATION = 16.0;
    private static final java.util.regex.Pattern MUNICIPIO_ARTICLE_PATTERN =
            java.util.regex.Pattern.compile("^(.+?)\\s*\\(([^)]+)\\)$");

    private MapView mapView;
    private FloatingActionButton fabMiUbicacion;
    private TextView tvLastSync;
    private ProgressBar progressBarSearch;
    private EditText etSearch;
    private BottomNavigationView bottomNav;
    private int lastRadiusKm = RadiusUtils.DEFAULT_KM;
    private int lastMarkersCount = RadiusUtils.DEFAULT_MARKERS;
    private int lastDiscountsVersion = -1;
    private int lastFavoritesVersion = -1;
    private PriceRange currentPriceRange = new PriceRange(null, null, 0);
    private boolean vehicleDialogShown = false;

    private final List<Gasolinera> allGasolineras = new ArrayList<>();
    private List<Gasolinera> searchGasolineras = null;
    private String lastSearchQuery = null;
    private List<Gasolinera> visibleGasolineras = new ArrayList<>();
    private FuelType selectedFuel = FuelType.GASOLEO_A;
    private String selectedBrand = null;
    private Location userLocation;
    private Location searchLocation;
    private final Map<Integer, Marker> markerMap = new HashMap<>();
    private Marker myLocationMarker;

    private EstacionRepository repository;
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
                            Toast.makeText(
                                    this,
                                    R.string.location_permission_message,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // osmdroid se inicializa (User-Agent incluido) en AhorraGasApp.

        setContentView(R.layout.activity_main);
        applySystemBarInsets(R.id.topBar, R.id.bottomNav);
        setupBrandFilter();

        AppDatabase db = AppDatabase.getInstance(this);
        RoomGasolineraDataSource roomGasolineraDs = new RoomGasolineraDataSource(db);
        RoomElectrolineraDataSource roomElectrolineraDs = new RoomElectrolineraDataSource(db);
        GasolineraRepository gasolineraRepo = GasolineraRepository.getInstance(roomGasolineraDs);
        ElectrolineraRepository electrolineraRepo = ElectrolineraRepository.getInstance(
                new RemoteDgtDataSource(), roomElectrolineraDs);
        repository = EstacionRepository.getInstance(gasolineraRepo, electrolineraRepo);
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
        loadGasolineras();
        requestLocationPermission();
        PriceAlertScheduler.schedule(this);
        SyncWorker.schedule(this);
        AppDatabase.getInstance(this)
                .metadataDao()
                .observe("last_sync_gasolineras")
                .observe(this, timestamp -> {
                    TextView tvLastSync = findViewById(R.id.tvLastSync);
                    if (tvLastSync == null) return;
                    if (timestamp == null) {
                        tvLastSync.setText(getString(R.string.last_sync_never));
                    } else {
                        long millis = Long.parseLong(timestamp);
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                                "dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                        tvLastSync.setText(getString(R.string.last_sync_format, sdf.format(new java.util.Date(millis))));
                        loadGasolineras();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();

        FuelType currentNavFuel = FuelType.fromString(
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getString(PREF_SELECTED_FUEL, FuelType.GASOLEO_A.name()));
        bottomNav.getMenu().findItem(R.id.nav_price).setTitle(
                currentNavFuel == FuelType.ELECTRICO ? getString(R.string.label_by_power) : getString(R.string.label_by_price));
        bottomNav.getMenu().findItem(R.id.nav_price).setIcon(
                currentNavFuel == FuelType.ELECTRICO ? R.drawable.ic_bolt : R.drawable.ic_price);

        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_map);
        }

        if (!VehiclePrefs.hasVehicles(this) && !vehicleDialogShown) {
            vehicleDialogShown = true;
            showFirstVehicleDialog();
            return;
        }

        FuelType savedFuel = FuelType.fromString(
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getString(PREF_SELECTED_FUEL, FuelType.GASOLEO_A.name())
        );
        if (savedFuel != selectedFuel) {
            MarkerBitmapFactory.clearCache();
            updateDisplayForFuel(savedFuel);
            return;
        }

        int currentRadius = RadiusUtils.loadRadiusKm(this);
        int currentMarkers = RadiusUtils.loadMarkersCount(this);
        if (currentRadius != lastRadiusKm || currentMarkers != lastMarkersCount) {
            lastRadiusKm = currentRadius;
            lastMarkersCount = currentMarkers;
            if (lastSearchQuery == null && userLocation != null) {
                loadByRadius(userLocation.getLatitude(), userLocation.getLongitude());
            } else if (lastSearchQuery != null) {
                filterMarkersByMunicipio(lastSearchQuery);
            }
            return;
        }

        int currentDiscountsVersion = DiscountPrefs.getVersion(this);
        if (currentDiscountsVersion != lastDiscountsVersion) {
            lastDiscountsVersion = currentDiscountsVersion;
            MarkerBitmapFactory.clearCache();
            if (lastSearchQuery != null) {
                filterMarkersByMunicipio(lastSearchQuery);
            } else {
                updateDisplayForFuel(selectedFuel);
            }
        }
        int currentFavoritesVersion = FavoritesPrefs.getVersion(this);
        if (currentFavoritesVersion != lastFavoritesVersion) {
            lastFavoritesVersion = currentFavoritesVersion;
            MarkerBitmapFactory.clearCache();
            if (lastSearchQuery != null) {
                filterMarkersByMunicipio(lastSearchQuery);
            } else {
                updateDisplayForFuel(selectedFuel);
            }
        }
        if (lastSearchQuery == null && userLocation != null) {
            locationHelper.getUserLocation(new LocationHelper.ResultCallback() {
                @Override
                public void onSuccess(Location newLocation) {
                    float distance = newLocation.distanceTo(userLocation);
                    if (distance > 500) {
                        applyUserLocation(newLocation);
                    }
                }

                @Override
                public void onError(LocationHelper.LocationError error) {
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    // ─── MÉTODO AUXILIAR ─────────────────────────────────────────────────────

    /**
     * Aplica color rojo al último carácter de un string (el asterisco *)
     * para indicar que el campo es obligatorio.
     *
     * @param text Texto del label que termina en *
     * @return SpannableString con el asterisco en rojo
     */
    private SpannableString makeRequiredLabel(String text) {
        SpannableString span = new SpannableString(text);
        span.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(this, R.color.error_red)),
                span.length() - 1,
                span.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return span;
    }

    // ─── DIÁLOGO PRIMER VEHÍCULO ─────────────────────────────────────────────

    private void showFirstVehicleDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(12), dp(20), dp(4));

        TextView labelName = new TextView(this);
        labelName.setText(makeRequiredLabel(getString(R.string.label_vehicle_name)));
        labelName.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        labelName.setTextSize(13);
        layout.addView(labelName);

        EditText etName = new EditText(this);
        etName.setHint(getString(R.string.dialogo_vehiculo_nombre_hint));
        etName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        layout.addView(etName);

        TextView labelCons = new TextView(this);
        labelCons.setText(getString(R.string.dialogo_vehiculo_consumo));
        labelCons.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        labelCons.setTextSize(13);
        labelCons.setPadding(0, dp(12), 0, 0);
        layout.addView(labelCons);

        EditText etCons = new EditText(this);
        etCons.setHint(getString(R.string.dialogo_vehiculo_consumo_hint));
        etCons.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etCons);

        TextView labelTank = new TextView(this);
        labelTank.setText(getString(R.string.label_tank_capacity));
        labelTank.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        labelTank.setTextSize(13);
        labelTank.setPadding(0, dp(12), 0, 0);
        layout.addView(labelTank);

        EditText etTank = new EditText(this);
        etTank.setHint(getString(R.string.hint_tank_capacity));
        etTank.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etTank);

        TextView labelCharging = new TextView(this);
        labelCharging.setText(getString(R.string.label_charging_power));
        labelCharging.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        labelCharging.setTextSize(13);
        labelCharging.setPadding(0, dp(12), 0, 0);
        labelCharging.setVisibility(View.GONE);
        layout.addView(labelCharging);

        EditText etCharging = new EditText(this);
        etCharging.setHint(getString(R.string.hint_charging_power));
        etCharging.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etCharging.setVisibility(View.GONE);
        layout.addView(etCharging);

        TextView labelFuel = new TextView(this);
        labelFuel.setText(makeRequiredLabel(getString(R.string.label_fuel_type)));
        labelFuel.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        labelFuel.setTextSize(13);
        labelFuel.setPadding(0, dp(12), 0, 0);
        layout.addView(labelFuel);

        FuelType[] fuels = FuelType.values();
        String[] fuelNames = new String[fuels.length];
        for (int i = 0; i < fuels.length; i++) fuelNames[i] = fuels[i].displayName();

        final FuelType[] selectedFuelLocal = {FuelType.GASOLEO_A};

        TextView tvFuelSelector = new TextView(this);
        tvFuelSelector.setText(selectedFuelLocal[0].displayName());
        tvFuelSelector.setTextColor(ContextCompat.getColor(this, R.color.black));
        tvFuelSelector.setBackgroundColor(ContextCompat.getColor(this, R.color.input_light));
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
                    .setTitle(getString(R.string.dialogo_vehiculo_combustible_titulo))
                    .setSingleChoiceItems(fuelNames, checked, (d, which) -> {
                        selectedFuelLocal[0] = fuels[which];
                        tvFuelSelector.setText(selectedFuelLocal[0].displayName());
                        boolean isEv = (selectedFuelLocal[0] == FuelType.ELECTRICO);
                        labelCons.setText(isEv
                                ? getString(R.string.label_consumption_ev)
                                : getString(R.string.dialogo_vehiculo_consumo));
                        etCons.setHint(isEv
                                ? getString(R.string.hint_consumption_ev)
                                : getString(R.string.dialogo_vehiculo_consumo_hint));
                        labelTank.setText(isEv
                                ? getString(R.string.label_battery_capacity)
                                : getString(R.string.label_tank_capacity));
                        etTank.setHint(isEv
                                ? getString(R.string.hint_battery_capacity)
                                : getString(R.string.hint_tank_capacity));
                        labelCharging.setVisibility(isEv ? View.VISIBLE : View.GONE);
                        etCharging.setVisibility(isEv ? View.VISIBLE : View.GONE);
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
            String chargingStr = etCharging.getText().toString().trim().replace(",", ".");

            if (name.isEmpty()) {
                Toast.makeText(this, getString(R.string.dialogo_vehiculo_nombre_vacio), Toast.LENGTH_SHORT).show();
                return;
            }

            double cons = 0.0;
            if (!consStr.isEmpty()) {
                try {
                    cons = Double.parseDouble(consStr);
                    if (cons <= 0 || cons > 100) throw new NumberFormatException();
                } catch (Exception e) {
                    Toast.makeText(this, getString(R.string.dialogo_vehiculo_consumo_invalido), Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            double tank = 0.0;
            if (!tankStr.isEmpty()) {
                try {
                    tank = Double.parseDouble(tankStr);
                    if (tank <= 0 || tank > 200) throw new NumberFormatException();
                } catch (Exception e) {
                    Toast.makeText(this, getString(R.string.msg_invalid_tank_capacity), Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            double charging = 0.0;
            if (!chargingStr.isEmpty()) {
                try {
                    charging = Double.parseDouble(chargingStr);
                    if (charging <= 0 || charging > 500) throw new NumberFormatException();
                } catch (Exception e) {
                    Toast.makeText(this, getString(R.string.msg_invalid_charging_power), Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            Vehicle vehicle = new Vehicle(name, selectedFuelLocal[0], cons, tank, charging);
            VehiclePrefs.addVehicle(this, vehicle);

            selectedFuel = selectedFuelLocal[0];
            MarkerBitmapFactory.clearCache();
            if (userLocation != null) {
                loadByRadius(userLocation.getLatitude(), userLocation.getLongitude());
            } else {
                updateDisplayForFuel(selectedFuel);
            }
            vehicleDialogShown = false;
            dialog.dismiss();
        });
    }

    // ─── VIEWS ───────────────────────────────────────────────────────────────

    private void initViews() {
        mapView = findViewById(R.id.mapView);
        fabMiUbicacion = findViewById(R.id.fabMiUbicacion);
        tvLastSync = findViewById(R.id.tvLastSync);
        progressBarSearch = findViewById(R.id.progressBarSearch);
        bottomNav = findViewById(R.id.bottomNav);
        etSearch = findViewById(R.id.etSearch);
        loadLastSync();
    }

    /**
     * Lee el timestamp de la última sincronización de gasolineras desde Room
     * en un hilo de fondo y lo muestra formateado en tvLastSync.
     */
    private void loadLastSync() {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                String raw = db.metadataDao().get("last_sync_gasolineras");
                String text;
                if (raw != null) {
                    long millis = Long.parseLong(raw);
                    String formatted = new SimpleDateFormat(
                            "dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(millis));
                    text = getString(R.string.label_last_sync_format, formatted);
                } else {
                    text = getString(R.string.label_last_sync_never);
                }
                mainHandler.post(() -> tvLastSync.setText(text));
            } catch (Exception e) {
                mainHandler.post(() -> tvLastSync.setText(getString(R.string.label_last_sync_never)));
            }
        });
    }

    private void setupMap() {
        mapView.setTileSource(OsmTiles.OPENSTREETMAP);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        showSpainFallback();

        mapView.setOnTouchListener((v, event) -> false);
    }

    private void setupBottomNav() {
        setupBottomNav(bottomNav, R.id.nav_map, selectedFuel);
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
            Toast.makeText(this, R.string.location_gps_message, Toast.LENGTH_LONG).show();
            return;
        }

        locationHelper.getUserLocation(new LocationHelper.ResultCallback() {
            @Override
            public void onSuccess(Location location) {
                applyUserLocation(location);
            }

            @Override
            public void onError(LocationHelper.LocationError error) {
                showSpainFallback();
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
        Location smoothed = getSmoothedLocation(location);
        userLocation = smoothed;
        updateMyLocationMarker(smoothed);

        GeoPoint point = new GeoPoint(smoothed.getLatitude(), smoothed.getLongitude());
        mapView.getController().animateTo(point);
        mapView.getController().setZoom(ZOOM_USER);

        loadByRadius(smoothed.getLatitude(), smoothed.getLongitude());
    }

    private void updateMyLocationMarker(Location location) {
        GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
        if (myLocationMarker == null) {
            myLocationMarker = new Marker(mapView);
            myLocationMarker.setAnchor(0.5f, 0.5f);
            myLocationMarker.setInfoWindow(null);
            myLocationMarker.setIcon(new android.graphics.drawable.BitmapDrawable(
                    getResources(),
                    tintedLocationBitmap(ContextCompat.getColor(this, R.color.secondary))
            ));
            mapView.getOverlays().add(0, myLocationMarker);
        }
        myLocationMarker.setPosition(point);
        mapView.invalidate();
    }

    private void loadGasolineras() {
        progressBarSearch.setVisibility(View.VISIBLE);

        if (userLocation != null) {
            loadByRadius(userLocation.getLatitude(), userLocation.getLongitude());
        } else {
            progressBarSearch.setVisibility(View.GONE);
        }
    }

    private void loadByRadius(double lat, double lon) {
        progressBarSearch.setVisibility(View.VISIBLE);
        int radiusKm = RadiusUtils.loadRadiusKm(this);
        double radiusMeters = RadiusUtils.kmToMetersClamped(radiusKm);

        executor.execute(() -> {
            try {
                List<Gasolinera> result = new ArrayList<>();
                if (selectedFuel == FuelType.ELECTRICO) {
                    result.addAll(repository.getElectrolinerasByRadius(lat, lon, radiusMeters));
                } else {
                    result.addAll(repository.getGasolinerasByRadius(lat, lon, radiusMeters));
                }

                for (Gasolinera g : result) {
                    g.setDistanceMeters(GeoUtils.distanceMeters(
                            lat, lon, g.getLat(), g.getLon()));
                }

                result.sort(java.util.Comparator.comparingDouble(g ->
                        g.getDistanceMeters() == null ? Double.MAX_VALUE : g.getDistanceMeters()));

                int maxMarkers = RadiusUtils.loadMarkersCount(this);
                if (result.size() > maxMarkers) {
                    result = result.subList(0, maxMarkers);
                }

                final List<Gasolinera> toShow = new ArrayList<>(result);

                mainHandler.post(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    allGasolineras.clear();
                    allGasolineras.addAll(toShow);
                    progressBarSearch.setVisibility(View.GONE);
                    updateDisplayForFuel(selectedFuel);
                });

            } catch (RepoError e) {
                mainHandler.post(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    progressBarSearch.setVisibility(View.GONE);
                });
            }
        });
    }

    private void updateDisplayForFuel(FuelType fuel) {
        FuelType previous = selectedFuel;
        selectedFuel = fuel != null ? fuel : FuelType.GASOLEO_A;

        boolean fuelTypeChanged = (previous == FuelType.ELECTRICO) != (selectedFuel == FuelType.ELECTRICO);
        if (fuelTypeChanged && lastSearchQuery == null) {
            Location ref = userLocation;
            if (ref != null) {
                MarkerBitmapFactory.clearCache();
                loadByRadius(ref.getLatitude(), ref.getLongitude());
                return;
            }
        }

        visibleGasolineras = buildVisibleGasolineras(selectedFuel);

        currentPriceRange = GasolineraSorter.calculatePriceRange(visibleGasolineras, selectedFuel);
        for (Gasolinera gasolinera : visibleGasolineras) {
            gasolinera.setPriceLevel(
                    GasolineraSorter.getPriceLevel(
                            gasolinera.getPrecio(selectedFuel),
                            currentPriceRange
                    )
            );
        }

        if (allGasolineras.isEmpty()) {
            clearMapMarkers();
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
    }

    private List<Gasolinera> buildVisibleGasolineras(FuelType fuel) {
        List<Gasolinera> result = new ArrayList<>();
        for (Gasolinera g : allGasolineras) {
            if (fuel == FuelType.ELECTRICO && g.isElectric()) result.add(g);
            else if (fuel != FuelType.ELECTRICO && !g.isElectric() && g.hasPrice(fuel)) result.add(g);
        }
        return result;
    }

    private void showStationsOnMap(int count) {
        // No se limpia la caché aquí a propósito: la clave del bitmap ya incluye
        // marca, nivel, precio (con descuento) y favorito, así que re-renderizar el
        // mapa (mover, zoom, filtrar) reutiliza los bitmaps en vez de recrearlos.
        // Los cambios de combustible/descuentos/favoritos ya invalidan en onResume.
        clearMapMarkers();
        final List<Gasolinera> toRender = applyBrandFilter(new ArrayList<>(
                visibleGasolineras.subList(0, Math.min(count, visibleGasolineras.size()))
        ));
        final FuelType fuelSnapshot = selectedFuel;
        final PriceRange rangeSnapshot = currentPriceRange;

        executor.execute(() -> {
            final java.util.LinkedHashMap<Gasolinera, android.graphics.Bitmap> bitmaps =
                    new java.util.LinkedHashMap<>();
            for (Gasolinera g : toRender) {
                if (g.getLat() == null || g.getLon() == null) continue;
                if (!g.isElectric() && g.getPrecio(fuelSnapshot) != null
                        && g.getPrecio(fuelSnapshot) > 0) {
                    double discounted = DiscountPrefs.applyAllDiscounts(
                            getApplicationContext(), g.getMarca(), g.getPrecio(fuelSnapshot));
                    PriceLevel level = GasolineraSorter.getPriceLevel(discounted, rangeSnapshot);
                    g.setPriceLevel(level);
                    String priceText = String.format(java.util.Locale.getDefault(),
                            "%.3f €", discounted);
                    bitmaps.put(g, MarkerBitmapFactory.createMarker(
                            getApplicationContext(), g, fuelSnapshot, priceText, level));
                } else {
                    bitmaps.put(g, MarkerBitmapFactory.createMarker(
                            getApplicationContext(), g, fuelSnapshot));
                }
            }

            mainHandler.post(() -> {
                if (isDestroyed() || isFinishing()) return;
                clearMapMarkers();
                for (java.util.Map.Entry<Gasolinera, android.graphics.Bitmap> entry
                        : bitmaps.entrySet()) {
                    Gasolinera g = entry.getKey();
                    Marker marker = new Marker(mapView);
                    marker.setPosition(new GeoPoint(g.getLat(), g.getLon()));
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    marker.setInfoWindow(null);
                    marker.setIcon(new android.graphics.drawable.BitmapDrawable(
                            getResources(), entry.getValue()));
                    marker.setOnMarkerClickListener((m, mv) -> {
                        Intent intent = new Intent(this, StationDetailActivity.class);
                        intent.putExtra(StationDetailActivity.EXTRA_GASOLINERA, g);
                        startActivity(intent);
                        return true;
                    });
                    mapView.getOverlays().add(marker);
                    markerMap.put(g.getId(), marker);
                }
                mapView.invalidate();
            });
        });
    }

    private void addMarker(Gasolinera gasolinera) {
        if (gasolinera.getLat() == null || gasolinera.getLon() == null) return;

        Double originalPrice = gasolinera.getPrecio(selectedFuel);
        String priceText;
        PriceLevel priceLevel;

        if (originalPrice != null && originalPrice > 0) {
            double discounted = DiscountPrefs.applyAllDiscounts(
                    this, gasolinera.getMarca(), originalPrice);
            priceText = String.format(java.util.Locale.getDefault(), "%.3f €", discounted);
            priceLevel = GasolineraSorter.getPriceLevel(discounted, currentPriceRange);
        } else {
            priceText = gasolinera.getFormattedPrice(selectedFuel);
            priceLevel = gasolinera.getPriceLevel();
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
            if (overlay instanceof Marker && overlay != myLocationMarker) {
                toRemove.add(overlay);
            }
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

    /**
     * Crea un bitmap del icono de ubicación teñido con el color indicado.
     *
     * @param color Color en formato ARGB
     * @return Bitmap teñido
     */
    private android.graphics.Bitmap tintedLocationBitmap(int color) {
        int size = dp(32);
        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(
                size, size, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(size / 2f, 0);
        path.lineTo(size, size);
        path.lineTo(size / 2f, size * 0.75f);
        path.lineTo(0, size);
        path.close();
        canvas.drawPath(path, paint);
        return bmp;
    }

    private static final int LOCATION_FILTER_SIZE = 5;
    private final java.util.ArrayDeque<double[]> locationFilterBuffer = new java.util.ArrayDeque<>();

    private Location getSmoothedLocation(Location raw) {
        locationFilterBuffer.addLast(new double[]{raw.getLatitude(), raw.getLongitude()});
        if (locationFilterBuffer.size() > LOCATION_FILTER_SIZE) {
            locationFilterBuffer.removeFirst();
        }
        double avgLat = 0, avgLon = 0;
        for (double[] point : locationFilterBuffer) {
            avgLat += point[0];
            avgLon += point[1];
        }
        avgLat /= locationFilterBuffer.size();
        avgLon /= locationFilterBuffer.size();
        Location smoothed = new Location(raw);
        smoothed.setLatitude(avgLat);
        smoothed.setLongitude(avgLon);
        return smoothed;
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
                double[] coords = com.ahorragas.app.data.repository.GeocodingRepository
                        .getInstance().geocodeCity(query);

                if (coords == null) {
                    mainHandler.post(() -> {
                        progressBarSearch.setVisibility(View.GONE);
                        Toast.makeText(this,
                                getString(R.string.msg_location_not_found), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                double lat = coords[0];
                double lon = coords[1];

                mainHandler.post(() -> {
                    GeoPoint point = new GeoPoint(lat, lon);
                    mapView.getController().animateTo(point);
                    mapView.getController().setZoom(13.0);
                    searchLocation = new Location("search");
                    searchLocation.setLatitude(lat);
                    searchLocation.setLongitude(lon);

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
                            getString(R.string.msg_error_searching_location), Toast.LENGTH_SHORT).show();
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
     * Filtra los markers del mapa usando el índice pre-calculado de municipios.
     *
     * @param query Texto a buscar en el municipio.
     */
    private void filterMarkersByMunicipio(String query) {
        lastSearchQuery = query;
        clearMapMarkers();
        progressBarSearch.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            try {
                String normalizedQuery = normalizeMunicipioQuery(query);

                List<Gasolinera> result = new ArrayList<>();
                if (selectedFuel == FuelType.ELECTRICO) {
                    result.addAll(repository.getElectrolinerasByMunicipio(query));
                    if (result.isEmpty()) {
                        result.addAll(repository.getElectrolinerasByMunicipio(
                                normalizeMunicipioQuery(query)));
                    }
                    if (result.isEmpty()) {
                        for (String variant : buildInvertedQueries(query)) {
                            result.addAll(repository.getElectrolinerasByMunicipio(variant));
                            if (!result.isEmpty()) break;
                        }
                    }
                } else {
                    result.addAll(repository.getGasolinerasByMunicipio(query));
                    if (result.isEmpty()) {
                        result.addAll(repository.getGasolinerasByMunicipio(
                                normalizeMunicipioQuery(query)));
                    }
                    if (result.isEmpty()) {
                        for (String variant : buildInvertedQueries(query)) {
                            result.addAll(repository.getGasolinerasByMunicipio(variant));
                            if (!result.isEmpty()) break;
                        }
                    }
                }
                Location ref = searchLocation != null ? searchLocation : userLocation;
                if (ref != null) {
                    for (Gasolinera g : result) {
                        g.setDistanceMeters(GeoUtils.distanceMeters(
                                ref.getLatitude(), ref.getLongitude(),
                                g.getLat(), g.getLon()));
                    }
                    result.sort(java.util.Comparator.comparingDouble(g ->
                            g.getDistanceMeters() == null
                                    ? Double.MAX_VALUE : g.getDistanceMeters()));
                }

                final List<Gasolinera> filtered = GasolineraSorter.filterByFuel(result, selectedFuel);
                currentPriceRange = GasolineraSorter.calculatePriceRange(filtered, selectedFuel);
                for (Gasolinera g : filtered) {
                    g.setPriceLevel(GasolineraSorter.getPriceLevel(
                            g.getPrecio(selectedFuel), currentPriceRange));
                }

                mainHandler.post(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    searchGasolineras = filtered;
                    allGasolineras.clear();
                    allGasolineras.addAll(filtered);
                    progressBarSearch.setVisibility(View.GONE);
                    for (Gasolinera g : filtered) {
                        addMarker(g);
                    }
                    mapView.invalidate();

                    if (filtered.isEmpty()) {
                        Toast.makeText(this,
                                getString(R.string.msg_no_stations_in_location), Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (RepoError e) {
                mainHandler.post(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    progressBarSearch.setVisibility(View.GONE);
                    Toast.makeText(this,
                            getString(R.string.msg_error_searching_stations), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setupFab() {
        fabMiUbicacion.setOnClickListener(v -> {
            if (userLocation != null) {
                searchLocation = null;
                searchGasolineras = null;
                lastSearchQuery = null;
                etSearch.setText("");
                GeoPoint point = new GeoPoint(
                        userLocation.getLatitude(),
                        userLocation.getLongitude()
                );
                mapView.getController().animateTo(point);
                mapView.getController().setZoom(ZOOM_USER);
                loadByRadius(userLocation.getLatitude(), userLocation.getLongitude());
            } else {
                requestLocationPermission();
            }
        });
    }

    @Override
    protected void navigateToPrice() {
        Intent intent = new Intent(this, PriceListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (searchGasolineras != null && !searchGasolineras.isEmpty()) {
            intent.putParcelableArrayListExtra("gasolineras", new ArrayList<>(searchGasolineras));
        }
        startActivity(intent);
    }

    @Override
    protected void navigateToDistanceList() {
        Intent intent = new Intent(this, DistanceListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (searchGasolineras != null && !searchGasolineras.isEmpty()) {
            intent.putParcelableArrayListExtra("gasolineras", new ArrayList<>(searchGasolineras));
        }
        startActivity(intent);
    }

    private int dp(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }

    /**
     * Normaliza el nombre de un municipio para la búsqueda en Room.
     *
     * @param query texto introducido por el usuario
     * @return palabra principal del municipio para buscar en Room
     */
    private String normalizeMunicipioQuery(String query) {
        if (query == null) return "";
        String[] articles = {"el ", "la ", "los ", "las ", "de ", "del "};
        String lower = query.trim().toLowerCase(java.util.Locale.getDefault());
        for (String article : articles) {
            if (lower.startsWith(article)) {
                return query.trim().substring(article.length()).trim();
            }
        }
        return query.trim();
    }

    /**
     * Genera variantes invertidas de un municipio con artículo para buscar en Room.
     *
     * @param query texto introducido por el usuario
     * @return lista de variantes o lista vacía si no empieza por artículo
     */
    private List<String> buildInvertedQueries(String query) {
        List<String> variants = new ArrayList<>();
        if (query == null) return variants;
        String[] articles = {"El ", "La ", "Los ", "Las ", "A "};
        String trimmed = query.trim();
        for (String article : articles) {
            if (trimmed.startsWith(article)) {
                String rest = trimmed.substring(article.length()).trim();
                variants.add(rest + " (" + article.trim() + ")");
                variants.add(rest + ", " + article.trim());
                return variants;
            }
        }
        return variants;
    }

    private void setupBrandFilter() {
        View button = findViewById(R.id.brandFilterButton);
        if (button == null) return;
        selectedBrand = com.ahorragas.app.util.BrandPrefs.get(this);
        updateBrandFilterButton();
        button.setOnClickListener(v -> showBrandFilterDialog());
    }

    private void updateBrandFilterButton() {
        TextView label = findViewById(R.id.tvBrandFilterLabel);
        android.widget.ImageView icon = findViewById(R.id.ivBrandFilterIcon);
        if (label != null) {
            label.setText(getString(R.string.brand_filter_label, displayBrandName(selectedBrand)));
        }
        if (icon != null) {
            if (selectedBrand == null) {
                icon.setVisibility(View.GONE);
            } else {
                icon.setImageResource(
                        com.ahorragas.app.map.BrandLogoProvider.getLogoResId(selectedBrand));
                icon.setVisibility(View.VISIBLE);
            }
        }
    }

    private String displayBrandName(String brandKey) {
        if (brandKey == null || BRAND_ALL_FILTER_KEY.equals(brandKey)) {
            return getString(R.string.brand_all);
        }
        if ("bp".equalsIgnoreCase(brandKey)) return "BP";
        if ("glp".equalsIgnoreCase(brandKey)) return "GLP";
        return Character.toUpperCase(brandKey.charAt(0)) + brandKey.substring(1).toLowerCase();
    }

    private void showBrandFilterDialog() {
        java.util.List<String> keys = new ArrayList<>();
        keys.add(BRAND_ALL_FILTER_KEY);
        keys.addAll(com.ahorragas.app.map.BrandLogoProvider.FILTER_BRANDS);

        String[] labels = new String[keys.size()];
        int currentIndex = 0;
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            labels[i] = displayBrandName(key);
            if ((BRAND_ALL_FILTER_KEY.equals(key) && selectedBrand == null)
                    || (selectedBrand != null && key.equalsIgnoreCase(selectedBrand))) {
                currentIndex = i;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.brand_filter_dialog_title)
                .setSingleChoiceItems(labels, currentIndex, (dialog, which) -> {
                    String selectedKey = keys.get(which);
                    String newBrand = BRAND_ALL_FILTER_KEY.equals(selectedKey) ? null : selectedKey;
                    selectedBrand = newBrand;
                    com.ahorragas.app.util.BrandPrefs.set(this, newBrand);
                    updateBrandFilterButton();
                    showStationsOnMap(lastMarkersCount);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private List<Gasolinera> applyBrandFilter(List<Gasolinera> input) {
        if (selectedBrand == null || BRAND_ALL_FILTER_KEY.equals(selectedBrand)) return input;
        List<Gasolinera> out = new ArrayList<>();
        for (Gasolinera g : input) {
            String marca = g.getMarca();
            if (marca != null && marca.toLowerCase().contains(selectedBrand)) {
                out.add(g);
            }
        }
        return out;
    }
}