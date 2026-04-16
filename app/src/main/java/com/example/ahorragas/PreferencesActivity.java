package com.example.ahorragas;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.ahorragas.model.Discount;
import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.PriceAlert;
import com.example.ahorragas.model.Vehicle;
import com.example.ahorragas.util.DiscountPrefs;
import com.example.ahorragas.util.PriceAlertPrefs;
import com.example.ahorragas.util.RadiusUtils;
import com.example.ahorragas.util.VehiclePrefs;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;
import java.util.Locale;

public class PreferencesActivity extends BaseActivity {

    // ── Vehículos ─────────────────────────────────────────────────────────────
    private LinearLayout vehicleListContainer;
    private List<Vehicle> vehicles;
    private int activeIndex;

    // ── Descuentos ────────────────────────────────────────────────────────────
    private LinearLayout discountListContainer;
    private List<Discount> discounts;

    // ── Alertas ───────────────────────────────────────────────────────────────
    private LinearLayout alertListContainer;

    // ── Radio de búsqueda ─────────────────────────────────────────────────────
    private SeekBar seekBarRadius;
    private TextView tvRadiusValue;

    // ── Gasolineras en mapa ───────────────────────────────────────────────────
    private SeekBar seekBarMarkers;
    private TextView tvMarkersValue;

    // ── Marcas disponibles ────────────────────────────────────────────────────
    private static final String[] BRANDS = {
            "Repsol", "Cepsa", "Moeve", "BP", "Shell", "Galp",
            "Petronor", "Carrefour", "Alcampo", "Avia",
            "Ballenoil", "Petroprix", "Plenergy"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        vehicleListContainer  = findViewById(R.id.vehicleListContainer);
        discountListContainer = findViewById(R.id.discountListContainer);
        alertListContainer    = findViewById(R.id.alertListContainer);

        findViewById(R.id.btnAddVehicle).setOnClickListener(v -> showVehicleDialog(-1, null, false));
        findViewById(R.id.btnAddDiscount).setOnClickListener(v -> showDiscountDialog(-1, null));

        setupRadiusSelector();
        setupBottomNav();
        setupBackPress();
        setupMarkersSelector();
        setupAlertTestButton();
        refreshVehicleList();
        refreshDiscountList();
        refreshAlertList();
    }

    // ─── Marcadores ───────────────────────────────────────────────────────────

    private void setupMarkersSelector() {
        seekBarMarkers = findViewById(R.id.seekBarMarkers);
        tvMarkersValue = findViewById(R.id.tvMarkersValue);

        int saved = RadiusUtils.loadMarkersCount(this);
        seekBarMarkers.setProgress(saved - RadiusUtils.MIN_MARKERS);
        tvMarkersValue.setText(String.valueOf(saved));

        seekBarMarkers.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvMarkersValue.setText(String.valueOf(progress + RadiusUtils.MIN_MARKERS));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                RadiusUtils.saveMarkersCount(PreferencesActivity.this,
                        seekBar.getProgress() + RadiusUtils.MIN_MARKERS);
            }
        });
    }

    // ─── Radio ────────────────────────────────────────────────────────────────

    private void setupRadiusSelector() {
        seekBarRadius = findViewById(R.id.seekBarRadius);
        tvRadiusValue = findViewById(R.id.tvRadiusValue);

        int savedKm = RadiusUtils.loadRadiusKm(this);
        applyRadius(savedKm, false);

        seekBarRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateRadiusLabel(progress + RadiusUtils.MIN_KM);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                RadiusUtils.saveRadiusKm(PreferencesActivity.this,
                        seekBar.getProgress() + RadiusUtils.MIN_KM);
            }
        });
    }

    private void applyRadius(int km, boolean save) {
        int clamped = Math.max(RadiusUtils.MIN_KM, Math.min(RadiusUtils.MAX_KM, km));
        seekBarRadius.setProgress(clamped - RadiusUtils.MIN_KM);
        updateRadiusLabel(clamped);
        if (save) RadiusUtils.saveRadiusKm(this, clamped);
    }

    private void updateRadiusLabel(int km) {
        tvRadiusValue.setText(String.format(Locale.getDefault(), "%d km", km));
    }

    // ─── Navegación ───────────────────────────────────────────────────────────

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavPrefs);
        bottomNav.setSelectedItemId(R.id.nav_preferences);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_preferences) return true;
            else if (id == R.id.nav_map) {
                if (!VehiclePrefs.hasVehicles(this)) {
                    Toast.makeText(this, "Añade al menos un vehículo para continuar.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                navigateToMap();
                return true;
            } else if (id == R.id.nav_distance) {
                navigateToDistanceList();
                return true;
            } else if (id == R.id.nav_favorites) {
                navigateToFavorites();
                return true;
            } else if (id == R.id.nav_price) {
                navigateToPrice();
                return true;
            }
            return false;
        });
        FuelType fuel = FuelType.fromString(
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
                        .getString("pref_selected_fuel", FuelType.GASOLEO_A.name()));
        if (fuel == FuelType.ELECTRICO) {
            bottomNav.getMenu().findItem(R.id.nav_price).setTitle("Por potencia");
        }
    }

    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!VehiclePrefs.hasVehicles(PreferencesActivity.this)) {
                    Toast.makeText(PreferencesActivity.this,
                            "Añade al menos un vehículo para continuar.", Toast.LENGTH_SHORT).show();
                } else {
                    finish();
                }
            }
        });
    }

    // ─── Test de notificaciones ───────────────────────────────────────────────

    private void setupAlertTestButton() {
        findViewById(R.id.btnTestAlerts).setOnClickListener(v -> runAlertTest());
    }

    /**
     * Lanza el worker de alertas de precio en modo prueba, ignorando el cooldown
     * de 24 horas, para verificar que las notificaciones funcionan correctamente.
     */
    private void runAlertTest() {
        List<PriceAlert> alerts = PriceAlertPrefs.loadAll(this);
        if (alerts.isEmpty()) {
            Toast.makeText(this, "No tienes alertas configuradas.", Toast.LENGTH_SHORT).show();
            return;
        }

        Data inputData = new Data.Builder()
                .putBoolean(PriceAlertWorker.KEY_IS_TEST, true)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PriceAlertWorker.class)
                .setInputData(inputData)
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build();

        WorkManager.getInstance(this).enqueue(request);
        Toast.makeText(this,
                "🔔 Comprobando precios… Si alguno baja de tu alerta, te avisaremos.",
                Toast.LENGTH_LONG).show();
    }

    // ─── UI Alertas ───────────────────────────────────────────────────────────

    private void refreshAlertList() {
        List<PriceAlert> alerts = PriceAlertPrefs.loadAll(this);
        alertListContainer.removeAllViews();

        if (alerts.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Sin alertas. Créalas desde tus favoritas.");
            empty.setTextColor(0xFFAAAAAA);
            empty.setPadding(dp(16), dp(24), dp(16), dp(8));
            alertListContainer.addView(empty);
            return;
        }

        for (PriceAlert alert : alerts) {
            addAlertRow(alert);
        }
    }

    private void addAlertRow(PriceAlert alert) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundColor(0xFF383848);
        card.setPadding(dp(16), dp(14), dp(12), dp(14));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, dp(4), 0, dp(4));
        card.setLayoutParams(cardParams);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(this);
        tvName.setText(alert.getGasolineraName());
        tvName.setTextColor(0xFFFFFFFF);
        tvName.setTextSize(15);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        textCol.addView(tvName);

        TextView tvDetail = new TextView(this);
        tvDetail.setText(alert.getFuelType().displayName()
                + " · alerta ≤ "
                + String.format(Locale.getDefault(), "%.3f €/L", alert.getTargetPrice()));
        tvDetail.setTextColor(0xFFAAAAAA);
        tvDetail.setTextSize(12);
        textCol.addView(tvDetail);

        card.addView(textCol);

        TextView btnDelete = makeTextButton("🗑", 0xFFEF5350);
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Eliminar alerta")
                    .setMessage("¿Eliminar la alerta de " + alert.getGasolineraName() + "?")
                    .setPositiveButton("Eliminar", (d, w) -> {
                        PriceAlertPrefs.remove(this, alert.getKey());
                        refreshAlertList();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
        card.addView(btnDelete);

        alertListContainer.addView(card);

        View sep = new View(this);
        sep.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        sep.setBackgroundColor(0xFF444455);
        alertListContainer.addView(sep);
    }

    // ─── UI Vehículos ─────────────────────────────────────────────────────────

    private void refreshVehicleList() {
        vehicles    = VehiclePrefs.loadVehicles(this);
        activeIndex = VehiclePrefs.loadActiveIndex(this);

        vehicleListContainer.removeAllViews();

        if (vehicles.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Sin vehículos. Pulsa + para añadir uno.");
            empty.setTextColor(0xFFAAAAAA);
            empty.setPadding(dp(16), dp(24), dp(16), dp(8));
            vehicleListContainer.addView(empty);
        }

        for (int i = 0; i < vehicles.size(); i++) {
            addVehicleRow(i, vehicles.get(i));
        }

        findViewById(R.id.btnAddVehicle).setVisibility(
                vehicles.size() < VehiclePrefs.MAX_VEHICLES ? View.VISIBLE : View.GONE
        );
    }

    private void addVehicleRow(int index, Vehicle vehicle) {
        boolean isActive = (index == activeIndex);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundColor(isActive ? 0xFF1A237E : 0xFF383848);
        card.setPadding(dp(16), dp(14), dp(12), dp(14));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, dp(4), 0, dp(4));
        card.setLayoutParams(cardParams);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(this);
        tvName.setText((isActive ? "✔ " : "") + vehicle.getName());
        tvName.setTextColor(0xFFFFFFFF);
        tvName.setTextSize(15);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        textCol.addView(tvName);

        boolean isEv = vehicle.isElectric();
        String consDetail = vehicle.hasConsumption()
                ? String.format(Locale.getDefault(), isEv ? "%.1f kWh/100km" : "%.1f L/100km", vehicle.getConsumption())
                : "consumo no especificado";
        String tankDetail = vehicle.hasTankCapacity()
                ? String.format(Locale.getDefault(), isEv ? " · %.0f kWh batería" : " · %.0f L depósito", vehicle.getTankCapacity())
                : "";

        TextView tvDetail = new TextView(this);
        tvDetail.setText(vehicle.getFuelType().displayName() + " · " + consDetail + tankDetail);
        tvDetail.setTextColor(0xFFAAAAAA);
        tvDetail.setTextSize(12);
        textCol.addView(tvDetail);

        card.addView(textCol);

        TextView btnEdit = makeTextButton("✏", 0xFF4DB6AC);
        btnEdit.setOnClickListener(v -> showVehicleDialog(index, vehicle, false));
        card.addView(btnEdit);

        TextView btnDelete = makeTextButton("🗑", 0xFFEF5350);
        btnDelete.setOnClickListener(v -> confirmDeleteVehicle(index));
        card.addView(btnDelete);

        card.setClickable(true);
        card.setFocusable(true);
        final int finalIndex = index;
        card.setOnClickListener(v -> {
            VehiclePrefs.saveActiveIndex(this, finalIndex);
            activeIndex = finalIndex;
            refreshVehicleList();

            BottomNavigationView bottomNav = findViewById(R.id.bottomNavPrefs);
            FuelType fuel = vehicles.get(finalIndex).getFuelType();
            bottomNav.getMenu().findItem(R.id.nav_price).setTitle(
                    fuel == FuelType.ELECTRICO ? "Por potencia" : "Por precio");
        });

        vehicleListContainer.addView(card);

        View sep = new View(this);
        sep.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        sep.setBackgroundColor(0xFF444455);
        vehicleListContainer.addView(sep);
    }

    // ─── UI Descuentos ────────────────────────────────────────────────────────

    private void refreshDiscountList() {
        discounts = DiscountPrefs.loadDiscounts(this);
        discountListContainer.removeAllViews();

        if (discounts.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Sin descuentos. Pulsa + para añadir uno.");
            empty.setTextColor(0xFFAAAAAA);
            empty.setPadding(dp(16), dp(24), dp(16), dp(8));
            discountListContainer.addView(empty);
        }

        for (int i = 0; i < discounts.size(); i++) {
            addDiscountRow(i, discounts.get(i));
        }

        findViewById(R.id.btnAddDiscount).setVisibility(
                discounts.size() < DiscountPrefs.MAX_DISCOUNTS ? View.VISIBLE : View.GONE
        );
    }

    private void addDiscountRow(int index, Discount discount) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundColor(0xFF383848);
        card.setPadding(dp(16), dp(14), dp(12), dp(14));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, dp(4), 0, dp(4));
        card.setLayoutParams(cardParams);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(this);
        tvName.setText(discount.getBrandName());
        tvName.setTextColor(0xFFFFFFFF);
        tvName.setTextSize(15);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        textCol.addView(tvName);

        String typeLabel = discount.getType() == Discount.Type.PERCENTAGE
                ? String.format(Locale.getDefault(), "%.1f%%", discount.getValue())
                : String.format(Locale.getDefault(), "%.0f cts/L", discount.getValue());

        TextView tvDetail = new TextView(this);
        tvDetail.setText("Descuento: " + typeLabel);
        tvDetail.setTextColor(0xFFAAAAAA);
        tvDetail.setTextSize(12);
        textCol.addView(tvDetail);

        card.addView(textCol);

        TextView btnEdit = makeTextButton("✏", 0xFF4DB6AC);
        btnEdit.setOnClickListener(v -> showDiscountDialog(index, discount));
        card.addView(btnEdit);

        TextView btnDelete = makeTextButton("🗑", 0xFFEF5350);
        btnDelete.setOnClickListener(v -> confirmDeleteDiscount(index));
        card.addView(btnDelete);

        discountListContainer.addView(card);

        View sep = new View(this);
        sep.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        sep.setBackgroundColor(0xFF444455);
        discountListContainer.addView(sep);
    }

    // ─── Diálogo Vehículo ─────────────────────────────────────────────────────

    private void showVehicleDialog(int index, Vehicle existing, boolean mandatory) {
        boolean isNew = (index == -1);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(12), dp(20), dp(4));

        TextView labelName = new TextView(this);
        labelName.setText("Nombre del vehículo *");
        labelName.setTextColor(0xFFCCCCCC);
        labelName.setTextSize(13);
        layout.addView(labelName);

        EditText etName = new EditText(this);
        etName.setHint("Ej: Mi Coche, Furgoneta…");
        etName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        if (existing != null) etName.setText(existing.getName());
        layout.addView(etName);

        TextView tvNameError = makeErrorLabel();
        layout.addView(tvNameError);

        TextView labelCons = new TextView(this);
        labelCons.setText("Consumo (L/100 km)  · opcional");
        labelCons.setTextColor(0xFFCCCCCC);
        labelCons.setTextSize(13);
        labelCons.setPadding(0, dp(12), 0, 0);
        layout.addView(labelCons);

        EditText etCons = new EditText(this);
        etCons.setHint("Ej: 6.5  (entre 0.1 y 100)");
        etCons.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (existing != null && existing.hasConsumption()) {
            etCons.setText(String.format(Locale.getDefault(), "%.1f", existing.getConsumption()));
        }
        layout.addView(etCons);

        TextView tvConsError = makeErrorLabel();
        layout.addView(tvConsError);

        TextView labelTank = new TextView(this);
        labelTank.setText("Capacidad depósito (L)  · opcional");
        labelTank.setTextColor(0xFFCCCCCC);
        labelTank.setTextSize(13);
        labelTank.setPadding(0, dp(12), 0, 0);
        layout.addView(labelTank);

        EditText etTank = new EditText(this);
        etTank.setHint("Ej: 50  (entre 1 y 200)");
        etTank.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (existing != null && existing.hasTankCapacity()) {
            etTank.setText(String.format(Locale.getDefault(), "%.0f", existing.getTankCapacity()));
        }
        layout.addView(etTank);

        TextView tvTankError = makeErrorLabel();
        layout.addView(tvTankError);

        // ── Campo potencia de carga (solo eléctricos) ─────────────────────────
        TextView labelCharging = new TextView(this);
        labelCharging.setText("Potencia de carga (kW)  · opcional");
        labelCharging.setTextColor(0xFFCCCCCC);
        labelCharging.setTextSize(13);
        labelCharging.setPadding(0, dp(12), 0, 0);
        layout.addView(labelCharging);

        EditText etCharging = new EditText(this);
        etCharging.setHint("Ej: 11  (entre 1 y 500)");
        etCharging.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (existing != null && existing.hasChargingPower()) {
            etCharging.setText(String.format(Locale.getDefault(), "%.0f", existing.getChargingPowerKw()));
        }
        layout.addView(etCharging);

        TextView tvChargingError = makeErrorLabel();
        layout.addView(tvChargingError);

        // Visibilidad inicial: solo si es eléctrico
        boolean initElectric = (existing != null && existing.isElectric())
                || (existing == null && FuelType.GASOLEO_A == FuelType.ELECTRICO);
        labelCharging.setVisibility(initElectric ? View.VISIBLE : View.GONE);
        etCharging.setVisibility(initElectric ? View.VISIBLE : View.GONE);

        if (initElectric) {
            labelCons.setText("Consumo (kWh/100 km)  · opcional");
            etCons.setHint("Ej: 18  (entre 0.1 y 100)");
            labelTank.setText("Batería (kWh)  · opcional");
            etTank.setHint("Ej: 60  (entre 1 y 200)");
        }

        TextView labelFuel = new TextView(this);
        labelFuel.setText("Tipo de combustible *");
        labelFuel.setTextColor(0xFFCCCCCC);
        labelFuel.setTextSize(13);
        labelFuel.setPadding(0, dp(12), 0, 0);
        layout.addView(labelFuel);

        FuelType[] fuels = FuelType.values();
        String[] fuelNames = new String[fuels.length];
        for (int i = 0; i < fuels.length; i++) fuelNames[i] = fuels[i].displayName();

        FuelType initialFuel = existing != null ? existing.getFuelType() : FuelType.GASOLEO_A;
        final FuelType[] selectedFuel = {initialFuel};

        TextView tvFuelSelector = new TextView(this);
        tvFuelSelector.setText(selectedFuel[0].displayName());
        tvFuelSelector.setTextColor(0xFFFFFFFF);
        tvFuelSelector.setBackgroundColor(0xFF2C2C3A);
        tvFuelSelector.setPadding(dp(12), dp(10), dp(12), dp(10));
        tvFuelSelector.setTextSize(14);
        tvFuelSelector.setClickable(true);
        tvFuelSelector.setFocusable(true);
        tvFuelSelector.setOnClickListener(v -> {
            int checked = 0;
            for (int i = 0; i < fuels.length; i++) {
                if (fuels[i] == selectedFuel[0]) { checked = i; break; }
            }
            new AlertDialog.Builder(this)
                    .setTitle("Combustible")
                    .setSingleChoiceItems(fuelNames, checked, (d, which) -> {
                        selectedFuel[0] = fuels[which];
                        tvFuelSelector.setText(selectedFuel[0].displayName());
                        boolean isEv = (selectedFuel[0] == FuelType.ELECTRICO);
                        labelCons.setText(isEv ? "Consumo (kWh/100 km)  · opcional" : "Consumo (L/100 km)  · opcional");
                        etCons.setHint(isEv ? "Ej: 18  (entre 0.1 y 100)" : "Ej: 6.5  (entre 0.1 y 100)");
                        labelTank.setText(isEv ? "Batería (kWh)  · opcional" : "Capacidad depósito (L)  · opcional");
                        etTank.setHint(isEv ? "Ej: 60  (entre 1 y 200)" : "Ej: 50  (entre 1 y 200)");
                        labelCharging.setVisibility(isEv ? View.VISIBLE : View.GONE);
                        etCharging.setVisibility(isEv ? View.VISIBLE : View.GONE);
                        d.dismiss();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
        layout.addView(tvFuelSelector);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(mandatory ? "Añade tu vehículo para empezar"
                        : (isNew ? "Añadir vehículo" : "Editar vehículo"))
                .setView(layout)
                .setPositiveButton("Guardar", null);

        if (mandatory) builder.setCancelable(false);
        else builder.setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean hasError = false;

            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                tvNameError.setText("El nombre es obligatorio.");
                tvNameError.setVisibility(View.VISIBLE);
                hasError = true;
            } else {
                tvNameError.setVisibility(View.GONE);
            }

            String consStr = etCons.getText().toString().trim().replace(",", ".");
            double cons = 0.0;
            if (!consStr.isEmpty()) {
                try {
                    cons = Double.parseDouble(consStr);
                    if (cons <= 0 || cons > 100) throw new NumberFormatException();
                    tvConsError.setVisibility(View.GONE);
                } catch (Exception e) {
                    tvConsError.setText("Valor no válido. Introduce un número entre 0.1 y 100.");
                    tvConsError.setVisibility(View.VISIBLE);
                    hasError = true;
                }
            } else {
                tvConsError.setVisibility(View.GONE);
            }

            String tankStr = etTank.getText().toString().trim().replace(",", ".");
            double tank = 0.0;
            if (!tankStr.isEmpty()) {
                try {
                    tank = Double.parseDouble(tankStr);
                    if (tank <= 0 || tank > 200) throw new NumberFormatException();
                    tvTankError.setVisibility(View.GONE);
                } catch (Exception e) {
                    tvTankError.setText("Valor no válido. Introduce un número entre 1 y 200.");
                    tvTankError.setVisibility(View.VISIBLE);
                    hasError = true;
                }
            } else {
                tvTankError.setVisibility(View.GONE);
            }

            String chargingStr = etCharging.getText().toString().trim().replace(",", ".");
            double chargingPower = 0.0;
            if (selectedFuel[0] == FuelType.ELECTRICO && !chargingStr.isEmpty()) {
                try {
                    chargingPower = Double.parseDouble(chargingStr);
                    if (chargingPower <= 0 || chargingPower > 500) throw new NumberFormatException();
                    tvChargingError.setVisibility(View.GONE);
                } catch (Exception e) {
                    tvChargingError.setText("Valor no válido. Introduce un número entre 1 y 500.");
                    tvChargingError.setVisibility(View.VISIBLE);
                    hasError = true;
                }
            } else {
                tvChargingError.setVisibility(View.GONE);
            }

            if (hasError) return;

            Vehicle vehicle = new Vehicle(name, selectedFuel[0], cons, tank, chargingPower);
            if (isNew) {
                boolean added = VehiclePrefs.addVehicle(this, vehicle);
                if (!added) {
                    tvNameError.setText("Ya tienes el máximo de 10 vehículos.");
                    tvNameError.setVisibility(View.VISIBLE);
                    return;
                }
            } else {
                VehiclePrefs.updateVehicle(this, index, vehicle);
            }

            dialog.dismiss();
            refreshVehicleList();
        });
    }

    // ─── Diálogo Descuento ────────────────────────────────────────────────────

    private void showDiscountDialog(int index, Discount existing) {
        boolean isNew = (index == -1);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(12), dp(20), dp(4));

        TextView labelBrand = new TextView(this);
        labelBrand.setText("Marca de gasolinera *");
        labelBrand.setTextColor(0xFFCCCCCC);
        labelBrand.setTextSize(13);
        layout.addView(labelBrand);

        final String[] selectedBrand = {existing != null ? existing.getBrandName() : BRANDS[0]};

        TextView tvBrandSelector = new TextView(this);
        tvBrandSelector.setText(selectedBrand[0]);
        tvBrandSelector.setTextColor(0xFFFFFFFF);
        tvBrandSelector.setBackgroundColor(0xFF2C2C3A);
        tvBrandSelector.setPadding(dp(12), dp(10), dp(12), dp(10));
        tvBrandSelector.setTextSize(14);
        tvBrandSelector.setClickable(true);
        tvBrandSelector.setFocusable(true);
        tvBrandSelector.setOnClickListener(v -> {
            int checked = 0;
            for (int i = 0; i < BRANDS.length; i++) {
                if (BRANDS[i].equals(selectedBrand[0])) { checked = i; break; }
            }
            new AlertDialog.Builder(this)
                    .setTitle("Selecciona la marca")
                    .setSingleChoiceItems(BRANDS, checked, (d, which) -> {
                        selectedBrand[0] = BRANDS[which];
                        tvBrandSelector.setText(selectedBrand[0]);
                        d.dismiss();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
        layout.addView(tvBrandSelector);

        TextView tvBrandError = makeErrorLabel();
        layout.addView(tvBrandError);

        TextView labelType = new TextView(this);
        labelType.setText("Tipo de descuento *");
        labelType.setTextColor(0xFFCCCCCC);
        labelType.setTextSize(13);
        labelType.setPadding(0, dp(12), 0, 0);
        layout.addView(labelType);

        final Discount.Type[] selectedType = {
                existing != null ? existing.getType() : Discount.Type.CENTS_PER_LITER
        };
        final String[] typeNames = {"Céntimos por litro", "Porcentaje (%)"};
        final Discount.Type[] typeValues = {Discount.Type.CENTS_PER_LITER, Discount.Type.PERCENTAGE};

        TextView tvTypeSelector = new TextView(this);
        tvTypeSelector.setText(selectedType[0] == Discount.Type.PERCENTAGE ? typeNames[1] : typeNames[0]);
        tvTypeSelector.setTextColor(0xFFFFFFFF);
        tvTypeSelector.setBackgroundColor(0xFF2C2C3A);
        tvTypeSelector.setPadding(dp(12), dp(10), dp(12), dp(10));
        tvTypeSelector.setTextSize(14);
        tvTypeSelector.setClickable(true);
        tvTypeSelector.setFocusable(true);
        tvTypeSelector.setOnClickListener(v -> {
            int checked = selectedType[0] == Discount.Type.PERCENTAGE ? 1 : 0;
            new AlertDialog.Builder(this)
                    .setTitle("Tipo de descuento")
                    .setSingleChoiceItems(typeNames, checked, (d, which) -> {
                        selectedType[0] = typeValues[which];
                        tvTypeSelector.setText(typeNames[which]);
                        d.dismiss();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
        layout.addView(tvTypeSelector);

        TextView labelValue = new TextView(this);
        labelValue.setText("Valor del descuento *");
        labelValue.setTextColor(0xFFCCCCCC);
        labelValue.setTextSize(13);
        labelValue.setPadding(0, dp(12), 0, 0);
        layout.addView(labelValue);

        EditText etValue = new EditText(this);
        etValue.setHint("Ej: 6 para 6 cts/L  ó  5 para 5%");
        etValue.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (existing != null) {
            if (existing.getType() == Discount.Type.CENTS_PER_LITER) {
                etValue.setText(String.format(Locale.getDefault(), "%.0f", existing.getValue()));
            } else {
                etValue.setText(String.format(Locale.getDefault(), "%.1f", existing.getValue()));
            }
        }
        layout.addView(etValue);

        TextView tvValueError = makeErrorLabel();
        layout.addView(tvValueError);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(isNew ? "Añadir descuento" : "Editar descuento")
                .setView(layout)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean hasError = false;

            String valueStr = etValue.getText().toString().trim().replace(",", ".");
            double value = 0.0;
            if (valueStr.isEmpty()) {
                tvValueError.setText("El valor es obligatorio.");
                tvValueError.setVisibility(View.VISIBLE);
                hasError = true;
            } else {
                try {
                    value = Double.parseDouble(valueStr);
                    if (value <= 0) throw new NumberFormatException();
                    if (selectedType[0] == Discount.Type.PERCENTAGE && value > 100)
                        throw new NumberFormatException();
                    if (selectedType[0] == Discount.Type.CENTS_PER_LITER && value > 50)
                        throw new NumberFormatException();
                    tvValueError.setVisibility(View.GONE);
                } catch (Exception e) {
                    tvValueError.setText(selectedType[0] == Discount.Type.PERCENTAGE
                            ? "Introduce un porcentaje entre 0.1 y 100."
                            : "Introduce céntimos entre 0.1 y 50.");
                    tvValueError.setVisibility(View.VISIBLE);
                    hasError = true;
                }
            }

            if (hasError) return;

            Discount discount = new Discount(selectedBrand[0], selectedType[0], value);
            if (isNew) {
                boolean added = DiscountPrefs.addDiscount(this, discount);
                if (!added) {
                    tvValueError.setText("Has alcanzado el máximo de descuentos.");
                    tvValueError.setVisibility(View.VISIBLE);
                    return;
                }
            } else {
                DiscountPrefs.updateDiscount(this, index, discount);
            }

            dialog.dismiss();
            refreshDiscountList();
        });
    }

    // ─── Confirmaciones borrado ───────────────────────────────────────────────

    private void confirmDeleteVehicle(int index) {
        boolean isLast = vehicles.size() == 1;
        new AlertDialog.Builder(this)
                .setTitle("Eliminar vehículo")
                .setMessage(isLast
                        ? "Es el único vehículo. Si lo eliminas tendrás que añadir uno nuevo antes de usar la app."
                        : "¿Seguro que quieres eliminar \"" + vehicles.get(index).getName() + "\"?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    VehiclePrefs.deleteVehicle(this, index);
                    refreshVehicleList();
                    if (isLast) showVehicleDialog(-1, null, true);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmDeleteDiscount(int index) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar descuento")
                .setMessage("¿Seguro que quieres eliminar el descuento de \""
                        + discounts.get(index).getBrandName() + "\"?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    DiscountPrefs.deleteDiscount(this, index);
                    refreshDiscountList();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ─── Utilidades UI ────────────────────────────────────────────────────────

    private TextView makeErrorLabel() {
        TextView tv = new TextView(this);
        tv.setTextColor(0xFFEF5350);
        tv.setTextSize(12);
        tv.setPadding(dp(2), dp(2), 0, 0);
        tv.setVisibility(View.GONE);
        return tv;
    }

    private TextView makeTextButton(String emoji, int color) {
        TextView tv = new TextView(this);
        tv.setText(emoji);
        tv.setTextSize(18);
        tv.setTextColor(color);
        tv.setPadding(dp(10), dp(4), dp(4), dp(4));
        tv.setClickable(true);
        tv.setFocusable(true);
        return tv;
    }

    private int dp(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(density * dp);
    }
}