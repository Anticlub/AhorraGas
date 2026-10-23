package com.ahorragas.app.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/** Abre enlaces web en el navegador del usuario. */
public final class WebLinks {

    private WebLinks() {}

    /** Abre la URL en el navegador. Si no hay ninguno instalado, no hace nada. */
    public static void open(Context ctx, String url) {
        try {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException ignored) {
            // Sin navegador instalado: no hay nada que abrir.
        }
    }
}