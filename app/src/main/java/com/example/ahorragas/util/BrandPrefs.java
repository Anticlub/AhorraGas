package com.example.ahorragas.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Almacena la marca seleccionada en el filtro de la pantalla principal.
 * null = "Todas las marcas".
 */
public final class BrandPrefs {

    private static final String PREFS = "brand_prefs";
    private static final String KEY_SELECTED = "selected_brand";

    private BrandPrefs() {}

    public static String get(Context ctx) {
        String value = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_SELECTED, null);
        return (value == null || value.isEmpty()) ? null : value;
    }

    public static void set(Context ctx, String brand) {
        SharedPreferences.Editor edit = ctx
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        if (brand == null || brand.isEmpty()) {
            edit.remove(KEY_SELECTED);
        } else {
            edit.putString(KEY_SELECTED, brand.toLowerCase());
        }
        edit.apply();
    }
}
