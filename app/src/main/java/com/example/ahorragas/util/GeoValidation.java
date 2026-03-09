package com.example.ahorragas.util;

public final class GeoValidation {

    private GeoValidation() {}

    public static boolean isValidLatLon(Double lat, Double lon) {
        if (lat == null || lon == null) return false;

        // Rango válido WGS84
        if (lat < -90.0 || lat > 90.0) return false;
        if (lon < -180.0 || lon > 180.0) return false;

        // Caso típico de datos rotos
        if (lat == 0.0 && lon == 0.0) return false;

        // Evitar NaN / Infinity
        if (lat.isNaN() || lon.isNaN()) return false;
        if (lat.isInfinite() || lon.isInfinite()) return false;

        return true;
    }
}