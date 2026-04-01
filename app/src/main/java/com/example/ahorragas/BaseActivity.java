package com.example.ahorragas;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ahorragas.model.Gasolinera;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity {

    protected void navigateToMap() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    protected void navigateToPreferences() {
        startActivity(new Intent(this, PreferencesActivity.class));
    }

    protected void navigateToDistanceList() {
        Intent intent = new Intent(this, DistanceListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
    protected void navigateToPrice() {
        Intent intent = new Intent(this, PriceListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    protected void navigateToDetail(Gasolinera gasolinera) {
        Intent intent = new Intent(this, com.example.ahorragas.detail.StationDetailActivity.class);
        intent.putExtra(com.example.ahorragas.detail.StationDetailActivity.EXTRA_GASOLINERA, gasolinera);
        startActivity(intent);
    }
    protected void navigateToFavorites() {
        Intent intent = new Intent(this, FavoritesActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
    /**
     * Configura el BottomNavigationView centralizando la lógica de navegación.
     *
     * @param bottomNav    Vista del menú inferior.
     * @param selectedItem ID del item que debe aparecer seleccionado.
     */
    protected void setupBottomNav(BottomNavigationView bottomNav, int selectedItem) {
        bottomNav.setSelectedItemId(selectedItem);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedItem) return true;
            if (id == R.id.nav_map) {
                navigateToMap();
                return true;
            } else if (id == R.id.nav_price) {
                navigateToPrice();
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
}