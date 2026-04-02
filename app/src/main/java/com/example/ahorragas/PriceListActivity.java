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
        selectedFuel = FuelType.fromString(
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getString("pref_selected_fuel", FuelType.GASOLEO_A.name())
        );
        loadAndDisplay();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
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
     * Carga y muestra las gasolineras filtradas por municipio si hay búsqueda
     * activa, o por radio alrededor de la ubicación GPS si no la hay.
     */
    private void loadAndDisplay() {
        showLoading();
        String query = getIntent().getStringExtra("search_query");
        if (query != null && !query.isEmpty()) {
            loadByMunicipio(query);
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
     * Filtra las gasolineras por municipio y las ordena por precio ascendente.
     *
     * @param query Nombre del municipio a buscar.
     */
    private void loadByMunicipio(String query) {
        executor.execute(() -> {
            try {
                List<Gasolinera> gasolineras = repository.getGasolineras();
                List<Gasolinera> filtered = GasolineraSorter.filterByFuel(gasolineras, selectedFuel);

                String normalizedQuery = normalize(query);
                List<Gasolinera> byMunicipio = new ArrayList<>();
                for (Gasolinera g : filtered) {
                    if (matchesMunicipio(normalize(g.getMunicipio()), normalizedQuery)) {
                        byMunicipio.add(g);
                    }
                }

                byMunicipio.sort(Comparator.comparingDouble(g ->
                        g.getPrecio(selectedFuel) != null ? g.getPrecio(selectedFuel) : Double.MAX_VALUE
                ));

                PriceRange range = GasolineraSorter.calculatePriceRange(byMunicipio, selectedFuel);
                for (Gasolinera g : byMunicipio) {
                    g.setPriceLevel(GasolineraSorter.getPriceLevel(g.getPrecio(selectedFuel), range));
                }

                mainHandler.post(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    if (byMunicipio.isEmpty()) showEmpty();
                    else showData(byMunicipio);
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    showError(getString(R.string.error_cargando_gasolineras));
                });
            }
        });
    }

    /**
     * Carga y muestra las gasolineras ordenadas por precio para las coordenadas dadas.
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

                inRadius.sort(Comparator.comparingDouble(g ->
                        g.getPrecio(selectedFuel) != null ? g.getPrecio(selectedFuel) : Double.MAX_VALUE
                ));

                PriceRange range = GasolineraSorter.calculatePriceRange(inRadius, selectedFuel);
                for (Gasolinera g : inRadius) {
                    g.setPriceLevel(GasolineraSorter.getPriceLevel(g.getPrecio(selectedFuel), range));
                }

                mainHandler.post(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    if (inRadius.isEmpty()) showEmpty();
                    else showData(inRadius);
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    showError(getString(R.string.error_cargando_gasolineras));
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
     * Maneja los formatos del Ministerio:
     * - "Casar (El)" → "el casar"
     * - "Donostia-San Sebastián" → "donostia" o "san sebastian"
     * - "Elche/Elx" → "elche" o "elx"
     *
     * @param normalizedMunicipio Municipio ya normalizado.
     * @param normalizedQuery     Búsqueda ya normalizada.
     * @return true si el municipio coincide con la búsqueda.
     */
    private boolean matchesMunicipio(String normalizedMunicipio, String normalizedQuery) {
        if (normalizedMunicipio.equals(normalizedQuery)) return true;

        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^(.+?)\\s*\\(([^)]+)\\)$")
                .matcher(normalizedMunicipio);
        if (m.matches()) {
            String reordered = m.group(2).trim() + " " + m.group(1).trim();
            if (reordered.equals(normalizedQuery)) return true;
        }

        for (String separator : new String[]{"/", "-"}) {
            for (String part : normalizedMunicipio.split(java.util.regex.Pattern.quote(separator))) {
                if (part.trim().equals(normalizedQuery)) return true;
            }
        }

        return false;
    }
    @Override
    protected void navigateToDistanceList() {
        Intent intent = new Intent(this, DistanceListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        String query = getIntent().getStringExtra("search_query");
        if (query != null) {
            intent.putExtra("search_query", query);
        }
        startActivity(intent);
    }
}