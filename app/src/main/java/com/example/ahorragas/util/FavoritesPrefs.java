package com.example.ahorragas.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Gasolinera;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona la persistencia de gasolineras favoritas en SharedPreferences.
 * Cada favorito se guarda como JSON con todos los datos necesarios para
 * mostrar la fila en la lista y abrir el detalle.
 */
public final class FavoritesPrefs {

    private static final String KEY_FAVORITES = "pref_favorites";

    private FavoritesPrefs() {}

    /**
     * Añade una gasolinera a favoritos. Si ya existe, no la duplica.
     *
     * @param ctx        Contexto de la aplicación.
     * @param gasolinera Gasolinera a guardar.
     */
    public static void add(Context ctx, Gasolinera gasolinera) {
        List<Gasolinera> list = loadAll(ctx);
        for (Gasolinera g : list) {
            if (g.getId() == gasolinera.getId()) return; // ya existe
        }
        list.add(gasolinera);
        saveAll(ctx, list);
    }

    /**
     * Elimina una gasolinera de favoritos por su ID.
     *
     * @param ctx Contexto de la aplicación.
     * @param id  ID de la gasolinera a eliminar.
     */
    public static void remove(Context ctx, int id) {
        List<Gasolinera> list = loadAll(ctx);
        list.removeIf(g -> g.getId() == id);
        saveAll(ctx, list);
    }

    /**
     * Comprueba si una gasolinera está en favoritos.
     *
     * @param ctx Contexto de la aplicación.
     * @param id  ID de la gasolinera.
     * @return true si está en favoritos.
     */
    public static boolean isFavorite(Context ctx, int id) {
        for (Gasolinera g : loadAll(ctx)) {
            if (g.getId() == id) return true;
        }
        return false;
    }

    /**
     * Devuelve la lista completa de gasolineras favoritas.
     *
     * @param ctx Contexto de la aplicación.
     * @return Lista de gasolineras favoritas, vacía si no hay ninguna.
     */
    public static List<Gasolinera> loadAll(Context ctx) {
        List<Gasolinera> list = new ArrayList<>();
        String raw = prefs(ctx).getString(KEY_FAVORITES, null);
        if (raw == null || raw.isEmpty()) return list;

        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Gasolinera g = new Gasolinera(
                        obj.optInt("id"),
                        obj.optString("marca"),
                        obj.optString("municipio"),
                        obj.optString("direccion"),
                        obj.optDouble("lat"),
                        obj.optDouble("lon"),
                        null
                );
                g.setHorario(obj.optString("horario"));

                JSONObject prices = obj.optJSONObject("prices");
                if (prices != null) {
                    for (FuelType fuel : FuelType.values()) {
                        if (prices.has(fuel.name())) {
                            g.setPrecio(fuel, prices.getDouble(fuel.name()));
                        }
                    }
                }
                list.add(g);
            }
        } catch (Exception e) {
            android.util.Log.e("FavoritesPrefs", "Error leyendo favoritos: " + e.getMessage(), e);
        }

        return list;
    }

    // ─── PRIVADO ─────────────────────────────────────────────────────────────

    private static void saveAll(Context ctx, List<Gasolinera> list) {
        try {
            JSONArray arr = new JSONArray();
            for (Gasolinera g : list) {
                JSONObject obj = new JSONObject();
                obj.put("id",        g.getId());
                obj.put("marca",     g.getMarca());
                obj.put("municipio", g.getMunicipio());
                obj.put("direccion", g.getDireccion());
                obj.put("lat",       g.getLat() != null ? g.getLat() : 0.0);
                obj.put("lon",       g.getLon() != null ? g.getLon() : 0.0);
                obj.put("horario",   g.getHorario());

                JSONObject prices = new JSONObject();
                for (FuelType fuel : FuelType.values()) {
                    Double price = g.getPrecio(fuel);
                    if (price != null) prices.put(fuel.name(), price);
                }
                obj.put("prices", prices);
                arr.put(obj);
            }
            prefs(ctx).edit().putString(KEY_FAVORITES, arr.toString()).apply();
        } catch (Exception e) {
            android.util.Log.e("FavoritesPrefs", "Error guardando favoritos: " + e.getMessage(), e);
        }
    }

    private static SharedPreferences prefs(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
    }
}