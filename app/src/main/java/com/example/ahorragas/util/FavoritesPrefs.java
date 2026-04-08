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
     * Devuelve una clave única para identificar una estación.
     * Para gasolineras usa el ID del Ministerio.
     * Para electrolineras (id=0) usa lat+lon como identificador.
     *
     * @param gasolinera estación a identificar
     * @return clave única como string
     */
    private static String uniqueKey(Gasolinera gasolinera) {
        if (gasolinera.getId() != 0) {
            return "id:" + gasolinera.getId();
        }
        return "ll:" + gasolinera.getLat() + "," + gasolinera.getLon();
    }

    /**
     * Añade una gasolinera a favoritos. Si ya existe, no la duplica.
     *
     * @param ctx        Contexto de la aplicación.
     * @param gasolinera Gasolinera a guardar.
     */
    public static void add(Context ctx, Gasolinera gasolinera) {
        List<Gasolinera> list = loadAll(ctx);
        String key = uniqueKey(gasolinera);
        for (Gasolinera g : list) {
            if (uniqueKey(g).equals(key)) return;
        }
        list.add(gasolinera);
        saveAll(ctx, list);
    }

    /**
     * Elimina una gasolinera de favoritos.
     *
     * @param ctx        Contexto de la aplicación.
     * @param gasolinera Gasolinera a eliminar.
     */
    public static void remove(Context ctx, Gasolinera gasolinera) {
        String key = uniqueKey(gasolinera);
        List<Gasolinera> list = loadAll(ctx);
        list.removeIf(g -> uniqueKey(g).equals(key));
        saveAll(ctx, list);
    }

    /**
     * Elimina una gasolinera de favoritos por su ID (solo gasolineras con ID válido).
     *
     * @param ctx Contexto de la aplicación.
     * @param id  ID de la gasolinera a eliminar.
     */
    public static void remove(Context ctx, int id) {
        List<Gasolinera> list = loadAll(ctx);
        list.removeIf(g -> g.getId() == id && id != 0);
        saveAll(ctx, list);
    }

    /**
     * Comprueba si una gasolinera está en favoritos.
     *
     * @param ctx        Contexto de la aplicación.
     * @param gasolinera Gasolinera a comprobar.
     * @return true si está en favoritos.
     */
    public static boolean isFavorite(Context ctx, Gasolinera gasolinera) {
        String key = uniqueKey(gasolinera);
        for (Gasolinera g : loadAll(ctx)) {
            if (uniqueKey(g).equals(key)) return true;
        }
        return false;
    }

    /**
     * Comprueba si una gasolinera está en favoritos por ID (solo gasolineras con ID válido).
     *
     * @param ctx Contexto de la aplicación.
     * @param id  ID de la gasolinera.
     * @return true si está en favoritos.
     */
    public static boolean isFavorite(Context ctx, int id) {
        if (id == 0) return false;
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
                g.setElectric(obj.optBoolean("electric", false));

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
                obj.put("electric",  g.isElectric());

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