package com.example.ahorragas;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
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
                                    "Sin permiso de notificaciones no podremos avisarte. Puedes activarlo en Ajustes.",
                                    Toast.LENGTH_LONG).show();
                        }
                        pendingAlertGasolinera = null;
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

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
     * Muestra un diálogo para que el usuario introduzca el precio umbral
     * de la alerta para la gasolinera y combustible seleccionados.
     * Tras guardar o eliminar, refresca el item correspondiente en el adapter.
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
                    .setPositiveButton("Eliminar", (d, w) -> {
                        PriceAlertPrefs.remove(this, key);
                        Toast.makeText(this, "Alerta eliminada", Toast.LENGTH_SHORT).show();
                        int pos = adapter.getPositionOf(gasolinera);
                        if (pos >= 0) adapter.notifyItemChanged(pos);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        if (PriceAlertPrefs.count(this) >= PriceAlertPrefs.MAX_ALERTS) {
            Toast.makeText(this,
                    "Ya tienes 3 alertas. Elimina una antes de añadir otra.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Double currentPrice = gasolinera.getPrecio(selectedFuel);
        String priceHint = currentPrice != null
                ? String.format(java.util.Locale.getDefault(), "Precio actual: %.3f €/L", currentPrice)
                : "Precio no disponible";

        EditText etPrice = new EditText(this);
        etPrice.setHint("Ej: 1.450");
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
                .setPositiveButton("Guardar", (d, w) -> {
                    String input = etPrice.getText().toString().trim().replace(",", ".");
                    double price;
                    try {
                        price = Double.parseDouble(input);
                        if (price <= 0) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Precio no válido.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String name = gasolinera.getMarca() + " · " + gasolinera.getMunicipio();
                    PriceAlert alert = new PriceAlert(
                            gasolinera.getId(), name, selectedFuel, price, 0L);

                    boolean saved = PriceAlertPrefs.add(this, alert);
                    if (saved) {
                        Toast.makeText(this, "✅ Alerta guardada", Toast.LENGTH_SHORT).show();
                        int pos = adapter.getPositionOf(gasolinera);
                        if (pos >= 0) adapter.notifyItemChanged(pos);
                    } else {
                        Toast.makeText(this,
                                "Ya tienes 3 alertas. Elimina una antes de añadir otra.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
    }

    private void showData(List<Gasolinera> data) {
        PriceRange priceRange = GasolineraSorter.calculatePriceRange(data, selectedFuel);
        for (Gasolinera g : data) {
            g.setPriceLevel(GasolineraSorter.getPriceLevel(g.getPrecio(selectedFuel), priceRange));
        }
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

    /**
     * Carga la lista de favoritos desde SharedPreferences, calcula la distancia
     * a cada gasolinera si la ubicación está disponible, y actualiza el adapter.
     */
    private void loadAndDisplay() {
        showLoading();

        List<Gasolinera> favorites = FavoritesPrefs.loadAll(this);

        if (favorites.isEmpty()) {
            showEmpty();
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
                runOnUiThread(() -> showData(favorites));
            }

            @Override
            public void onError(LocationHelper.LocationError error) {
                runOnUiThread(() -> showData(favorites));
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