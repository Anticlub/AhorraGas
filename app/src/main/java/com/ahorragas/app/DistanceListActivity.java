package com.ahorragas.app;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.IntentCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ahorragas.app.adapter.GasolineraAdapter;
import com.ahorragas.app.location.LocationHelper;
import com.ahorragas.app.model.FuelType;
import com.ahorragas.app.model.Gasolinera;
import com.ahorragas.app.model.PriceRange;
import com.ahorragas.app.ui.DistanceListViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class DistanceListActivity extends BaseActivity {

    private DistanceListViewModel viewModel;
    private LocationHelper locationHelper;
    private GasolineraAdapter adapter;
    private FuelType selectedFuel;

    private boolean dataLoaded = false;
    private FuelType lastLoadedFuel = null;

    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private TextView tvError;
    private View layoutError;
    private Button btnRetry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_distance_list);
        applySystemBarInsets(R.id.topBar, R.id.bottomNavDistance);

        viewModel = new ViewModelProvider(this).get(DistanceListViewModel.class);
        locationHelper = new LocationHelper(this);

        bindViews();
        setupRecyclerView();
        selectedFuel = FuelType.fromString(
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getString("pref_selected_fuel", FuelType.GASOLEO_A.name()));
        setupBottomNav();
        btnRetry.setOnClickListener(v -> loadAndDisplay());

        viewModel.getState().observe(this, this::render);
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
        locationHelper.cancel();
    }

    private void bindViews() {
        progressBar  = findViewById(R.id.progressBarDistance);
        recyclerView = findViewById(R.id.recyclerViewDistance);
        tvEmpty      = findViewById(R.id.tvEmptyDistance);
        layoutError  = findViewById(R.id.layoutErrorDistance);
        tvError      = findViewById(R.id.tvErrorDistance);
        btnRetry     = findViewById(R.id.btnRetryDistance);
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavDistance);
        setupBottomNav(bottomNav, R.id.nav_distance, selectedFuel);
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

    /**
     * Pide al ViewModel que cargue las gasolineras. Si vienen en el Intent las
     * usa directamente; si no, obtiene la ubicación por GPS y carga por radio.
     */
    private void loadAndDisplay() {
        showLoading();

        ArrayList<Gasolinera> fromIntent = IntentCompat.getParcelableArrayListExtra(
                getIntent(), "gasolineras", Gasolinera.class);
        if (fromIntent != null && !fromIntent.isEmpty()) {
            viewModel.loadFromIntent(fromIntent, selectedFuel);
        } else {
            locationHelper.getUserLocation(new LocationHelper.ResultCallback() {
                @Override
                public void onSuccess(Location location) {
                    viewModel.loadByRadius(location.getLatitude(), location.getLongitude(), selectedFuel);
                }

                @Override
                public void onError(LocationHelper.LocationError error) {
                    runOnUiThread(() -> {
                        if (isDestroyed() || isFinishing()) return;
                        showError(getString(R.string.error_ubicacion));
                    });
                }
            });
        }
    }

    /** Pinta el estado emitido por el ViewModel. */
    private void render(DistanceListViewModel.UiState st) {
        switch (st.status) {
            case LOADING: showLoading(); break;
            case DATA:    showData(st.data, st.priceRange); break;
            case EMPTY:   showEmpty(); break;
            case ERROR:   showError(st.errorMessage); break;
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
    }

    private void showData(List<Gasolinera> data, PriceRange priceRange) {
        dataLoaded = true;
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
    protected void navigateToPrice() {
        Intent intent = new Intent(this, PriceListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        ArrayList<Gasolinera> gasolineras =
                IntentCompat.getParcelableArrayListExtra(getIntent(), "gasolineras", Gasolinera.class);
        if (gasolineras != null) {
            intent.putParcelableArrayListExtra("gasolineras", gasolineras);
        }
        startActivity(intent);
    }
}
