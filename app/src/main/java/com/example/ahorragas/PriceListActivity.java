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
import com.example.ahorragas.data.ElectrolineraRepository;
import com.example.ahorragas.data.EstacionRepository;
import com.example.ahorragas.data.GasolineraRepository;
import com.example.ahorragas.data.remote.RemoteDgtDataSource;
import com.example.ahorragas.data.RoomElectrolineraDataSource;
import com.example.ahorragas.data.RoomGasolineraDataSource;
import com.example.ahorragas.data.local.AppDatabase;
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

    private EstacionRepository repository;
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

        AppDatabase db = AppDatabase.getInstance(this);
        RoomGasolineraDataSource roomGasolineraDs = new RoomGasolineraDataSource(db);
        RoomElectrolineraDataSource roomElectrolineraDs = new RoomElectrolineraDataSource(db);
        GasolineraRepository gasolineraRepo = GasolineraRepository.getInstance(roomGasolineraDs);
        ElectrolineraRepository electrolineraRepo = ElectrolineraRepository.getInstance(
                new RemoteDgtDataSource(), roomElectrolineraDs);
        repository = EstacionRepository.getInstance(gasolineraRepo, electrolineraRepo);
        locationHelper = new LocationHelper(this);

        bindViews();
        setupRecyclerView();
        selectedFuel = FuelType.fromString(
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getString("pref_selected_fuel", FuelType.GASOLEO_A.name()));
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
            TextView tvTitle = findViewById(R.id.tvPriceListTitle);
            tvTitle.setText(currentFuel == FuelType.ELECTRICO
                    ? "Estaciones por potencia"
                    : "Estaciones por precio");
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
        locationHelper.cancel();
        executor.shutdownNow();
    }

    private void bindViews() {
        progressBar = findViewById(R.id.progressBarPrice);
        recyclerView = findViewById(R.id.recyclerViewPrice);
        tvEmpty = findViewById(R.id.tvEmptyPrice);
        layoutError = findViewById(R.id.layoutErrorPrice);
        tvError = findViewById(R.id.tvErrorPrice);
        btnRetry = findViewById(R.id.btnRetryPrice);
        TextView tvTitle = findViewById(R.id.tvPriceListTitle);
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
        setupBottomNav(bottomNav, R.id.nav_price, selectedFuel);
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

            if (selectedFuel == FuelType.ELECTRICO) {
                // Electrolineras: ordenar por potencia máxima descendente
                filtered.sort((a, b) -> {
                    double potA = getMaxPotencia(a);
                    double potB = getMaxPotencia(b);
                    return Double.compare(potB, potA);
                });
            } else {
                filtered.sort(Comparator.comparingDouble(g ->
                        g.getPrecio(selectedFuel) != null
                                ? DiscountPrefs.applyAllDiscounts(
                                PriceListActivity.this, g.getMarca(),
                                g.getPrecio(selectedFuel))
                                : Double.MAX_VALUE
                ));
                PriceRange range = GasolineraSorter.calculatePriceRange(filtered, selectedFuel);
                for (Gasolinera g : filtered) {
                    double discounted = g.getPrecio(selectedFuel) != null
                            ? DiscountPrefs.applyAllDiscounts(
                            PriceListActivity.this, g.getMarca(),
                            g.getPrecio(selectedFuel))
                            : 0;
                    g.setPriceLevel(GasolineraSorter.getPriceLevel(discounted, range));
                }
            }

            final PriceRange finalRange = selectedFuel == FuelType.ELECTRICO
                    ? new PriceRange(null, null, 0)
                    : GasolineraSorter.calculatePriceRange(filtered, selectedFuel);

            mainHandler.post(() -> {
                if (isDestroyed() || isFinishing()) return;
                if (filtered.isEmpty()) showEmpty();
                else showData(filtered, finalRange);
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

                List<Gasolinera> gasolineras;
                if (selectedFuel == FuelType.ELECTRICO) {
                    gasolineras = new ArrayList<>(
                            repository.getElectrolinerasByRadius(lat, lon, radiusMeters));
                } else {
                    gasolineras = new ArrayList<>(
                            repository.getGasolinerasByRadius(lat, lon, radiusMeters));
                }

                List<Gasolinera> filtered = GasolineraSorter.filterByFuel(gasolineras, selectedFuel);

                List<Gasolinera> inRadius = GasolineraSorter.getWithinRadius(
                        filtered, lat, lon, radiusMeters, maxMarkers);

                if (selectedFuel == FuelType.ELECTRICO) {
                    inRadius.sort((a, b) -> {
                        double potA = getMaxPotencia(a);
                        double potB = getMaxPotencia(b);
                        return Double.compare(potB, potA);
                    });
                } else {
                    inRadius.sort(Comparator.comparingDouble(g ->
                            g.getPrecio(selectedFuel) != null
                                    ? DiscountPrefs.applyAllDiscounts(
                                    PriceListActivity.this, g.getMarca(),
                                    g.getPrecio(selectedFuel))
                                    : Double.MAX_VALUE
                    ));
                    PriceRange range = GasolineraSorter.calculatePriceRange(inRadius, selectedFuel);
                    for (Gasolinera g : inRadius) {
                        double discounted = g.getPrecio(selectedFuel) != null
                                ? DiscountPrefs.applyAllDiscounts(
                                PriceListActivity.this, g.getMarca(),
                                g.getPrecio(selectedFuel))
                                : 0;
                        g.setPriceLevel(GasolineraSorter.getPriceLevel(discounted, range));
                    }
                }

                final List<Gasolinera> finalList = inRadius;
                final PriceRange finalRange = selectedFuel == FuelType.ELECTRICO
                        ? new PriceRange(null, null, 0)
                        : GasolineraSorter.calculatePriceRange(inRadius, selectedFuel);

                mainHandler.post(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    if (finalList.isEmpty()) showEmpty();
                    else showData(finalList, finalRange);
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
    /**
     * Devuelve la potencia máxima en vatios de una electrolinera.
     * Devuelve 0 si no tiene conectores o no es eléctrica.
     *
     * @param g estación a consultar
     * @return potencia máxima en vatios
     */
    private double getMaxPotencia(Gasolinera g) {
        if (g.getConectores() == null || g.getConectores().isEmpty()) return 0;
        double max = 0;
        for (com.example.ahorragas.model.Electrolinera.Conector c : g.getConectores()) {
            if (c.getPotenciaW() != null && c.getPotenciaW() > max) {
                max = c.getPotenciaW();
            }
        }
        return max;
    }

}