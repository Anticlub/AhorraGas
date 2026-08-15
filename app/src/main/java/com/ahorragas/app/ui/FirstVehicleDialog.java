package com.ahorragas.app.ui;

import android.app.Activity;
import android.content.Context;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.ahorragas.app.R;
import com.ahorragas.app.model.FuelType;
import com.ahorragas.app.model.Vehicle;

/**
 * Diálogo de alta del primer vehículo (se muestra cuando el usuario no tiene
 * ninguno). Construye el formulario, valida los campos y, al guardar, entrega el
 * {@link Vehicle} creado mediante {@link Listener}. La persistencia y la recarga
 * del mapa son responsabilidad de quien lo invoca.
 */
public final class FirstVehicleDialog {

    /** Se invoca cuando el usuario guarda un vehículo válido. */
    public interface Listener {
        void onVehicleCreated(Vehicle vehicle);
    }

    private FirstVehicleDialog() {}

    public static void show(Activity activity, Listener listener) {
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(activity, 20), dp(activity, 12), dp(activity, 20), dp(activity, 4));

        TextView labelName = new TextView(activity);
        labelName.setText(makeRequiredLabel(activity, activity.getString(R.string.label_vehicle_name)));
        labelName.setTextColor(ContextCompat.getColor(activity, R.color.text_dark));
        labelName.setTextSize(13);
        layout.addView(labelName);

        EditText etName = new EditText(activity);
        etName.setHint(activity.getString(R.string.dialogo_vehiculo_nombre_hint));
        etName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        layout.addView(etName);

        TextView labelCons = new TextView(activity);
        labelCons.setText(activity.getString(R.string.dialogo_vehiculo_consumo));
        labelCons.setTextColor(ContextCompat.getColor(activity, R.color.text_dark));
        labelCons.setTextSize(13);
        labelCons.setPadding(0, dp(activity, 12), 0, 0);
        layout.addView(labelCons);

        EditText etCons = new EditText(activity);
        etCons.setHint(activity.getString(R.string.dialogo_vehiculo_consumo_hint));
        etCons.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etCons);

        TextView labelTank = new TextView(activity);
        labelTank.setText(activity.getString(R.string.label_tank_capacity));
        labelTank.setTextColor(ContextCompat.getColor(activity, R.color.text_dark));
        labelTank.setTextSize(13);
        labelTank.setPadding(0, dp(activity, 12), 0, 0);
        layout.addView(labelTank);

        EditText etTank = new EditText(activity);
        etTank.setHint(activity.getString(R.string.hint_tank_capacity));
        etTank.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etTank);

        TextView labelCharging = new TextView(activity);
        labelCharging.setText(activity.getString(R.string.label_charging_power));
        labelCharging.setTextColor(ContextCompat.getColor(activity, R.color.text_dark));
        labelCharging.setTextSize(13);
        labelCharging.setPadding(0, dp(activity, 12), 0, 0);
        labelCharging.setVisibility(View.GONE);
        layout.addView(labelCharging);

        EditText etCharging = new EditText(activity);
        etCharging.setHint(activity.getString(R.string.hint_charging_power));
        etCharging.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etCharging.setVisibility(View.GONE);
        layout.addView(etCharging);

        TextView labelFuel = new TextView(activity);
        labelFuel.setText(makeRequiredLabel(activity, activity.getString(R.string.label_fuel_type)));
        labelFuel.setTextColor(ContextCompat.getColor(activity, R.color.text_dark));
        labelFuel.setTextSize(13);
        labelFuel.setPadding(0, dp(activity, 12), 0, 0);
        layout.addView(labelFuel);

        FuelType[] fuels = FuelType.values();
        String[] fuelNames = new String[fuels.length];
        for (int i = 0; i < fuels.length; i++) fuelNames[i] = fuels[i].displayName();

        final FuelType[] selectedFuel = {FuelType.GASOLEO_A};

        TextView tvFuelSelector = new TextView(activity);
        tvFuelSelector.setText(selectedFuel[0].displayName());
        tvFuelSelector.setTextColor(ContextCompat.getColor(activity, R.color.black));
        tvFuelSelector.setBackgroundColor(ContextCompat.getColor(activity, R.color.input_light));
        tvFuelSelector.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
        tvFuelSelector.setTextSize(14);
        tvFuelSelector.setClickable(true);
        tvFuelSelector.setFocusable(true);
        tvFuelSelector.setOnClickListener(v -> {
            int checked = 0;
            for (int i = 0; i < fuels.length; i++) {
                if (fuels[i] == selectedFuel[0]) { checked = i; break; }
            }
            new AlertDialog.Builder(activity)
                    .setTitle(activity.getString(R.string.dialogo_vehiculo_combustible_titulo))
                    .setSingleChoiceItems(fuelNames, checked, (d, which) -> {
                        selectedFuel[0] = fuels[which];
                        tvFuelSelector.setText(selectedFuel[0].displayName());
                        boolean isEv = (selectedFuel[0] == FuelType.ELECTRICO);
                        labelCons.setText(isEv
                                ? activity.getString(R.string.label_consumption_ev)
                                : activity.getString(R.string.dialogo_vehiculo_consumo));
                        etCons.setHint(isEv
                                ? activity.getString(R.string.hint_consumption_ev)
                                : activity.getString(R.string.dialogo_vehiculo_consumo_hint));
                        labelTank.setText(isEv
                                ? activity.getString(R.string.label_battery_capacity)
                                : activity.getString(R.string.label_tank_capacity));
                        etTank.setHint(isEv
                                ? activity.getString(R.string.hint_battery_capacity)
                                : activity.getString(R.string.hint_tank_capacity));
                        labelCharging.setVisibility(isEv ? View.VISIBLE : View.GONE);
                        etCharging.setVisibility(isEv ? View.VISIBLE : View.GONE);
                        d.dismiss();
                    })
                    .setNegativeButton(activity.getString(R.string.dialogo_vehiculo_cancelar), null)
                    .show();
        });
        layout.addView(tvFuelSelector);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.dialogo_vehiculo_titulo))
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton(activity.getString(R.string.dialogo_vehiculo_guardar), null)
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String consStr = etCons.getText().toString().trim().replace(",", ".");
            String tankStr = etTank.getText().toString().trim().replace(",", ".");
            String chargingStr = etCharging.getText().toString().trim().replace(",", ".");

            if (name.isEmpty()) {
                Toast.makeText(activity, activity.getString(R.string.dialogo_vehiculo_nombre_vacio), Toast.LENGTH_SHORT).show();
                return;
            }

            double cons = 0.0;
            if (!consStr.isEmpty()) {
                try {
                    cons = Double.parseDouble(consStr);
                    if (cons <= 0 || cons > 100) throw new NumberFormatException();
                } catch (Exception e) {
                    Toast.makeText(activity, activity.getString(R.string.dialogo_vehiculo_consumo_invalido), Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            double tank = 0.0;
            if (!tankStr.isEmpty()) {
                try {
                    tank = Double.parseDouble(tankStr);
                    if (tank <= 0 || tank > 200) throw new NumberFormatException();
                } catch (Exception e) {
                    Toast.makeText(activity, activity.getString(R.string.msg_invalid_tank_capacity), Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            double charging = 0.0;
            if (!chargingStr.isEmpty()) {
                try {
                    charging = Double.parseDouble(chargingStr);
                    if (charging <= 0 || charging > 500) throw new NumberFormatException();
                } catch (Exception e) {
                    Toast.makeText(activity, activity.getString(R.string.msg_invalid_charging_power), Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            Vehicle vehicle = new Vehicle(name, selectedFuel[0], cons, tank, charging);
            listener.onVehicleCreated(vehicle);
            dialog.dismiss();
        });
    }

    /** Pinta en rojo el último carácter (el asterisco) para marcar campo obligatorio. */
    private static SpannableString makeRequiredLabel(Context ctx, String text) {
        SpannableString span = new SpannableString(text);
        span.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(ctx, R.color.error_red)),
                span.length() - 1,
                span.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return span;
    }

    private static int dp(Context ctx, int dp) {
        return Math.round(ctx.getResources().getDisplayMetrics().density * dp);
    }
}
