package com.ahorragas.app;

import com.ahorragas.app.data.local.AppDatabase;
import com.ahorragas.app.databinding.ActivityMainBinding;
import com.ahorragas.app.detail.StationDetailActivity;
import android.Manifest;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.ahorragas.app.location.LocationHelper;
import com.ahorragas.app.map.MarkerBitmapFactory;
import com.ahorragas.app.model.FuelType;
import com.ahorragas.app.model.Gasolinera;
import com.ahorragas.app.model.PriceLevel;
import com.ahorragas.app.model.PriceRange;
import com.ahorragas.app.model.Vehicle;
import com.ahorragas.app.ui.BrandFilterDialog;
import com.ahorragas.app.ui.FirstVehicleDialog;
import com.ahorragas.app.ui.MapViewModel;
import com.ahorragas.app.util.DiscountPrefs;
import com.ahorragas.app.util.FavoritesPrefs;
import com.ahorragas.app.util.GasolineraSorter;
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
    private static final GeoPoint SPAIN_CENTER = new GeoPoint(40.4168, -3.7038);
    private static final double ZOOM_SPAIN = 6.0;
    private static final double ZOOM_USER = 14.0;

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
    private boolean vehicleDialogShown = false;

    private Location userLocation;
    private final Map<Integer, Marker> markerMap = new HashMap<>();
    private Marker myLocationMarker;

    private ActivityMainBinding binding;
    private MapViewModel viewModel;
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

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applySystemBarInsets(R.id.topBar, R.id.bottomNav);

        viewModel = new ViewModelProvider(this).get(MapViewModel.class);
        locationHelper = new LocationHelper(this);
        observeViewModel();
        setupBrandFilter();

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
                    if (timestamp == null) {
                        binding.tvLastSync.setText(getString(R.string.last_sync_never));
                    } else {
                        long millis = Long.parseLong(timestamp);
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                                "dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                        binding.tvLastSync.setText(getString(R.string.last_sync_format, sdf.format(new java.util.Date(millis))));
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
        if (savedFuel != viewModel.getSelectedFuel()) {
            MarkerBitmapFactory.clearCache();
            viewModel.changeFuel(savedFuel, userLocation);
            return;
        }

        String activeSearch = viewModel.getLastSearchQuery();

        int currentRadius = RadiusUtils.loadRadiusKm(this);
        int currentMarkers = RadiusUtils.loadMarkersCount(this);
        if (currentRadius != lastRadiusKm || currentMarkers != lastMarkersCount) {
            lastRadiusKm = currentRadius;
            lastMarkersCount = currentMarkers;
            if (activeSearch == null && userLocation != null) {
                viewModel.loadByRadius(userLocation.getLatitude(), userLocation.getLongitude());
            } else if (activeSearch != null) {
                viewModel.filterByMunicipio(activeSearch, userLocation);
            }
            return;
        }

        int currentDiscountsVersion = DiscountPrefs.getVersion(this);
        if (currentDiscountsVersion != lastDiscountsVersion) {
            lastDiscountsVersion = currentDiscountsVersion;
            MarkerBitmapFactory.clearCache();
            refreshActiveView(activeSearch);
        }
        int currentFavoritesVersion = FavoritesPrefs.getVersion(this);
        if (currentFavoritesVersion != lastFavoritesVersion) {
            lastFavoritesVersion = currentFavoritesVersion;
            MarkerBitmapFactory.clearCache();
            refreshActiveView(activeSearch);
        }
        if (activeSearch == null && userLocation != null) {
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

    /**
     * Suscribe la Activity a las salidas del ViewModel: barra de progreso,
     * órdenes de repintado (radio/búsqueda), centrado del mapa y toasts.
     */
    private void observeViewModel() {
        viewModel.getLoading().observe(this, isLoading ->
                progressBarSearch.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE));

        viewModel.getRadiusRender().observe(this, event -> {
            MapViewModel.RadiusOutcome outcome = event.getIfNotHandled();
            if (outcome != null) renderRadius(outcome);
        });

        viewModel.getSearchRender().observe(this, event -> {
            Boolean empty = event.getIfNotHandled();
            if (empty != null) renderSearch(empty);
        });

        viewModel.getCenterOnSearch().observe(this, event -> {
            double[] coords = event.getIfNotHandled();
            if (coords != null) centerMapOnSearch(coords[0], coords[1]);
        });

        viewModel.getToast().observe(this, event -> {
            Integer resId = event.getIfNotHandled();
            if (resId != null) Toast.makeText(this, getString(resId), Toast.LENGTH_SHORT).show();
        });
    }

    /** Pinta el resultado de una carga por radio según su desenlace. */
    private void renderRadius(MapViewModel.RadiusOutcome outcome) {
        switch (outcome) {
            case SHOW:
                showStationsOnMap(RadiusUtils.loadMarkersCount(this));
                break;
            case EMPTY_FUEL:
                clearMapMarkers();
                Toast.makeText(this,
                        getString(R.string.no_stations_for_fuel, viewModel.getSelectedFuel().displayName()),
                        Toast.LENGTH_SHORT).show();
                break;
            case NO_DATA:
            default:
                clearMapMarkers();
                break;
        }
    }

    /** Pinta los markers de una búsqueda por municipio. */
    private void renderSearch(boolean empty) {
        clearMapMarkers();
        List<Gasolinera> stations = viewModel.getSearchGasolineras();
        if (stations != null) {
            for (Gasolinera g : stations) {
                addMarker(g);
            }
        }
        mapView.invalidate();
        if (empty) {
            Toast.makeText(this,
                    getString(R.string.msg_no_stations_in_location), Toast.LENGTH_SHORT).show();
        }
    }

    /** Centra el mapa en las coordenadas de la búsqueda y oculta el teclado. */
    private void centerMapOnSearch(double lat, double lon) {
        clearMapMarkers();
        GeoPoint point = new GeoPoint(lat, lon);
        mapView.getController().animateTo(point);
        mapView.getController().setZoom(13.0);

        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager)
                        getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }
    }

    // ─── DIÁLOGO PRIMER VEHÍCULO ─────────────────────────────────────────────

    private void showFirstVehicleDialog() {
        FirstVehicleDialog.show(this, vehicle -> {
            VehiclePrefs.addVehicle(this, vehicle);
            viewModel.setSelectedFuel(vehicle.getFuelType());
            MarkerBitmapFactory.clearCache();
            if (userLocation != null) {
                viewModel.loadByRadius(userLocation.getLatitude(), userLocation.getLongitude());
            } else {
                viewModel.updateVisible();
            }
            vehicleDialogShown = false;
        });
    }

    // ─── VIEWS ───────────────────────────────────────────────────────────────

    private void initViews() {
        mapView = binding.mapView;
        fabMiUbicacion = binding.fabMiUbicacion;
        tvLastSync = binding.tvLastSync;
        progressBarSearch = binding.progressBarSearch;
        bottomNav = binding.bottomNav;
        etSearch = binding.etSearch;
        loadLastSync();
    }

    /**
     * Lee el timestamp de la última sincronización de gasolineras desde Room
     * en un hilo de fondo y lo muestra formateado en tvLastSync.
     */
    private void loadLastSync() {
        com.ahorragas.app.data.AppExecutors.io().execute(() -> {
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
                runOnUiThread(() -> tvLastSync.setText(text));
            } catch (Exception e) {
                runOnUiThread(() -> tvLastSync.setText(getString(R.string.label_last_sync_never)));
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
        setupBottomNav(bottomNav, R.id.nav_map, viewModel.getSelectedFuel());
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
                viewModel.updateVisible();
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

        viewModel.loadByRadius(smoothed.getLatitude(), smoothed.getLongitude());
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
        if (userLocation != null) {
            viewModel.loadByRadius(userLocation.getLatitude(), userLocation.getLongitude());
        } else {
            progressBarSearch.setVisibility(View.GONE);
        }
    }

    /**
     * Refresca la vista activa reusando los datos ya cargados: si hay una
     * búsqueda por municipio en curso la re-filtra, si no recalcula las visibles.
     */
    private void refreshActiveView(String activeSearch) {
        if (activeSearch != null) {
            viewModel.filterByMunicipio(activeSearch, userLocation);
        } else {
            viewModel.updateVisible();
        }
    }

    private void showStationsOnMap(int count) {
        // No se limpia la caché aquí a propósito: la clave del bitmap ya incluye
        // marca, nivel, precio (con descuento) y favorito, así que re-renderizar el
        // mapa (mover, zoom, filtrar) reutiliza los bitmaps en vez de recrearlos.
        // Los cambios de combustible/descuentos/favoritos ya invalidan en onResume.
        clearMapMarkers();
        // El filtro de marca ya viene aplicado en los datos (MapViewModel), aquí
        // solo se recorta al máximo de markers para pintar.
        final List<Gasolinera> visible = viewModel.getVisibleGasolineras();
        final List<Gasolinera> toRender = new ArrayList<>(
                visible.subList(0, Math.min(count, visible.size())));
        final FuelType fuelSnapshot = viewModel.getSelectedFuel();
        final PriceRange rangeSnapshot = viewModel.getCurrentPriceRange();

        com.ahorragas.app.data.AppExecutors.io().execute(() -> {
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

            runOnUiThread(() -> {
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

        FuelType fuel = viewModel.getSelectedFuel();
        Double originalPrice = gasolinera.getPrecio(fuel);
        String priceText;
        PriceLevel priceLevel;

        if (originalPrice != null && originalPrice > 0) {
            double discounted = DiscountPrefs.applyAllDiscounts(
                    this, gasolinera.getMarca(), originalPrice);
            priceText = String.format(java.util.Locale.getDefault(), "%.3f €", discounted);
            priceLevel = GasolineraSorter.getPriceLevel(discounted, viewModel.getCurrentPriceRange());
        } else {
            priceText = gasolinera.getFormattedPrice(fuel);
            priceLevel = gasolinera.getPriceLevel();
        }

        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(gasolinera.getLat(), gasolinera.getLon()));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setInfoWindow(null);

        marker.setIcon(new android.graphics.drawable.BitmapDrawable(
                getResources(),
                MarkerBitmapFactory.createMarker(this, gasolinera, fuel)
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

    private final com.ahorragas.app.util.LocationSmoother locationSmoother =
            new com.ahorragas.app.util.LocationSmoother();

    private Location getSmoothedLocation(Location raw) {
        double[] smoothedCoords = locationSmoother.add(raw.getLatitude(), raw.getLongitude());
        Location smoothed = new Location(raw);
        smoothed.setLatitude(smoothedCoords[0]);
        smoothed.setLongitude(smoothedCoords[1]);
        return smoothed;
    }

    private void showSpainFallback() {
        mapView.getController().setZoom(ZOOM_SPAIN);
        mapView.getController().setCenter(SPAIN_CENTER);
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
     * Lanza la búsqueda de una localidad: el ViewModel geocodifica con Nominatim,
     * pide centrar el mapa y filtra las gasolineras por municipio.
     *
     * @param query Nombre de la localidad a buscar.
     */
    private void searchLocalidad(String query) {
        viewModel.searchCity(query, userLocation);
    }

    private void setupFab() {
        fabMiUbicacion.setOnClickListener(v -> {
            if (userLocation != null) {
                viewModel.clearSearch();
                etSearch.setText("");
                GeoPoint point = new GeoPoint(
                        userLocation.getLatitude(),
                        userLocation.getLongitude()
                );
                mapView.getController().animateTo(point);
                mapView.getController().setZoom(ZOOM_USER);
                viewModel.loadByRadius(userLocation.getLatitude(), userLocation.getLongitude());
            } else {
                requestLocationPermission();
            }
        });
    }

    @Override
    protected void navigateToPrice() {
        Intent intent = new Intent(this, PriceListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        List<Gasolinera> search = viewModel.getSearchGasolineras();
        if (search != null && !search.isEmpty()) {
            intent.putParcelableArrayListExtra("gasolineras", new ArrayList<>(search));
        }
        startActivity(intent);
    }

    @Override
    protected void navigateToDistanceList() {
        Intent intent = new Intent(this, DistanceListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        List<Gasolinera> search = viewModel.getSearchGasolineras();
        if (search != null && !search.isEmpty()) {
            intent.putParcelableArrayListExtra("gasolineras", new ArrayList<>(search));
        }
        startActivity(intent);
    }

    private int dp(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }

    private void setupBrandFilter() {
        if (binding.brandFilterButton == null) return;
        updateBrandFilterButton();
        binding.brandFilterButton.setOnClickListener(v -> showBrandFilterDialog());
    }

    private void updateBrandFilterButton() {
        String selectedBrand = viewModel.getSelectedBrand();
        binding.tvBrandFilterLabel.setText(
                getString(R.string.brand_filter_label, BrandFilterDialog.displayName(this, selectedBrand)));
        if (selectedBrand == null) {
            binding.ivBrandFilterIcon.setVisibility(View.GONE);
        } else {
            binding.ivBrandFilterIcon.setImageResource(
                    com.ahorragas.app.map.BrandLogoProvider.getLogoResId(selectedBrand));
            binding.ivBrandFilterIcon.setVisibility(View.VISIBLE);
        }
    }

    private void showBrandFilterDialog() {
        BrandFilterDialog.show(this, viewModel.getSelectedBrand(), newBrand -> {
            viewModel.setSelectedBrand(newBrand);
            updateBrandFilterButton();
            // Cambiar de marca requiere volver a filtrar/recortar los datos
            // (el filtro se aplica antes del recorte al máximo de markers).
            viewModel.reloadForCurrentMode(userLocation);
        });
    }
}