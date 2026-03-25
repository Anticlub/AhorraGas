package com.example.ahorragas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.example.ahorragas.model.FuelType;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class PreferencesActivity extends AppCompatActivity {

    private static final String PREF_SELECTED_FUEL = "pref_selected_fuel";
    private TextView tvCurrentFuel;
    private FuelType selectedFuel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        selectedFuel = FuelType.fromString(
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getString(PREF_SELECTED_FUEL, FuelType.GASOLEO_A.name())
        );

        tvCurrentFuel = findViewById(R.id.tvCurrentFuel);
        tvCurrentFuel.setText(selectedFuel.displayName());

        LinearLayout itemFuelType = findViewById(R.id.itemFuelType);
        itemFuelType.setOnClickListener(v -> showFuelSelector());

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavPrefs);
        bottomNav.setSelectedItemId(R.id.nav_preferences);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_preferences) {
                return true;
            } else if (id == R.id.nav_map) {
                finish();
                return true;
            } else {
                return false;
            }
        });
    }

    private void showFuelSelector() {
        FuelType[] fuels = FuelType.values();
        String[] names = new String[fuels.length];
        int checkedIndex = 0;

        for (int i = 0; i < fuels.length; i++) {
            names[i] = fuels[i].displayName();
            if (fuels[i] == selectedFuel) checkedIndex = i;
        }

        new AlertDialog.Builder(this)
                .setTitle("Tipo de combustible")
                .setSingleChoiceItems(names, checkedIndex, (dialog, which) -> {
                    selectedFuel = fuels[which];
                    tvCurrentFuel.setText(selectedFuel.displayName());

                    PreferenceManager.getDefaultSharedPreferences(this)
                            .edit()
                            .putString(PREF_SELECTED_FUEL, selectedFuel.name())
                            .apply();

                    dialog.dismiss();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
