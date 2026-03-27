package com.example.ahorragas.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.example.ahorragas.model.Gasolinera;

/**
 * Lanza navegación hacia una gasolinera en Google Maps o Waze.
 * Si ambas están instaladas, muestra un chooser.
 * Si solo una está instalada, la abre directamente.
 * Si ninguna está instalada, abre el navegador con Google Maps web.
 */
public final class NavigationHelper {

    private static final String PKG_MAPS = "com.google.android.apps.maps";
    private static final String PKG_WAZE = "com.waze";

    private NavigationHelper() {}

    public static void navigate(Context context, Gasolinera gasolinera) {
        if (gasolinera.getLat() == null || gasolinera.getLon() == null) return;

        double lat = gasolinera.getLat();
        double lon = gasolinera.getLon();
        String label = gasolinera.getMarca() != null ? gasolinera.getMarca() : "Gasolinera";

        boolean mapsInstalled = isAppInstalled(context, PKG_MAPS);
        boolean wazeInstalled = isAppInstalled(context, PKG_WAZE);

        if (mapsInstalled && wazeInstalled) {
            showChooser(context, lat, lon, label);
        } else if (wazeInstalled) {
            openWaze(context, lat, lon);
        } else {
            // Google Maps (app o web)
            openGoogleMaps(context, lat, lon, label);
        }
    }

    // ─── Apps individuales ───────────────────────────────────────────────────

    private static void openGoogleMaps(Context context, double lat, double lon, String label) {
        // geo URI abre la app si está instalada, o el navegador con maps.google.com si no
        Uri uri = Uri.parse(String.format(
                java.util.Locale.US,
                "geo:%f,%f?q=%f,%f(%s)",
                lat, lon, lat, lon,
                Uri.encode(label)
        ));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(PKG_MAPS);

        if (isAppInstalled(context, PKG_MAPS)) {
            context.startActivity(intent);
        } else {
            // Fallback web
            Uri webUri = Uri.parse(String.format(
                    java.util.Locale.US,
                    "https://www.google.com/maps/dir/?api=1&destination=%f,%f",
                    lat, lon
            ));
            context.startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
    }

    private static void openWaze(Context context, double lat, double lon) {
        Uri uri = Uri.parse(String.format(
                java.util.Locale.US,
                "waze://?ll=%f,%f&navigate=yes",
                lat, lon
        ));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(PKG_WAZE);
        context.startActivity(intent);
    }

    // ─── Chooser ─────────────────────────────────────────────────────────────

    private static void showChooser(Context context, double lat, double lon, String label) {
        String[] options = {"Google Maps", "Waze"};

        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Abrir con…")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) openGoogleMaps(context, lat, lon, label);
                    else            openWaze(context, lat, lon);
                })
                .show();
    }

    // ─── Utils ───────────────────────────────────────────────────────────────

    private static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
