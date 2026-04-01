package com.example.ahorragas;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.List;

public class DistanceListActivity extends BaseActivity {
    private GasolineraRepository repository;
    private LocationHelper locationHelper;
    private GasolineraAdapter adapter;
    private FuelType selectedFuel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_distance_list);

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

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavDistance);
        setupBottomNav(bottomNav, R.id.nav_distance);
    }

    private void setupRecyclerView() {
        adapter = new GasolineraAdapter(
                new ArrayList<>(),
                selectedFuel,
                gasolinera -> navigateToDetail(gasolinera)
        );
        RecyclerView recyclerView = findViewById(R.id.recyclerViewDistance);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadAndDisplay() {
        locationHelper.getUserLocation(new LocationHelper.ResultCallback() {
            @Override
            public void onSuccess(Location location) {
                new Thread(() -> {
                    try {
                        int radiusKm = RadiusUtils.loadRadiusKm(DistanceListActivity.this);
                        double radiusMeters = RadiusUtils.kmToMetersClamped(radiusKm);
                        int maxMarkers = RadiusUtils.loadMarkersCount(DistanceListActivity.this);

                        List<Gasolinera> gasolineras = repository.getGasolineras();
                        List<Gasolinera> filtered = GasolineraSorter.filterByFuel(gasolineras, selectedFuel);
                        List<Gasolinera> sorted = GasolineraSorter.getWithinRadius(
                                filtered,
                                location.getLatitude(),
                                location.getLongitude(),
                                radiusMeters,
                                maxMarkers
                        );
                        PriceRange range = GasolineraSorter.calculatePriceRange(sorted, selectedFuel);
                        for (Gasolinera g : sorted) {
                            g.setPriceLevel(GasolineraSorter.getPriceLevel(g.getPrecio(selectedFuel), range));
                        }
                        runOnUiThread(() -> {
                            if (isDestroyed() || isFinishing()) return;
                            adapter.updateData(sorted, selectedFuel);
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            if (isDestroyed() || isFinishing()) return;
                            Toast.makeText(DistanceListActivity.this,
                                    "Error cargando gasolineras", Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            }

            @Override
            public void onError(LocationHelper.LocationError error) {
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    Toast.makeText(DistanceListActivity.this,
                            "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}