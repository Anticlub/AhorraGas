package com.example.ahorragas.util;

public final class GeoUtils {

    private static final double EARTH_RADIUS_M = 6371000.0; // metros

    private GeoUtils() {}

    /**
     * Distancia Haversine entre dos puntos WGS84.
     * @return distancia en metros
     */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_M * c;
    }
    /**
     * Calcula el bounding box (minLat, maxLat, minLon, maxLon) para un radio dado.
     * Útil para queries geoespaciales en SQLite sin extensiones espaciales.
     * El resultado es un cuadrado que contiene el círculo — las queries pueden
     * devolver esquinas fuera del radio exacto, pero es suficiente para el mapa.
     *
     * @param lat          latitud del centro en grados
     * @param lon          longitud del centro en grados
     * @param radiusMeters radio en metros
     * @return array [minLat, maxLat, minLon, maxLon]
     */
    public static double[] boundingBox(double lat, double lon, double radiusMeters) {
        double deltaLat = Math.toDegrees(radiusMeters / EARTH_RADIUS_M);
        double deltaLon = Math.toDegrees(radiusMeters /
                (EARTH_RADIUS_M * Math.cos(Math.toRadians(lat))));
        return new double[]{
                lat - deltaLat,
                lat + deltaLat,
                lon - deltaLon,
                lon + deltaLon
        };
    }
}