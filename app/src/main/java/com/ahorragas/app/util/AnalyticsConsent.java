package com.ahorragas.app.util;

import android.content.Context;

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Consentimiento del usuario para Firebase Analytics (RGPD).
 *
 * <p>Analytics arranca desactivado desde el manifest
 * ({@code firebase_analytics_collection_enabled = false}) y solo se activa si el
 * usuario acepta. La decisión se guarda aquí y se vuelve a aplicar en cada
 * arranque desde {@code AhorraGasApp}.</p>
 */
public final class AnalyticsConsent {

    /** Decisión del usuario. UNKNOWN = todavía no ha elegido. */
    public enum State { UNKNOWN, GRANTED, DENIED }

    private static final String PREFS = "analytics_consent_prefs";
    private static final String KEY_STATE = "state";

    private AnalyticsConsent() {}

    public static State get(Context ctx) {
        String value = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_STATE, null);
        if (value == null) return State.UNKNOWN;
        try {
            return State.valueOf(value);
        } catch (IllegalArgumentException e) {
            return State.UNKNOWN;
        }
    }

    public static boolean isGranted(Context ctx) {
        return get(ctx) == State.GRANTED;
    }

    /**
     * Guarda la decisión del usuario y la aplica a Analytics. Si la rechaza (o la
     * retira), además se borran los datos de Analytics guardados en el dispositivo.
     */
    public static void set(Context ctx, boolean granted) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_STATE, (granted ? State.GRANTED : State.DENIED).name())
                .apply();
        apply(ctx);
        if (!granted) {
            FirebaseAnalytics.getInstance(ctx).resetAnalyticsData();
        }
    }

    /** Aplica a Analytics la decisión guardada. Sin decisión, sigue desactivado. */
    public static void apply(Context ctx) {
        FirebaseAnalytics.getInstance(ctx).setAnalyticsCollectionEnabled(isGranted(ctx));
    }
}