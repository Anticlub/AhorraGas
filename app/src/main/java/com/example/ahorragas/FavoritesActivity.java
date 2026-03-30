package com.example.ahorragas;

import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ahorragas.adapter.GasolineraAdapter;
import com.example.ahorragas.location.LocationHelper;
import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Gasolinera;
import com.example.ahorragas.util.FavoritesPrefs;
import com.example.ahorragas.util.GeoUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class FavoritesActivity extends BaseActivity {

    private GasolineraAdapter adapter;
    private FuelType selectedFuel;
    private TextView tvEmptyState;
    private LocationHelper locationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        setupRecyclerView();
        setupBottomNav();
        locationHelper = new LocationHelper(this);
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
                new java.util.ArrayList<>(),
                selectedFuel,
                gasolinera -> navigateToDetail(gasolinera)
        );
        RecyclerView recyclerView = findViewById(R.id.recyclerViewFavorites);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        tvEmptyState = findViewById(R.id.tvEmptyState);
    }

    /**
     * Carga la lista de favoritos desde SharedPreferences, calcula la distancia
     * a cada gasolinera si la ubicación está disponible, y actualiza el adapter.
     */
    private void loadAndDisplay() {
        List<Gasolinera> favorites = FavoritesPrefs.loadAll(this);

        if (favorites.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            adapter.updateData(favorites, selectedFuel);
            return;
        }

        tvEmptyState.setVisibility(View.GONE);

        locationHelper.getUserLocation(new LocationHelper.ResultCallback() {
            @Override
            public void onSuccess(Location location) {
                for (Gasolinera g : favorites) {
                    double distance = GeoUtils.distanceMeters(
                            location.getLatitude(),
                            location.getLongitude(),
                            g.getLat(),
                            g.getLon()
                    );
                    g.setDistanceMeters(distance);
                }
                runOnUiThread(() -> adapter.updateData(favorites, selectedFuel));
            }

            @Override
            public void onError(LocationHelper.LocationError error) {
                runOnUiThread(() -> adapter.updateData(favorites, selectedFuel));
            }
        });
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavFavorites);
        bottomNav.setSelectedItemId(R.id.nav_favorites);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_favorites) return true;
            else if (id == R.id.nav_map) {
                navigateToMap();
                return true;
            } else if (id == R.id.nav_price) {
                navigateToPrice();
                return true;
            } else if (id == R.id.nav_distance) {
                navigateToDistanceList();
                return true;
            } else if (id == R.id.nav_preferences) {
                navigateToPreferences();
                return true;
            }
            return false;
        });
    }
}