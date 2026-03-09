package com.example.ahorragas.util;

public final class RadiusUtils {

    private RadiusUtils() {}

    public static final int MIN_KM = 1;
    public static final int MAX_KM = 50;

    /**
     * Convierte km a metros asegurando que está en el rango permitido.
     */
    public static double kmToMetersClamped(int km) {
        int clamped = Math.max(MIN_KM, Math.min(MAX_KM, km));
        return clamped * 1000.0;
    }
}

// Cuando llegue UI (Semana 5/7), el slider te dará un int km (por ejemplo 1..50) y tú harás:
//double radiusMeters = RadiusUtils.kmToMetersClamped(km);
//List<Gasolinera> result = GasolineraSorter.getWithinRadius(lista, lat, lon, radiusMeters, 150);