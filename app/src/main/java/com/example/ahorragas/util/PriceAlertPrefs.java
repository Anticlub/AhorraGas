package com.example.ahorragas.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.PriceAlert;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Persiste hasta {@link #MAX_ALERTS} alertas de precio en SharedPreferences.
 */
public final class PriceAlertPrefs {

    public static final int MAX_ALERTS = 5;
    private static final String KEY_ALERTS = "pref_price_alerts";

    private PriceAlertPrefs() {}

    /**
     * Añade una alerta. Si ya existe una con la misma clave (gasolinera +
     * combustible) la sobreescribe. Devuelve false si ya hay MAX_ALERTS
     * alertas distintas y no hay hueco.
     *
     * @param ctx   Contexto de la aplicación.
     * @param alert Alerta a guardar.
     * @return true si se guardó correctamente.
     */
    public static boolean add(Context ctx, PriceAlert alert) {
        List<PriceAlert> list = loadAll(ctx);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getKey().equals(alert.getKey())) {
                list.set(i, alert);
                saveAll(ctx, list);
                return true;
            }
        }
        if (list.size() >= MAX_ALERTS) return false;
        list.add(alert);
        saveAll(ctx, list);
        return true;
    }

    /**
     * Elimina la alerta con la clave indicada.
     *
     * @param ctx Contexto de la aplicación.
     * @param key Clave de la alerta (ver {@link PriceAlert#getKey()}).
     */
    public static void remove(Context ctx, String key) {
        List<PriceAlert> list = loadAll(ctx);
        list.removeIf(a -> a.getKey().equals(key));
        saveAll(ctx, list);
    }

    /**
     * Devuelve todas las alertas guardadas.
     *
     * @param ctx Contexto de la aplicación.
     * @return Lista de alertas, vacía si no hay ninguna.
     */
    public static List<PriceAlert> loadAll(Context ctx) {
        List<PriceAlert> list = new ArrayList<>();
        String raw = prefs(ctx).getString(KEY_ALERTS, null);
        if (raw == null || raw.isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new PriceAlert(
                        obj.optInt("gasolineraId"),
                        obj.optString("gasolineraName"),
                        FuelType.fromString(obj.optString("fuelType")),
                        obj.optDouble("targetPrice"),
                        obj.optLong("lastNotifiedAt", 0L)
                ));
            }
        } catch (Exception e) {
            android.util.Log.e("PriceAlertPrefs", "Error leyendo alertas: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Actualiza el timestamp de última notificación de una alerta concreta.
     *
     * @param ctx Contexto de la aplicación.
     * @param key Clave de la alerta.
     * @param ts  Timestamp en millis.
     */
    public static void updateLastNotified(Context ctx, String key, long ts) {
        List<PriceAlert> list = loadAll(ctx);
        for (PriceAlert a : list) {
            if (a.getKey().equals(key)) {
                a.setLastNotifiedAt(ts);
                break;
            }
        }
        saveAll(ctx, list);
    }

    /**
     * Comprueba si ya existe una alerta para la clave dada.
     *
     * @param ctx Contexto de la aplicación.
     * @param key Clave de la alerta.
     * @return true si existe.
     */
    public static boolean exists(Context ctx, String key) {
        for (PriceAlert a : loadAll(ctx)) {
            if (a.getKey().equals(key)) return true;
        }
        return false;
    }

    /** Devuelve cuántas alertas hay guardadas actualmente. */
    public static int count(Context ctx) {
        return loadAll(ctx).size();
    }

    // ─── PRIVADO ──────────────────────────────────────────────────────────────

    private static void saveAll(Context ctx, List<PriceAlert> list) {
        try {
            JSONArray arr = new JSONArray();
            for (PriceAlert a : list) {
                JSONObject obj = new JSONObject();
                obj.put("gasolineraId",   a.getGasolineraId());
                obj.put("gasolineraName", a.getGasolineraName());
                obj.put("fuelType",       a.getFuelType().name());
                obj.put("targetPrice",    a.getTargetPrice());
                obj.put("lastNotifiedAt", a.getLastNotifiedAt());
                arr.put(obj);
            }
            prefs(ctx).edit().putString(KEY_ALERTS, arr.toString()).apply();
        } catch (Exception e) {
            android.util.Log.e("PriceAlertPrefs", "Error guardando alertas: " + e.getMessage(), e);
        }
    }

    private static SharedPreferences prefs(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
    }
}