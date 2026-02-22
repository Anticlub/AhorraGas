package com.example.ahorragas.util;

import com.example.ahorragas.model.Gasolinera;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GasolineraSorter {

    private GasolineraSorter() {}

    // Valores por defecto para mapa (ajustables)
    private static final int DEFAULT_MAP_MAX_RESULTS = 150;
    private static final double DEFAULT_MAP_RADIUS_METERS = 10_000; // 10 km

    /**
     * Filtra gasolineras con coords inválidas, calcula distanceMeters y ordena por cercanía.
     * Devuelve una lista NUEVA (no modifica el orden de la lista original),
     * pero sí setea distanceMeters en cada Gasolinera válida.
     */
    public static List<Gasolinera> filterComputeAndSort(List<Gasolinera> gasolineras,
                                                        double userLat,
                                                        double userLon) {
        List<Gasolinera> result = new ArrayList<>();
        if (gasolineras == null) return result;

        for (Gasolinera g : gasolineras) {
            if (!GeoValidation.isValidLatLon(g.getLat(), g.getLon())) continue;

            double d = GeoUtils.distanceMeters(userLat, userLon, g.getLat(), g.getLon());
            g.setDistanceMeters(d);
            result.add(g);
        }

        result.sort(Comparator.comparingDouble(g ->
                g.getDistanceMeters() == null ? Double.MAX_VALUE : g.getDistanceMeters()
        ));

        return result;
    }

    /**
     * Devuelve las N gasolineras más cercanas al usuario.
     */
    public static List<Gasolinera> getTopClosest(List<Gasolinera> gasolineras,
                                                 double userLat, double userLon,
                                                 int maxResults) {

        List<Gasolinera> sorted = filterComputeAndSort(gasolineras, userLat, userLon);
        return limit(sorted, maxResults);
    }

    /**
     * Devuelve gasolineras dentro de un radio (en metros), ordenadas por cercanía.
     */
    public static List<Gasolinera> getWithinRadius(List<Gasolinera> gasolineras,
                                                   double userLat, double userLon,
                                                   double radiusMeters) {

        if (radiusMeters <= 0) return new ArrayList<>();

        List<Gasolinera> sorted = filterComputeAndSort(gasolineras, userLat, userLon);

        List<Gasolinera> result = new ArrayList<>();
        for (Gasolinera g : sorted) {
            Double d = g.getDistanceMeters();
            if (d != null && d <= radiusMeters) {
                result.add(g);
            } else if (d != null && d > radiusMeters) {
                // Como está ordenado, al pasar el radio podemos cortar
                break;
            }
        }
        return result;
    }

    /**
     * Radio + límite (útil para mapa: evita saturación en ciudades).
     */
    public static List<Gasolinera> getWithinRadius(List<Gasolinera> gasolineras,
                                                   double userLat, double userLon,
                                                   double radiusMeters,
                                                   int maxResults) {

        List<Gasolinera> inRadius = getWithinRadius(gasolineras, userLat, userLon, radiusMeters);
        return limit(inRadius, maxResults);
    }

    // Limita el máximo de elemento de una lista
    private static List<Gasolinera> limit(List<Gasolinera> list, int maxResults) {
        if (maxResults <= 0) return new ArrayList<>();
        if (list.size() <= maxResults) return list;
        return new ArrayList<>(list.subList(0, maxResults));
    }

    /**
     * Selección recomendada para pintar en el mapa:
     * - filtra coordenadas inválidas
     * - calcula distanceMeters
     * - ordena por cercanía
     * - limita por radio y por máximo de resultados
     */
    public static List<Gasolinera> getForMap(List<Gasolinera> gasolineras,
                                             double userLat,
                                             double userLon) {
        return getWithinRadius(gasolineras, userLat, userLon,
                DEFAULT_MAP_RADIUS_METERS,
                DEFAULT_MAP_MAX_RESULTS);
    }

    public static List<Gasolinera> getForMap(List<Gasolinera> gasolineras,
                                             double userLat,
                                             double userLon,
                                             double radiusMeters,
                                             int maxResults) {
        return getWithinRadius(gasolineras, userLat, userLon,
                radiusMeters,
                maxResults);
    }
}