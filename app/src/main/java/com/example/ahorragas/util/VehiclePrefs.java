package com.example.ahorragas.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Vehicle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Persiste hasta MAX_VEHICLES vehículos en SharedPreferences como JSON array.
 * Clave: "pref_vehicles"
 * Índice activo: "pref_active_vehicle" (int, 0-based)
 *
 * El combustible activo de la app ("pref_selected_fuel") se sincroniza siempre
 * con el vehículo activo a través de syncActiveFuel().
 */
public final class VehiclePrefs {

    public static final int MAX_VEHICLES = 3;
    private static final String KEY_VEHICLES     = "pref_vehicles";
    private static final String KEY_ACTIVE       = "pref_active_vehicle";
    private static final String KEY_SELECTED_FUEL = "pref_selected_fuel";

    private VehiclePrefs() {}

    // ─── READ ────────────────────────────────────────────────────────────────

    public static List<Vehicle> loadVehicles(Context ctx) {
        List<Vehicle> list = new ArrayList<>();
        String raw = prefs(ctx).getString(KEY_VEHICLES, null);
        if (raw == null || raw.isEmpty()) return list;

        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String name = obj.optString("name", "Vehículo " + (i + 1));
                String fuel = obj.optString("fuel", FuelType.GASOLEO_A.name());
                double cons = obj.optDouble("consumption", 6.0);
                list.add(new Vehicle(name, FuelType.fromString(fuel), cons));
            }
        } catch (Exception e) {
            android.util.Log.e("VehiclePrefs", "Error leyendo vehículos: " + e.getMessage(), e);
        }

        return list;
    }

    public static int loadActiveIndex(Context ctx) {
        int idx = prefs(ctx).getInt(KEY_ACTIVE, 0);
        List<Vehicle> list = loadVehicles(ctx);
        if (list.isEmpty()) return -1;
        return Math.max(0, Math.min(idx, list.size() - 1));
    }

    /** Devuelve el vehículo activo o null si no hay ninguno guardado. */
    public static Vehicle loadActiveVehicle(Context ctx) {
        List<Vehicle> list = loadVehicles(ctx);
        if (list.isEmpty()) return null;
        int idx = loadActiveIndex(ctx);
        return list.get(idx);
    }

    /** True si hay al menos un vehículo guardado. */
    public static boolean hasVehicles(Context ctx) {
        return !loadVehicles(ctx).isEmpty();
    }

    // ─── WRITE ───────────────────────────────────────────────────────────────

    public static void saveVehicles(Context ctx, List<Vehicle> vehicles) {
        try {
            JSONArray arr = new JSONArray();
            int limit = Math.min(vehicles.size(), MAX_VEHICLES);
            for (int i = 0; i < limit; i++) {
                Vehicle v = vehicles.get(i);
                JSONObject obj = new JSONObject();
                obj.put("name",        v.getName());
                obj.put("fuel",        v.getFuelType().name());
                obj.put("consumption", v.getConsumption());
                arr.put(obj);
            }
            prefs(ctx).edit().putString(KEY_VEHICLES, arr.toString()).apply();
        } catch (Exception e) {
            android.util.Log.e("VehiclePrefs", "Error guardando vehículos: " + e.getMessage(), e);
        }
    }

    public static void saveActiveIndex(Context ctx, int index) {
        prefs(ctx).edit().putInt(KEY_ACTIVE, index).apply();
        syncActiveFuel(ctx);
    }

    /** Añade un vehículo (máx MAX_VEHICLES). Devuelve false si ya está lleno. */
    public static boolean addVehicle(Context ctx, Vehicle vehicle) {
        List<Vehicle> list = loadVehicles(ctx);
        if (list.size() >= MAX_VEHICLES) return false;
        list.add(vehicle);
        saveVehicles(ctx, list);
        // Si es el primero, activarlo y sincronizar combustible
        if (list.size() == 1) {
            prefs(ctx).edit().putInt(KEY_ACTIVE, 0).apply();
        }
        syncActiveFuel(ctx);
        return true;
    }

    /** Reemplaza el vehículo en position. */
    public static void updateVehicle(Context ctx, int position, Vehicle vehicle) {
        List<Vehicle> list = loadVehicles(ctx);
        if (position < 0 || position >= list.size()) return;
        list.set(position, vehicle);
        saveVehicles(ctx, list);
        syncActiveFuel(ctx);
    }

    /** Elimina el vehículo en position y ajusta el índice activo. */
    public static void deleteVehicle(Context ctx, int position) {
        List<Vehicle> list = loadVehicles(ctx);
        if (position < 0 || position >= list.size()) return;
        list.remove(position);
        saveVehicles(ctx, list);

        int active = loadActiveIndex(ctx);
        if (!list.isEmpty()) {
            prefs(ctx).edit()
                    .putInt(KEY_ACTIVE, Math.min(active, list.size() - 1))
                    .apply();
        } else {
            prefs(ctx).edit().putInt(KEY_ACTIVE, 0).apply();
        }
        syncActiveFuel(ctx);
    }

    /**
     * Sincroniza "pref_selected_fuel" con el combustible del vehículo activo.
     * Llamar siempre que cambie el vehículo activo o se modifique uno.
     */
    public static void syncActiveFuel(Context ctx) {
        Vehicle active = loadActiveVehicle(ctx);
        if (active == null) return;
        prefs(ctx).edit()
                .putString(KEY_SELECTED_FUEL, active.getFuelType().name())
                .apply();
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private static SharedPreferences prefs(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
    }
}