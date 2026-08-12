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
import com.ahorragas.app.ui.PriceListViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class PriceListActivity extends BaseActivity {

    private PriceListViewModel viewModel;
    private GasolineraAdapter adapter;
    private FuelType selectedFuel;
    private LocationHelper locationHelper;

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
        setContentView(R.layout.activity_price_list);
        applySystemBarInsets(R.id.topBar, R.id.bottomNavPrice);

        viewModel = new ViewModelProvider(this).get(PriceListViewModel.class);
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
    }

    private void bindViews() {
        progressBar = findViewById(R.id.progressBarPrice);
        recyclerView = findViewById(R.id.recyclerViewPrice);
        tvEmpty = findViewById(R.id.tvEmptyPrice);
        layoutError = findViewById(R.id.layoutErrorPrice);
        tvError = findViewById(R.id.tvErrorPrice);
        btnRetry = findViewById(R.id.btnRetryPrice);
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
    private void render(PriceListViewModel.UiState st) {
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
    protected void navigateToDistanceList() {
        Intent intent = new Intent(this, DistanceListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        ArrayList<Gasolinera> gasolineras =
                IntentCompat.getParcelableArrayListExtra(getIntent(), "gasolineras", Gasolinera.class);
        if (gasolineras != null) {
            intent.putParcelableArrayListExtra("gasolineras", gasolineras);
        }
        startActivity(intent);
    }
}
