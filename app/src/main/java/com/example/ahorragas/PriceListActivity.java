package com.example.ahorragas;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ahorragas.adapter.GasolineraAdapter;
import com.example.ahorragas.data.CachedRemoteApiDataSource;
import com.example.ahorragas.data.GasolineraRepository;
import com.example.ahorragas.location.LocationHelper;
import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Gasolinera;
import com.example.ahorragas.model.PriceRange;
import com.example.ahorragas.util.DiscountPrefs;
import com.example.ahorragas.util.GasolineraSorter;
import com.example.ahorragas.util.RadiusUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PriceListActivity extends BaseActivity {

    private GasolineraRepository repository;
    private GasolineraAdapter adapter;
    private FuelType selectedFuel;
    private LocationHelper locationHelper;

    private boolean dataLoaded = false;
    private FuelType lastLoadedFuel = null;
    private List<Gasolinera> lastLoadedGasolineras = null;

    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private TextView tvError;
    private View layoutError;
    private Button btnRetry;
    private final java.util.concurrent.ExecutorService executor =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private final android.os.Handler mainHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_price_list);

        repository = GasolineraRepository.getInstance(new CachedRemoteApiDataSource(this));
        locationHelper = new LocationHelper(this);

        bindViews();
        setupRecyclerView();
        setupBottomNav();
        btnRetry.setOnClickListener(v -> loadAndDisplay());
    }

    @Override
    protected void onResume() {
        super.onResume();
        FuelType currentFuel = FuelType.fromString(
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getString("pref_selected_fuel", FuelType.GASOLEO_A.name())
        );
        if (!dataLoaded || currentFuel != lastLoadedFuel) {
            selectedFuel = currentFuel;
            lastLoadedFuel = currentFuel;
            loadAndDisplay();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        dataLoaded = false;
        loadAndDisplay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void bindViews() {
        progressBar  = findViewById(R.id.progressBarPrice);
        recyclerView = findViewById(R.id.recyclerViewPrice);
        tvEmpty      = findViewById(R.id.tvEmptyPrice);
        layoutError  = findViewById(R.id.layoutErrorPrice);
        tvError      = findViewById(R.id.tvErrorPrice);
        btnRetry     = findViewById(R.id.btnRetryPrice);
    }

    private void setupRecyclerView() {
        adapter = new GasolineraAdapter(
                new ArrayList<>(),
                selectedFuel,
                gasolinera -> navigateToDetail(gasolinera)
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavPrice);
        setupBottomNav(bottomNav, R.id.nav_price);
    }

    /**
     * Carga y muestra las gasolineras. Si vienen en el Intent las usa
     * directamente, si no las obtiene por GPS y radio.
     */
    private void loadAndDisplay() {
        showLoading();

        ArrayList<Gasolinera> fromIntent = getIntent().getParcelableArrayListExtra("gasolineras");
        if (fromIntent != null && !fromIntent.isEmpty()) {
            loadFromIntent(fromIntent);
        } else {
            locationHelper.getUserLocation(new LocationHelper.ResultCallback() {
                @Override
                public void onSuccess(Location location) {
                    loadWithCoordinates(location.getLatitude(), location.getLongitude());
                }

                @Override
                public void onError(LocationHelper.LocationError error) {
                    runOnUiThread(() ->
                            showError(getString(R.string.error_ubicacion)));
                }
            });
        }
    }

    /**
     * Usa las gasolineras recibidas desde el Intent, las ordena por precio descontado.
     *
     * @param gasolineras Lista de gasolineras del municipio buscado.
     */
    private void loadFromIntent(List<Gasolinera> gasolineras) {
        executor.execute(() -> {
            List<Gasolinera> filtered = GasolineraSorter.filterByFuel(gasolineras, selectedFuel);

            // Ordenar por precio descontado
            filtered.sort(Comparator.comparingDouble(g -> {
                Double price = g.getPrecio(selectedFuel);
                if (price == null || price <= 0) return Double.MAX_VALUE;
                return DiscountPrefs.applyAllDiscounts(
                        PriceListActivity.this, g.getMarca(), price);
            }));

            PriceRange range = GasolineraSorter.calculatePriceRange(filtered, selectedFuel);
            for (Gasolinera g : filtered) {
                g.setPriceLevel(GasolineraSorter.getPriceLevel(g.getPrecio(selectedFuel), range));
            }

            mainHandler.post(() -> {
                if (isDestroyed() || isFinishing()) return;
                if (filtered.isEmpty()) showEmpty();
                else showData(filtered, range);
            });
        });
    }

    /**
     * Carga y muestra las gasolineras ordenadas por precio descontado para las coordenadas dadas.
     *
     * @param lat Latitud del punto de referencia.
     * @param lon Longitud del punto de referencia.
     */
    private void loadWithCoordinates(double lat, double lon) {
        executor.execute(() -> {
            try {
                int radiusKm = RadiusUtils.loadRadiusKm(PriceListActivity.this);
                double radiusMeters = RadiusUtils.kmToMetersClamped(radiusKm);
                int maxMarkers = RadiusUtils.loadMarkersCount(PriceListActivity.this);

                List<Gasolinera> gasolineras = repository.getGasolineras();
                List<Gasolinera> filtered = GasolineraSorter.filterByFuel(gasolineras, selectedFuel);
                List<Gasolinera> inRadius = GasolineraSorter.getWithinRadius(
                        filtered, lat, lon, radiusMeters, maxMarkers
                );

                // Ordenar por precio descontado
                inRadius.sort(Comparator.comparingDouble(g -> {
                    Double price = g.getPrecio(selectedFuel);
                    if (price == null || price <= 0) return Double.MAX_VALUE;
                    return DiscountPrefs.applyAllDiscounts(
                            PriceListActivity.this, g.getMarca(), price);
                }));

                PriceRange range = GasolineraSorter.calculatePriceRange(inRadius, selectedFuel);
                for (Gasolinera g : inRadius) {
                    g.setPriceLevel(GasolineraSorter.getPriceLevel(g.getPrecio(selectedFuel), range));
                }

                mainHandler.post(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    if (inRadius.isEmpty()) showEmpty();
                    else showData(inRadius, range);
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    showError(getString(R.string.error_cargando_gasolineras));
                });
            }
        });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
    }

    private void showData(List<Gasolinera> data, PriceRange priceRange) {
        dataLoaded = true;
        lastLoadedGasolineras = data;
        progressBar.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        adapter.updateData(data, selectedFuel, priceRange);
    }

    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        tvError.setText(message);
        layoutError.setVisibility(View.VISIBLE);
    }

    @Override
    protected void navigateToDistanceList() {
        Intent intent = new Intent(this, DistanceListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        ArrayList<Gasolinera> gasolineras =
                getIntent().getParcelableArrayListExtra("gasolineras");
        if (gasolineras != null) {
            intent.putParcelableArrayListExtra("gasolineras", gasolineras);
        }
        startActivity(intent);
    }
}