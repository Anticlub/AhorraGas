package com.example.ahorragas;

import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_price_list);

        repository = GasolineraRepository.getInstance(new CachedRemoteApiDataSource(this));
        locationHelper = new LocationHelper(this);

        setupRecyclerView();
        setupBottomNav();
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

    private void setupRecyclerView() {
        adapter = new GasolineraAdapter(
                new ArrayList<>(),
                selectedFuel,
                gasolinera -> navigateToDetail(gasolinera)
        );
        RecyclerView recyclerView = findViewById(R.id.recyclerViewPrice);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavPrice);
        bottomNav.setSelectedItemId(R.id.nav_price);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_price) return true;
            else if (id == R.id.nav_map) {
                navigateToMap();
                return true;
            } else if (id == R.id.nav_distance) {
                navigateToDistanceList();
                return true;
            } else if (id == R.id.nav_favorites) {
                navigateToFavorites();
                return true;
            } else if (id == R.id.nav_preferences) {
                navigateToPreferences();
                return true;
            }
            return false;
        });
    }

    /**
     * Obtiene la ubicación del usuario, carga las gasolineras del repositorio,
     * filtra por radio y máximo de marcadores configurados en Preferencias,
     * las ordena por precio ascendente y actualiza el adapter.
     */
    private void loadAndDisplay() {
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

                        runOnUiThread(() -> adapter.updateData(inRadius, selectedFuel));

                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(PriceListActivity.this,
                                "Error cargando gasolineras", Toast.LENGTH_SHORT).show());
                    }
                }).start();
            }

            @Override
            public void onError(LocationHelper.LocationError error) {
                runOnUiThread(() -> Toast.makeText(PriceListActivity.this,
                        "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show());
            }
        });
    }
}