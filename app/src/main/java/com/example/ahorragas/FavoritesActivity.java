package com.example.ahorragas;

import com.example.ahorragas.model.Vehicle;
import com.example.ahorragas.util.VehiclePrefs;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ahorragas.adapter.GasolineraAdapter;
import com.example.ahorragas.location.LocationHelper;
import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Gasolinera;
import com.example.ahorragas.model.PriceAlert;
import com.example.ahorragas.model.PriceRange;
import com.example.ahorragas.util.FavoritesPrefs;
import com.example.ahorragas.util.GasolineraSorter;
import com.example.ahorragas.util.GeoUtils;
import com.example.ahorragas.util.PriceAlertPrefs;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends BaseActivity {

    private GasolineraAdapter adapter;
    private FuelType selectedFuel;
    private LocationHelper locationHelper;

    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private TextView tvError;
    private View layoutError;
    private Button btnRetry;

    private Gasolinera pendingAlertGasolinera = null;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            if (pendingAlertGasolinera != null) {
                                showAlertDialog(pendingAlertGasolinera);
                            }
                        } else {
                            Toast.makeText(this,
                                    getString(R.string.msg_no_notification_permission),
                                    Toast.LENGTH_LONG).show();
                        }
                        pendingAlertGasolinera = null;
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        applySystemBarInsets(R.id.topBar, R.id.bottomNavFavorites);

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
        loadAndDisplay();
    }

    private void bindViews() {
        progressBar  = findViewById(R.id.progressBarFavorites);
        recyclerView = findViewById(R.id.recyclerViewFavorites);
        tvEmpty      = findViewById(R.id.tvEmptyState);
        layoutError  = findViewById(R.id.layoutErrorFavorites);
        tvError      = findViewById(R.id.tvErrorFavorites);
        btnRetry     = findViewById(R.id.btnRetryFavorites);
    }

    private void setupRecyclerView() {
        adapter = new GasolineraAdapter(
                new ArrayList<>(),
                selectedFuel,
                gasolinera -> navigateToDetail(gasolinera)
        );
        adapter.setOnAlertClickListener(gasolinera -> requestNotificationPermissionIfNeeded(gasolinera));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Comprueba si la app tiene permiso para enviar notificaciones (Android 13+).
     * Si no lo tiene, lo solicita al usuario. Si ya lo tiene, abre el diálogo
     * de creación de alerta directamente.
     *
     * @param gasolinera Gasolinera sobre la que el usuario quiere crear una alerta.
     */
    private void requestNotificationPermissionIfNeeded(Gasolinera gasolinera) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                showAlertDialog(gasolinera);
            } else {
                pendingAlertGasolinera = gasolinera;
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            showAlertDialog(gasolinera);
        }
    }

    /**
     * Muestra un diálogo explicativo y lleva al usuario a la pantalla del sistema
     * para eximir la app de la optimización de batería, si aún no está eximida.
     */
    private void requestBatteryOptimizationExemptionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm == null) return;
        if (pm.isIgnoringBatteryOptimizations(getPackageName())) return;

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_background_alerts))
                .setMessage(getString(R.string.msg_background_alerts))
                .setPositiveButton(getString(R.string.btn_configure), (d, w) -> {
                    Intent intent = new Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:" + getPackageName())
                    );
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.btn_not_now), null)
                .show();
    }

    /**
     * Muestra un diálogo para que el usuario introduzca el precio umbral
     * de la alerta para la gasolinera y combustible seleccionados.
     *
     * @param gasolinera Gasolinera sobre la que crear la alerta.
     */
    private void showAlertDialog(Gasolinera gasolinera) {
        String key = gasolinera.getId() + "_" + selectedFuel.name();

        if (PriceAlertPrefs.exists(this, key)) {
            new AlertDialog.Builder(this)
                    .setTitle("🔔 Alerta existente")
                    .setMessage("Ya tienes una alerta para " + gasolinera.getMarca()
                            + " con " + selectedFuel.displayName()
                            + ".\n¿Quieres eliminarla?")
                    .setPositiveButton(getString(R.string.btn_delete), (d, w) -> {
                        PriceAlertPrefs.remove(this, key);
                        Toast.makeText(this, getString(R.string.msg_alert_deleted), Toast.LENGTH_SHORT).show();
                        int pos = adapter.getPositionOf(gasolinera);
                        if (pos >= 0) adapter.notifyItemChanged(pos);
                    })
                    .setNegativeButton(getString(R.string.btn_cancel), null)
                    .show();
            return;
        }

        if (PriceAlertPrefs.count(this) >= PriceAlertPrefs.MAX_ALERTS) {
            Toast.makeText(this, getString(R.string.msg_max_alerts), Toast.LENGTH_SHORT).show();
            return;
        }

        Double currentPrice = gasolinera.getPrecio(selectedFuel);
        String priceHint = currentPrice != null
                ? String.format(java.util.Locale.getDefault(), "Precio actual: %.3f €/L", currentPrice)
                : getString(R.string.msg_price_unavailable);

        EditText etPrice = new EditText(this);
        etPrice.setHint(getString(R.string.hint_alert_price));
        etPrice.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, 0);

        TextView tvInfo = new TextView(this);
        tvInfo.setText(priceHint);
        tvInfo.setTextSize(13);
        tvInfo.setPadding(0, 0, 0, pad / 2);
        layout.addView(tvInfo);
        layout.addView(etPrice);

        new AlertDialog.Builder(this)
                .setTitle("🔔 Alerta para " + gasolinera.getMarca())
                .setMessage("Combustible: " + selectedFuel.displayName()
                        + "\nNotificaremos cuando el precio sea igual o menor al que indiques.")
                .setView(layout)
                .setPositiveButton(getString(R.string.btn_save), (d, w) -> {
                    String input = etPrice.getText().toString().trim().replace(",", ".");
                    double price;
                    try {
                        price = Double.parseDouble(input);
                        if (price <= 0) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, getString(R.string.msg_invalid_price), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String name = gasolinera.getMarca() + " · " + gasolinera.getMunicipio();
                    PriceAlert alert = new PriceAlert(
                            gasolinera.getId(), name, selectedFuel, price, 0L);

                    boolean saved = PriceAlertPrefs.add(this, alert);
                    if (saved) {
                        Toast.makeText(this, getString(R.string.msg_alert_saved), Toast.LENGTH_SHORT).show();
                        requestBatteryOptimizationExemptionIfNeeded();
                        int pos = adapter.getPositionOf(gasolinera);
                        if (pos >= 0) adapter.notifyItemChanged(pos);
                    } else {
                        Toast.makeText(this, getString(R.string.msg_max_alerts), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
    }

    private void showData(List<Gasolinera> data, FuelType fuel) {
        // Ordenar: primero con precio de menor a mayor, N/D al final
        data.sort((a, b) -> {
            Double priceA = a.getPrecio(fuel);
            Double priceB = b.getPrecio(fuel);
            boolean aHasPrice = priceA != null && priceA > 0;
            boolean bHasPrice = priceB != null && priceB > 0;
            if (aHasPrice && bHasPrice) return Double.compare(priceA, priceB);
            if (aHasPrice) return -1;
            if (bHasPrice) return 1;
            return 0;
        });

        PriceRange priceRange = GasolineraSorter.calculatePriceRange(data, fuel);
        for (Gasolinera g : data) {
            g.setPriceLevel(GasolineraSorter.getPriceLevel(g.getPrecio(fuel), priceRange));
        }
        progressBar.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        adapter.updateData(data, fuel, priceRange);
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
     * Carga la lista de favoritos desde Room en background, calcula la distancia
     * a cada gasolinera si la ubicación está disponible, y actualiza el adapter.
     * Lee el combustible activo en el momento de la carga para evitar
     * inconsistencias al cambiar de vehículo.
     */
    private void loadAndDisplay() {
        showLoading();

        Vehicle activeVehicle = VehiclePrefs.loadActiveVehicle(this);
        if (activeVehicle != null && activeVehicle.isElectric()) {
            showEmpty();
            tvEmpty.setText(getString(R.string.msg_favorites_not_available_electric));
            return;
        }

        new Thread(() -> {
            FuelType currentFuel = FuelType.fromString(
                    PreferenceManager.getDefaultSharedPreferences(this)
                            .getString("pref_selected_fuel", FuelType.GASOLEO_A.name()));
            selectedFuel = currentFuel;

            List<Gasolinera> favorites = FavoritesPrefs.loadAll(this);

            if (favorites.isEmpty()) {
                runOnUiThread(this::showEmpty);
                return;
            }

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
                    runOnUiThread(() -> showData(favorites, currentFuel));
                }

                @Override
                public void onError(LocationHelper.LocationError error) {
                    runOnUiThread(() -> showData(favorites, currentFuel));
                }
            });
        }).start();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavFavorites);
        setupBottomNav(bottomNav, R.id.nav_favorites, selectedFuel);
    }
}
