package com.example.ahorragas;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.modules.SqlTileWriter;

/**
 * Clase Application de AhorraGas.
 *
 * Centraliza la inicialización de osmdroid una sola vez para toda la app,
 * de modo que todas las pantallas con mapa (mapa principal y detalle de
 * estación) usen el mismo User-Agent válido.
 *
 * Contexto: los servidores de tiles de OpenStreetMap bloquean los User-Agent
 * genéricos o de ejemplo (como el antiguo "com.example.ahorragas"), devolviendo
 * una imagen de "Access blocked". Ver la política de uso:
 * https://operations.osmfoundation.org/policies/tiles/
 */
public class AhorraGasApp extends Application {

    /**
     * User-Agent que identifica la app ante OSM cumpliendo su política de uso.
     * osmdroid le añade automáticamente "/&lt;versionCode&gt;" al final.
     */
    private static final String OSM_USER_AGENT =
            "AhorraGas/1.0 (+https://github.com/Anticlub/AhorraGas)";

    /**
     * Flag de preferencias para purgar una única vez el caché de tiles.
     * Necesario porque osmdroid pudo haber cacheado las imágenes de "bloqueado"
     * mientras el User-Agent antiguo estaba vetado; sin purgarlas, seguiría
     * mostrándolas desde disco aunque el User-Agent ya sea válido.
     */
    private static final String PREF_TILE_CACHE_PURGED = "osm_tile_cache_purged_v1";

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Inicializa osmdroid y fija el User-Agent válido para toda la app.
        Configuration.getInstance().load(this, prefs);
        Configuration.getInstance().setUserAgentValue(OSM_USER_AGENT);

        purgeStaleTileCacheOnce(prefs);
    }

    /**
     * Purga el caché de tiles una sola vez, para eliminar las imágenes de
     * "Access blocked" que osmdroid pudiera tener guardadas. Tras la purga se
     * marca un flag para no volver a borrar el caché en cada arranque.
     *
     * @param prefs preferencias por defecto de la app
     */
    private void purgeStaleTileCacheOnce(SharedPreferences prefs) {
        if (prefs.getBoolean(PREF_TILE_CACHE_PURGED, false)) {
            return;
        }
        try {
            SqlTileWriter writer = new SqlTileWriter();
            writer.purgeCache();
            writer.onDetach();
        } catch (Exception ignored) {
            // Si la purga falla no es crítico: las tiles caducan por su cuenta.
        }
        prefs.edit().putBoolean(PREF_TILE_CACHE_PURGED, true).apply();
    }
}
