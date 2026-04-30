package com.example.ahorragas.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.example.ahorragas.data.GasolineraRepository;
import com.example.ahorragas.data.RepoError;
import com.example.ahorragas.data.RoomGasolineraDataSource;
import com.example.ahorragas.data.local.AppDatabase;
import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Gasolinera;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona la persistencia de gasolineras favoritas en SharedPreferences.
 * Guarda solo el ID de cada gasolinera y obtiene los datos frescos desde Room.
 */
public final class FavoritesPrefs {

    private static final String KEY_FAVORITES = "pref_favorites";

    private FavoritesPrefs() {}

    /**
     * Devuelve una clave única para identificar una estación.
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
        List<String> ids = loadIds(ctx);
        String key = uniqueKey(gasolinera);
        if (ids.contains(key)) return;
        ids.add(key);
        saveIds(ctx, ids);
    }

    /**
     * Elimina una gasolinera de favoritos.
     *
     * @param ctx        Contexto de la aplicación.
     * @param gasolinera Gasolinera a eliminar.
     */
    public static void remove(Context ctx, Gasolinera gasolinera) {
        String key = uniqueKey(gasolinera);
        List<String> ids = loadIds(ctx);
        ids.remove(key);
        saveIds(ctx, ids);
    }

    /**
     * Elimina una gasolinera de favoritos por su ID.
     *
     * @param ctx Contexto de la aplicación.
     * @param id  ID de la gasolinera a eliminar.
     */
    public static void remove(Context ctx, int id) {
        List<String> ids = loadIds(ctx);
        ids.remove("id:" + id);
        saveIds(ctx, ids);
    }

    /**
     * Comprueba si una gasolinera está en favoritos.
     *
     * @param ctx        Contexto de la aplicación.
     * @param gasolinera Gasolinera a comprobar.
     * @return true si está en favoritos.
     */
    public static boolean isFavorite(Context ctx, Gasolinera gasolinera) {
        return loadIds(ctx).contains(uniqueKey(gasolinera));
    }

    /**
     * Comprueba si una gasolinera está en favoritos por ID.
     *
     * @param ctx Contexto de la aplicación.
     * @param id  ID de la gasolinera.
     * @return true si está en favoritos.
     */
    public static boolean isFavorite(Context ctx, int id) {
        if (id == 0) return false;
        return loadIds(ctx).contains("id:" + id);
    }

    /**
     * Devuelve la lista completa de gasolineras favoritas con datos frescos de Room.
     * Mantiene compatibilidad con el formato antiguo (objeto completo).
     *
     * @param ctx Contexto de la aplicación.
     * @return Lista de gasolineras favoritas, vacía si no hay ninguna.
     */
    public static List<Gasolinera> loadAll(Context ctx) {
        List<Gasolinera> result = new ArrayList<>();
        String raw = prefs(ctx).getString(KEY_FAVORITES, null);
        if (raw == null || raw.isEmpty()) return result;

        try {
            JSONArray arr = new JSONArray(raw);

            // Detectar formato antiguo (objetos completos) y migrar
            if (arr.length() > 0 && arr.get(0) instanceof JSONObject) {
                result = loadAllLegacy(ctx, arr);
                // Migrar al nuevo formato
                List<String> ids = new ArrayList<>();
                for (Gasolinera g : result) {
                    ids.add(uniqueKey(g));
                }
                saveIds(ctx, ids);
                return result;
            }

            // Formato nuevo: array de strings con keys
            GasolineraRepository repo = GasolineraRepository.getInstance(
                    new RoomGasolineraDataSource(
                            AppDatabase.getInstance(ctx)));

            for (int i = 0; i < arr.length(); i++) {
                String key = arr.getString(i);
                if (key.startsWith("id:")) {
                    String idStr = key.substring(3);
                    try {
                        Gasolinera g = repo.getById(idStr);
                        if (g != null) result.add(g);
                    } catch (RepoError ignored) {}
                }
                // las claves "ll:" (electrolineras) se ignoran de momento
            }

        } catch (Exception e) {
            android.util.Log.e("FavoritesPrefs", "Error leyendo favoritos: " + e.getMessage(), e);
        }

        return result;
    }

    // ─── PRIVADO ─────────────────────────────────────────────────────────────

    private static List<String> loadIds(Context ctx) {
        List<String> ids = new ArrayList<>();
        String raw = prefs(ctx).getString(KEY_FAVORITES, null);
        if (raw == null || raw.isEmpty()) return ids;

        try {
            JSONArray arr = new JSONArray(raw);
            if (arr.length() == 0) return ids;

            // Si es formato antiguo, extraer las keys
            if (arr.get(0) instanceof JSONObject) {
                List<Gasolinera> legacy = loadAllLegacy(ctx, arr);
                for (Gasolinera g : legacy) {
                    ids.add(uniqueKey(g));
                }
                saveIds(ctx, ids);
                return ids;
            }

            for (int i = 0; i < arr.length(); i++) {
                ids.add(arr.getString(i));
            }
        } catch (Exception e) {
            android.util.Log.e("FavoritesPrefs", "Error leyendo IDs: " + e.getMessage(), e);
        }

        return ids;
    }

    private static void saveIds(Context ctx, List<String> ids) {
        try {
            JSONArray arr = new JSONArray();
            for (String id : ids) arr.put(id);
            prefs(ctx).edit().putString(KEY_FAVORITES, arr.toString()).apply();
        } catch (Exception e) {
            android.util.Log.e("FavoritesPrefs", "Error guardando IDs: " + e.getMessage(), e);
        }
    }

    /**
     * Carga favoritos en formato antiguo (objeto completo) para migración.
     */
    private static List<Gasolinera> loadAllLegacy(Context ctx, JSONArray arr) {
        List<Gasolinera> list = new ArrayList<>();
        try {
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
            android.util.Log.e("FavoritesPrefs", "Error leyendo legacy: " + e.getMessage(), e);
        }
        return list;
    }

    private static SharedPreferences prefs(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
    }
}