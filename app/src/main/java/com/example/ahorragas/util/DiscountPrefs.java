package com.example.ahorragas.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.example.ahorragas.model.Discount;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Persiste los descuentos del usuario en SharedPreferences como JSON array.
 * Clave: "pref_discounts"
 */
public final class DiscountPrefs {

    private static final String KEY_DISCOUNTS = "pref_discounts";
    public static final int MAX_DISCOUNTS = 10;

    private DiscountPrefs() {}

    // ─── READ ────────────────────────────────────────────────────────────────

    /**
     * Carga la lista de descuentos guardados.
     *
     * @param ctx Contexto de la aplicación.
     * @return Lista de descuentos, vacía si no hay ninguno.
     */
    public static List<Discount> loadDiscounts(Context ctx) {
        List<Discount> list = new ArrayList<>();
        String raw = prefs(ctx).getString(KEY_DISCOUNTS, null);
        if (raw == null || raw.isEmpty()) return list;

        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String brand = obj.optString("brand", "");
                String type  = obj.optString("type", Discount.Type.CENTS_PER_LITER.name());
                double value = obj.optDouble("value", 0.0);
                list.add(new Discount(brand, Discount.Type.valueOf(type), value));
            }
        } catch (Exception e) {
            android.util.Log.e("DiscountPrefs", "Error leyendo descuentos: " + e.getMessage(), e);
        }

        return list;
    }

    /**
     * Devuelve el descuento aplicable a una marca de gasolinera, o null si no hay ninguno.
     *
     * @param ctx         Contexto de la aplicación.
     * @param stationBrand Marca de la gasolinera.
     * @return Primer descuento que aplica, o null.
     */
    public static Discount findForBrand(Context ctx, String stationBrand) {
        for (Discount d : loadDiscounts(ctx)) {
            if (d.appliesTo(stationBrand)) return d;
        }
        return null;
    }

    // ─── WRITE ───────────────────────────────────────────────────────────────

    /**
     * Guarda la lista completa de descuentos.
     *
     * @param ctx       Contexto de la aplicación.
     * @param discounts Lista de descuentos a guardar.
     */
    public static void saveDiscounts(Context ctx, List<Discount> discounts) {
        try {
            JSONArray arr = new JSONArray();
            int limit = Math.min(discounts.size(), MAX_DISCOUNTS);
            for (int i = 0; i < limit; i++) {
                Discount d = discounts.get(i);
                JSONObject obj = new JSONObject();
                obj.put("brand", d.getBrandName());
                obj.put("type",  d.getType().name());
                obj.put("value", d.getValue());
                arr.put(obj);
            }
            prefs(ctx).edit().putString(KEY_DISCOUNTS, arr.toString()).apply();
        } catch (Exception e) {
            android.util.Log.e("DiscountPrefs", "Error guardando descuentos: " + e.getMessage(), e);
        }
    }

    /**
     * Añade un descuento. Devuelve false si ya se alcanzó el máximo.
     *
     * @param ctx      Contexto de la aplicación.
     * @param discount Descuento a añadir.
     * @return true si se añadió correctamente.
     */
    public static boolean addDiscount(Context ctx, Discount discount) {
        List<Discount> list = loadDiscounts(ctx);
        if (list.size() >= MAX_DISCOUNTS) return false;
        list.add(discount);
        saveDiscounts(ctx, list);
        return true;
    }

    /**
     * Elimina el descuento en la posición indicada.
     *
     * @param ctx      Contexto de la aplicación.
     * @param position Índice del descuento a eliminar.
     */
    public static void deleteDiscount(Context ctx, int position) {
        List<Discount> list = loadDiscounts(ctx);
        if (position < 0 || position >= list.size()) return;
        list.remove(position);
        saveDiscounts(ctx, list);
    }

    /**
     * Reemplaza el descuento en la posición indicada.
     *
     * @param ctx      Contexto de la aplicación.
     * @param position Índice del descuento a reemplazar.
     * @param discount Nuevo descuento.
     */
    public static void updateDiscount(Context ctx, int position, Discount discount) {
        List<Discount> list = loadDiscounts(ctx);
        if (position < 0 || position >= list.size()) return;
        list.set(position, discount);
        saveDiscounts(ctx, list);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private static SharedPreferences prefs(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
    }
}