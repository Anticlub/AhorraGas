package com.example.ahorragas.util;

import com.example.ahorragas.model.FuelType;
import com.example.ahorragas.model.Gasolinera;
import com.example.ahorragas.model.PriceLevel;
import com.example.ahorragas.model.PriceRange;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GasolineraSorter {

    private GasolineraSorter() {}

    private static final int DEFAULT_MAP_MAX_RESULTS = 150;
    private static final double DEFAULT_MAP_RADIUS_METERS = 10_000; // 10 km

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

    public static List<Gasolinera> getTopClosest(List<Gasolinera> gasolineras,
                                                 double userLat, double userLon,
                                                 int maxResults) {

        List<Gasolinera> sorted = filterComputeAndSort(gasolineras, userLat, userLon);
        return limit(sorted, maxResults);
    }

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
                break;
            }
        }
        return result;
    }

    public static List<Gasolinera> getWithinRadius(List<Gasolinera> gasolineras,
                                                   double userLat, double userLon,
                                                   double radiusMeters,
                                                   int maxResults) {

        List<Gasolinera> inRadius = getWithinRadius(gasolineras, userLat, userLon, radiusMeters);
        return limit(inRadius, maxResults);
    }

    private static List<Gasolinera> limit(List<Gasolinera> list, int maxResults) {
        if (maxResults <= 0) return new ArrayList<>();
        if (list.size() <= maxResults) return list;
        return new ArrayList<>(list.subList(0, maxResults));
    }

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

    /**
     * ✅ Nuevo: rango de precios para un combustible concreto.
     * Ignora precios nulos o <= 0.
     */
    public static PriceRange calculatePriceRange(List<Gasolinera> gasolineras, FuelType fuel) {
        if (gasolineras == null || gasolineras.isEmpty()) {
            return new PriceRange(null, null, 0);
        }

        Double min = null;
        Double max = null;
        int count = 0;

        for (Gasolinera g : gasolineras) {
            Double price = (fuel == null) ? g.getPrecio() : g.getPrecio(fuel);
            if (price == null || price <= 0) continue;

            if (min == null || price < min) min = price;
            if (max == null || price > max) max = price;

            count++;
        }

        return new PriceRange(min, max, count);
    }

    /**
     * ✅ Compatibilidad: el método antiguo sigue funcionando (Gasóleo A por defecto).
     */
    public static PriceRange calculatePriceRange(List<Gasolinera> gasolineras) {
        return calculatePriceRange(gasolineras, FuelType.GASOLEO_A);
    }

    public static PriceLevel getPriceLevel(Double price, PriceRange range) {

        if (price == null || range == null || range.isEmpty()) {
            return PriceLevel.MID;
        }

        Double min = range.getMin();
        Double max = range.getMax();

        if (min == null || max == null) {
            return PriceLevel.MID;
        }

        if (max.equals(min)) {
            return PriceLevel.MID;
        }

        double normalized = (price - min) / (max - min);

        if (normalized <= 0.33) {
            return PriceLevel.CHEAP;
        } else if (normalized <= 0.66) {
            return PriceLevel.MID;
        } else {
            return PriceLevel.EXPENSIVE;
        }
    }
}