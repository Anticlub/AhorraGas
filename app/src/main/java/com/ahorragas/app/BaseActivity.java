package com.ahorragas.app;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ahorragas.app.model.FuelType;
import com.ahorragas.app.model.Gasolinera;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity {

    private static final int ANDROID_15_API = 35;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
    }

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
        Intent intent = new Intent(this, com.ahorragas.app.detail.StationDetailActivity.class);
        intent.putExtra(com.ahorragas.app.detail.StationDetailActivity.EXTRA_GASOLINERA, gasolinera);
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

    /**
     * Configura el BottomNavigationView adaptando las etiquetas al tipo
     * de combustible seleccionado.
     *
     * @param bottomNav    Vista del menú inferior.
     * @param selectedItem ID del item que debe aparecer seleccionado.
     * @param fuelType     Tipo de combustible activo.
     */
    protected void setupBottomNav(BottomNavigationView bottomNav, int selectedItem, FuelType fuelType) {
        setupBottomNav(bottomNav, selectedItem);
        if (fuelType == FuelType.ELECTRICO) {
            bottomNav.getMenu().findItem(R.id.nav_price).setTitle(getString(R.string.label_by_power));
            bottomNav.getMenu().findItem(R.id.nav_price).setIcon(R.drawable.ic_bolt);
        }
    }

    protected void applySystemBarInsets(int topBarId, int bottomBarId) {
        if (Build.VERSION.SDK_INT < ANDROID_15_API) return;

        View topBar = topBarId != 0 ? findViewById(topBarId) : null;
        View bottomBar = bottomBarId != 0 ? findViewById(bottomBarId) : null;

        if (topBar != null) {
            applyTopInset(topBar);
        }
        if (bottomBar != null) {
            applyBottomInset(bottomBar);
        }
    }

    private void applyTopInset(View view) {
        final int initialLeft = view.getPaddingLeft();
        final int initialTop = view.getPaddingTop();
        final int initialRight = view.getPaddingRight();
        final int initialBottom = view.getPaddingBottom();
        final int initialHeight = view.getLayoutParams() != null
                ? view.getLayoutParams().height
                : ViewGroup.LayoutParams.WRAP_CONTENT;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            v.setPadding(initialLeft, initialTop + insets.top, initialRight, initialBottom);
            if (initialHeight > 0) {
                ViewGroup.LayoutParams params = v.getLayoutParams();
                params.height = initialHeight + insets.top;
                v.setLayoutParams(params);
            }
            return windowInsets;
        });
        requestApplyInsetsWhenAttached(view);
    }

    private void applyBottomInset(View view) {
        final int initialLeft = view.getPaddingLeft();
        final int initialTop = view.getPaddingTop();
        final int initialRight = view.getPaddingRight();
        final int initialBottom = view.getPaddingBottom();
        final int initialHeight = view.getLayoutParams() != null
                ? view.getLayoutParams().height
                : ViewGroup.LayoutParams.WRAP_CONTENT;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            v.setPadding(initialLeft, initialTop, initialRight, initialBottom + insets.bottom);
            if (initialHeight > 0) {
                ViewGroup.LayoutParams params = v.getLayoutParams();
                params.height = initialHeight + insets.bottom;
                v.setLayoutParams(params);
            }
            return windowInsets;
        });
        requestApplyInsetsWhenAttached(view);
    }

    private void requestApplyInsetsWhenAttached(View view) {
        if (ViewCompat.isAttachedToWindow(view)) {
            ViewCompat.requestApplyInsets(view);
            return;
        }

        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View attachedView) {
                attachedView.removeOnAttachStateChangeListener(this);
                ViewCompat.requestApplyInsets(attachedView);
            }

            @Override
            public void onViewDetachedFromWindow(View detachedView) {
            }
        });
    }
}
