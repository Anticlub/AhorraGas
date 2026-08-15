package com.ahorragas.app.ui;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.ahorragas.app.R;
import com.ahorragas.app.map.BrandLogoProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Diálogo de selección de marca para el filtro del mapa. Construye la lista de
 * opciones ("Todas" + marcas conocidas) y entrega la clave elegida (o null para
 * "todas") mediante {@link Listener}. También expone el nombre visible de una
 * marca, reutilizado por el botón del filtro.
 */
public final class BrandFilterDialog {

    /** Clave interna para la opción "Todas las marcas". */
    private static final String ALL = "__all__";

    /** Se invoca al elegir una marca (clave en minúsculas, o null = todas). */
    public interface Listener {
        void onBrandSelected(String brandKey);
    }

    private BrandFilterDialog() {}

    public static void show(Context ctx, String currentBrand, Listener listener) {
        List<String> keys = new ArrayList<>();
        keys.add(ALL);
        keys.addAll(BrandLogoProvider.FILTER_BRANDS);

        String[] labels = new String[keys.size()];
        int currentIndex = 0;
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            labels[i] = displayName(ctx, key);
            if ((ALL.equals(key) && currentBrand == null)
                    || (currentBrand != null && key.equalsIgnoreCase(currentBrand))) {
                currentIndex = i;
            }
        }

        new AlertDialog.Builder(ctx)
                .setTitle(R.string.brand_filter_dialog_title)
                .setSingleChoiceItems(labels, currentIndex, (dialog, which) -> {
                    String selectedKey = keys.get(which);
                    listener.onBrandSelected(ALL.equals(selectedKey) ? null : selectedKey);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** Nombre visible de una marca (o "Todas" si es null / la clave especial). */
    public static String displayName(Context ctx, String brandKey) {
        if (brandKey == null || ALL.equals(brandKey)) {
            return ctx.getString(R.string.brand_all);
        }
        if ("bp".equalsIgnoreCase(brandKey)) return "BP";
        if ("glp".equalsIgnoreCase(brandKey)) return "GLP";
        return Character.toUpperCase(brandKey.charAt(0))
                + brandKey.substring(1).toLowerCase(Locale.getDefault());
    }
}
