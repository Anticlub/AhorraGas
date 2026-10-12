package com.ahorragas.app.map;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

/**
 * Atribución de OpenStreetMap que se muestra sobre los mapas. La política de
 * uso de los tiles exige que sea visible y que enlace a la página de copyright.
 * Ver: https://operations.osmfoundation.org/policies/tiles/
 */
public final class OsmAttribution {

    private static final Uri COPYRIGHT_URL =
            Uri.parse("https://www.openstreetmap.org/copyright");

    private OsmAttribution() {}

    /** Hace que la vista de atribución abra la página de copyright de OSM al pulsarla. */
    public static void bind(View attribution) {
        attribution.setOnClickListener(v -> {
            try {
                v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, COPYRIGHT_URL));
            } catch (ActivityNotFoundException ignored) {
                // Sin navegador instalado: no hay nada que abrir.
            }
        });
    }
}