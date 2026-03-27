package com.example.ahorragas.util;

import android.content.Context;

import androidx.preference.PreferenceManager;

public final class RadiusUtils {

    private RadiusUtils() {}

    public static final int MIN_KM = 1;
    public static final int MAX_KM = 50;
    public static final int DEFAULT_KM = 10;
    public static final int DEFAULT_MARKERS = 100;
    public static final int MIN_MARKERS = 1;
    public static final int MAX_MARKERS = 150;
    public static final String PREF_MARKERS_COUNT = "pref_markers_count";

    /** Clave usada en SharedPreferences para guardar el radio. */
    public static final String PREF_RADIUS_KM = "pref_radius_km";

    /**
     * Convierte km a metros asegurando que está en el rango permitido.
     */
    public static double kmToMetersClamped(int km) {
        int clamped = Math.max(MIN_KM, Math.min(MAX_KM, km));
        return clamped * 1000.0;
    }

    /** Lee el radio guardado en preferencias (en km). */
    public static int loadRadiusKm(Context context) {
        int saved = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(PREF_RADIUS_KM, DEFAULT_KM);
        return Math.max(MIN_KM, Math.min(MAX_KM, saved));
    }

    /** Guarda el radio en preferencias (en km). */
    public static void saveRadiusKm(Context context, int km) {
        int clamped = Math.max(MIN_KM, Math.min(MAX_KM, km));
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(PREF_RADIUS_KM, clamped)
                .apply();
    }
    /**
     * Lee el número de marcadores guardado en preferencias.
     * @param context contexto de la aplicación.
     * @return número de marcadores entre {@link #MIN_MARKERS} y {@link #MAX_MARKERS}.
     */
    public static int loadMarkersCount(Context context) {
        int saved = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(PREF_MARKERS_COUNT, DEFAULT_MARKERS);
        return Math.max(MIN_MARKERS, Math.min(MAX_MARKERS, saved));
    }

    /**
     * Guarda el número de marcadores en preferencias.
     * @param context contexto de la aplicación.
     * @param count   número de marcadores a guardar (se ajusta al rango permitido).
     */
    public static void saveMarkersCount(Context context, int count) {
        int clamped = Math.max(MIN_MARKERS, Math.min(MAX_MARKERS, count));
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putInt(PREF_MARKERS_COUNT, clamped).apply();
    }
}