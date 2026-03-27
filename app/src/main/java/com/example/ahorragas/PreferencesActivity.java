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
import androidx.appcompat.app.AppCompatActivity;

import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Vehicle;
import com.example.ahorragas.util.RadiusUtils;
import com.example.ahorragas.util.VehiclePrefs;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Locale;

public class PreferencesActivity extends AppCompatActivity {

    private LinearLayout vehicleListContainer;
    private FloatingActionButton fabAddVehicle;
    private List<Vehicle> vehicles;
    private int activeIndex;

    // ── Radio de búsqueda ────────────────────────────────────────────────────
    private SeekBar seekBarRadius;
    private TextView tvRadiusValue;

    // ── Gasolineras en mapa ────────────────────────────────────────────────────
    private SeekBar seekBarMarkers;
    private TextView tvMarkersValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        vehicleListContainer = findViewById(R.id.vehicleListContainer);
        fabAddVehicle        = findViewById(R.id.fabAddVehicle);

        fabAddVehicle.setOnClickListener(v -> showVehicleDialog(-1, null, false));

        setupRadiusSelector();
        setupBottomNav();
        setupBackPress();
        setupMarkersSelector();
        refreshVehicleList();
    }

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

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                RadiusUtils.saveMarkersCount(PreferencesActivity.this,
                        seekBar.getProgress() + RadiusUtils.MIN_MARKERS);
            }
        });
    }
    // ─── Radio de búsqueda ────────────────────────────────────────────────────

    /**
     * Inicializa el SeekBar y el TextView del radio.
     * El SeekBar va de 0 a 49 (offset de 1) → representa 1–50 km.
     */
    private void setupRadiusSelector() {
        seekBarRadius  = findViewById(R.id.seekBarRadius);
        tvRadiusValue  = findViewById(R.id.tvRadiusValue);

        int savedKm = RadiusUtils.loadRadiusKm(this);
        applyRadius(savedKm, false);   // actualiza label sin guardar de nuevo

        seekBarRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int km = progress + RadiusUtils.MIN_KM;   // progress 0..49 → 1..50 km
                updateRadiusLabel(km);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { /* no-op */ }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int km = seekBar.getProgress() + RadiusUtils.MIN_KM;
                RadiusUtils.saveRadiusKm(PreferencesActivity.this, km);
            }
        });
    }

    /**
     * Aplica un valor de radio al SeekBar y al label.
     * @param km     valor en kilómetros (1–50)
     * @param save   si true, persiste el valor en SharedPreferences
     */
    private void applyRadius(int km, boolean save) {
        int clamped = Math.max(RadiusUtils.MIN_KM, Math.min(RadiusUtils.MAX_KM, km));
        seekBarRadius.setProgress(clamped - RadiusUtils.MIN_KM);
        updateRadiusLabel(clamped);
        if (save) {
            RadiusUtils.saveRadiusKm(this, clamped);
        }
    }

    private void updateRadiusLabel(int km) {
        tvRadiusValue.setText(String.format(Locale.getDefault(), "%d km", km));
    }

    // ─── Navegación y back press ──────────────────────────────────────────────

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavPrefs);
        bottomNav.setSelectedItemId(R.id.nav_preferences);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_preferences) return true;
            if (id == R.id.nav_map) {
                if (!VehiclePrefs.hasVehicles(this)) {
                    Toast.makeText(this,
                            "Añade al menos un vehículo para continuar.",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                finish();
                return true;
            }
            return false;
        });
    }

    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!VehiclePrefs.hasVehicles(PreferencesActivity.this)) {
                    Toast.makeText(PreferencesActivity.this,
                            "Añade al menos un vehículo para continuar.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    finish();
                }
            }
        });
    }

    // ─── UI de vehículos ──────────────────────────────────────────────────────

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

        fabAddVehicle.setVisibility(
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
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textCol.setLayoutParams(textParams);

        TextView tvName = new TextView(this);
        tvName.setText((isActive ? "✔ " : "") + vehicle.getName());
        tvName.setTextColor(0xFFFFFFFF);
        tvName.setTextSize(15);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        textCol.addView(tvName);

        // Detalle: combustible + consumo (o "sin consumo" si no está especificado)
        String consDetail = vehicle.hasConsumption()
                ? String.format(Locale.getDefault(), "%.1f L/100km", vehicle.getConsumption())
                : "consumo no especificado";
        TextView tvDetail = new TextView(this);
        tvDetail.setText(vehicle.getFuelType().displayName() + " · " + consDetail);
        tvDetail.setTextColor(0xFFAAAAAA);
        tvDetail.setTextSize(12);
        textCol.addView(tvDetail);

        card.addView(textCol);

        TextView btnEdit = makeTextButton("✏", 0xFF4DB6AC);
        btnEdit.setOnClickListener(v -> showVehicleDialog(index, vehicle, false));
        card.addView(btnEdit);

        TextView btnDelete = makeTextButton("🗑", 0xFFEF5350);
        btnDelete.setOnClickListener(v -> confirmDelete(index));
        card.addView(btnDelete);

        card.setClickable(true);
        card.setFocusable(true);
        final int finalIndex = index;
        card.setOnClickListener(v -> {
            VehiclePrefs.saveActiveIndex(this, finalIndex);
            activeIndex = finalIndex;
            refreshVehicleList();
        });

        vehicleListContainer.addView(card);

        View sep = new View(this);
        sep.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        sep.setBackgroundColor(0xFF444455);
        vehicleListContainer.addView(sep);
    }

    // ─── Diálogos vehículos ───────────────────────────────────────────────────

    private void showVehicleDialog(int index, Vehicle existing, boolean mandatory) {
        boolean isNew = (index == -1);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(12), dp(20), dp(4));

        // ── Nombre ──────────────────────────────────────────────────────────
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

        // ── Consumo (opcional) ───────────────────────────────────────────────
        TextView labelCons = new TextView(this);
        labelCons.setText("Consumo (L/100 km)  · opcional");
        labelCons.setTextColor(0xFFCCCCCC);
        labelCons.setTextSize(13);
        labelCons.setPadding(0, dp(12), 0, 0);
        layout.addView(labelCons);

        EditText etCons = new EditText(this);
        etCons.setHint("Ej: 6.5  (entre 0.1 y 100)");
        etCons.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        // Solo pre-rellenar si el vehículo tiene consumo especificado (> 0)
        if (existing != null && existing.hasConsumption()) {
            etCons.setText(String.format(Locale.getDefault(), "%.1f", existing.getConsumption()));
        }
        layout.addView(etCons);

        TextView tvConsError = makeErrorLabel();
        layout.addView(tvConsError);

        // ── Combustible ──────────────────────────────────────────────────────
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
                        d.dismiss();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
        layout.addView(tvFuelSelector);

        // ── Construir diálogo ────────────────────────────────────────────────
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(mandatory ? "Añade tu vehículo para empezar"
                        : (isNew ? "Añadir vehículo" : "Editar vehículo"))
                .setView(layout)
                .setPositiveButton("Guardar", null);

        if (mandatory) {
            builder.setCancelable(false);
        } else {
            builder.setNegativeButton("Cancelar", null);
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean hasError = false;

            // Validar nombre (obligatorio)
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                tvNameError.setText("El nombre es obligatorio.");
                tvNameError.setVisibility(View.VISIBLE);
                hasError = true;
            } else {
                tvNameError.setVisibility(View.GONE);
            }

            // Validar consumo (opcional — vacío = 0 = no especificado)
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

            if (hasError) return;

            Vehicle vehicle = new Vehicle(name, selectedFuel[0], cons);

            if (isNew) {
                boolean added = VehiclePrefs.addVehicle(this, vehicle);
                if (!added) {
                    tvNameError.setText("Ya tienes el máximo de 3 vehículos.");
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

    private void confirmDelete(int index) {
        boolean isLast = vehicles.size() == 1;

        new AlertDialog.Builder(this)
                .setTitle("Eliminar vehículo")
                .setMessage(isLast
                        ? "Es el único vehículo. Si lo eliminas tendrás que añadir uno nuevo antes de usar la app."
                        : "¿Seguro que quieres eliminar \"" + vehicles.get(index).getName() + "\"?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    VehiclePrefs.deleteVehicle(this, index);
                    refreshVehicleList();
                    if (isLast) {
                        showVehicleDialog(-1, null, true);
                    }
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