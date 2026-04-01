package com.example.ahorragas;

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

    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private TextView tvError;
    private View layoutError;
    private Button btnRetry;

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
        selectedFuel = FuelType.fromString(
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getString("pref_selected_fuel", FuelType.GASOLEO_A.name())
        );
        loadAndDisplay();
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

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
    }

    private void showData(List<Gasolinera> data) {
        progressBar.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        adapter.updateData(data, selectedFuel);
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

    /**
     * Obtiene la ubicación del usuario, carga las gasolineras del repositorio,
     * filtra por radio y máximo de marcadores configurados en Preferencias,
     * las ordena por precio ascendente y actualiza el adapter.
     */
    private void loadAndDisplay() {
        showLoading();

        locationHelper.getUserLocation(new LocationHelper.ResultCallback() {
            @Override
            public void onSuccess(Location location) {
                new Thread(() -> {
                    try {
                        int radiusKm = RadiusUtils.loadRadiusKm(PriceListActivity.this);
                        double radiusMeters = RadiusUtils.kmToMetersClamped(radiusKm);
                        int maxMarkers = RadiusUtils.loadMarkersCount(PriceListActivity.this);

                        List<Gasolinera> gasolineras = repository.getGasolineras();
                        List<Gasolinera> filtered = GasolineraSorter.filterByFuel(gasolineras, selectedFuel);
                        List<Gasolinera> inRadius = GasolineraSorter.getWithinRadius(
                                filtered,
                                location.getLatitude(),
                                location.getLongitude(),
                                radiusMeters,
                                maxMarkers
                        );

                        inRadius.sort(Comparator.comparingDouble(g ->
                                g.getPrecio(selectedFuel) != null ? g.getPrecio(selectedFuel) : Double.MAX_VALUE
                        ));

                        PriceRange range = GasolineraSorter.calculatePriceRange(inRadius, selectedFuel);
                        for (Gasolinera g : inRadius) {
                            g.setPriceLevel(GasolineraSorter.getPriceLevel(g.getPrecio(selectedFuel), range));
                        }

                        runOnUiThread(() -> {
                            if (inRadius.isEmpty()) {
                                showEmpty();
                            } else {
                                showData(inRadius);
                            }
                        });

                    } catch (Exception e) {
                        runOnUiThread(() ->
                                showError(getString(R.string.error_cargando_gasolineras)));
                    }
                }).start();
            }

            @Override
            public void onError(LocationHelper.LocationError error) {
                runOnUiThread(() ->
                        showError(getString(R.string.error_ubicacion)));
            }
        });
    }
}